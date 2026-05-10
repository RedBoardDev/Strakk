import type {
  ScanMealPort,
  ScanRequest,
  ScanResponse,
} from "../port/in/scan-meal.port.ts";
import type { VisionPort } from "../port/out/vision.port.ts";
import type { EmbeddingPort } from "../port/out/embedding.port.ts";
import type { VectorSearchPort } from "../port/out/vector-search.port.ts";
import type { DisambiguationPort } from "../port/out/disambiguation.port.ts";
import type {
  MealValidatorPort,
  ValidationItem,
} from "../port/out/validator.port.ts";
import type { Prediction } from "../model/prediction.ts";
import type { FoodMatch } from "../model/food-match.ts";
import type { ComputedMacros, GroundedItem } from "../model/grounded-item.ts";
import { computeMacros, isVolumetric, toGrams, zeroMacros } from "./unit-conversion.ts";
import { applyCookingAdjustment } from "./cooking-adjustment.ts";

const MATCH_THRESHOLD = 0.25;
const MATCH_COUNT = 40;
// Direct-match shortcut disabled: empirically the embedding ranks similar but
// nutritionally different items at the top (e.g. "duck breast" closest to
// "chicken breast sauteed"). Always run disambiguation so Claude can pick the
// correct cut/species using both the candidate names and the source images.
const DIRECT_MATCH_SIMILARITY = 1.01;
const DIRECT_MATCH_MARGIN = 1.0;

/**
 * Lexical species/keyword anchors. When the prediction name contains one of
 * these tokens, candidates that lack the same token are filtered out before
 * disambiguation. This corrects the systematic embedding bias where
 * "duck breast pan-seared" maps closer to "chicken breast sauteed" than to
 * any actual duck entry, because the cooking/cut/skin tokens dominate.
 */
const SPECIES_ANCHORS: ReadonlyArray<readonly string[]> = [
  // Proteins where misclassification across species materially changes macros
  // (e.g. duck has 2× the fat of chicken at similar protein density).
  ["duck"],
  ["chicken"],
  ["turkey"],
  ["beef", "veal", "steak"],
  ["pork", "ham", "bacon"],
  ["lamb", "mutton"],
  ["salmon"],
  ["tuna"],
  ["cod"],
  ["trout"],
  ["shrimp", "prawn"],
  ["tofu"],
];

function pickSpeciesAnchor(predictionName: string): readonly string[] | null {
  // Only consider the PRIMARY food (the part before the first comma or
  // parenthesis). Otherwise "olive oil, used for cooking (duck and sauce)"
  // would be anchored to "duck" and never match an oil entry.
  const primary = predictionName
    .split(/[,(]/, 1)[0]
    .toLowerCase()
    .trim();
  for (const group of SPECIES_ANCHORS) {
    if (group.some((tok) => primary.includes(tok))) return group;
  }
  return null;
}

export interface ScanMealServiceDeps {
  vision: VisionPort;
  embedding: EmbeddingPort;
  vectorSearch: VectorSearchPort;
  disambiguation: DisambiguationPort;
  validator: MealValidatorPort;
}

export class ScanMealService implements ScanMealPort {
  private readonly vision: VisionPort;
  private readonly embedding: EmbeddingPort;
  private readonly vectorSearch: VectorSearchPort;
  private readonly disambiguation: DisambiguationPort;
  private readonly validator: MealValidatorPort;

  constructor(deps: ScanMealServiceDeps) {
    this.vision = deps.vision;
    this.embedding = deps.embedding;
    this.vectorSearch = deps.vectorSearch;
    this.disambiguation = deps.disambiguation;
    this.validator = deps.validator;
  }

  async scan(request: ScanRequest): Promise<ScanResponse> {
    // Pass 1 — identify and ground from the photo.
    const initialPredictions = await this.vision.identify(request.images, request.hint);
    console.log("[scan] Pass 1 predictions:", JSON.stringify(initialPredictions));
    if (initialPredictions.length === 0) {
      return { predictions: [], items: [] };
    }

    let items = await this.groundPredictions(initialPredictions, request.images);
    let predictions = initialPredictions;
    console.log(
      "[scan] Pass 1 grounded:",
      JSON.stringify(items.map((it) => ({
        predicted: it.prediction.name,
        amount: `${it.prediction.amount} ${it.prediction.unit}`,
        matched: it.match?.name,
        grams: it.macros.grams,
        kcal: it.macros.kcal,
        protein: it.macros.protein,
        fat: it.macros.fat,
        carbs: it.macros.carbs,
      }))),
    );
    console.log("[scan] Pass 1 totals:", JSON.stringify(sumMacros(items)));

    // Pass 2 (validator) — DISABLED. In testing it consistently increased
    // quantities and added items rather than catching overestimates. Re-enable
    // once we have a stricter "only reduce, never increase" prompt or a
    // calibrated few-shot example set.
    void this.validator;

    return { predictions, items };
  }

  private async groundPredictions(
    predictions: Prediction[],
    images: string[],
  ): Promise<GroundedItem[]> {
    if (predictions.length === 0) return [];
    const embeddings = await this.embedding.embed(predictions.map((p) => p.name));
    return Promise.all(
      predictions.map((prediction, i) =>
        this.processPrediction(prediction, embeddings[i], images)
      ),
    );
  }

  private async processPrediction(
    prediction: Prediction,
    embedding: number[],
    images: string[],
  ): Promise<GroundedItem> {
    const requireDensity = isVolumetric(prediction.unit);

    const rawMatches = await this.vectorSearch.search(embedding, {
      limit: MATCH_COUNT,
      threshold: MATCH_THRESHOLD,
      requireDensity,
    });

    if (rawMatches.length === 0) {
      return {
        prediction,
        match: null,
        macros: zeroMacros(),
        isGrounded: false,
        confidence: 0,
      };
    }

    // Lexical species filter: keep only candidates that share the same protein
    // anchor as the prediction. If an anchor was found in the prediction but
    // no candidate matches it, we'd rather return "no match" than substitute
    // a different species (e.g. chicken for duck).
    const anchor = pickSpeciesAnchor(prediction.name);
    const matches = anchor
      ? rawMatches.filter((m) => {
        const name = m.name.toLowerCase();
        return anchor.some((tok) => name.includes(tok));
      })
      : rawMatches;

    if (matches.length === 0) {
      console.log(
        `[scan] No matches after species filter for "${prediction.name}" (anchor=${anchor?.join("|") ?? "none"}, raw=${rawMatches.length})`,
      );
      return {
        prediction,
        match: null,
        macros: zeroMacros(),
        isGrounded: false,
        confidence: 0,
      };
    }

    const topMatch = matches[0];
    let resolvedMatch: FoodMatch | null;

    if (
      matches.length === 1 ||
      (topMatch.similarity >= DIRECT_MATCH_SIMILARITY &&
        topMatch.similarity - matches[1].similarity >= DIRECT_MATCH_MARGIN)
    ) {
      resolvedMatch = topMatch;
    } else {
      resolvedMatch = await this.disambiguation.pickBest(prediction, matches, images);
    }

    if (!resolvedMatch) {
      return {
        prediction,
        match: null,
        macros: zeroMacros(),
        isGrounded: false,
        confidence: topMatch.similarity,
      };
    }

    const grams = toGrams(
      prediction.amount,
      prediction.unit,
      resolvedMatch.density,
      resolvedMatch.defaultPortionGrams,
    );

    const rawMacros = computeMacros(resolvedMatch, grams);
    const adjustedMacros = applyCookingAdjustment(
      rawMacros,
      prediction.name,
      resolvedMatch.name,
    );

    return {
      prediction,
      match: resolvedMatch,
      macros: adjustedMacros,
      isGrounded: true,
      confidence: resolvedMatch.similarity,
    };
  }

  /**
   * Runs the nutritionist validation pass. Returns the corrected predictions
   * when the validator wants to adjust the output, or null to keep the original.
   * On any error the validator is bypassed (best-effort improvement).
   */
  private async runValidation(
    items: GroundedItem[],
    request: ScanRequest,
  ): Promise<Prediction[] | null> {
    try {
      const result = await this.validator.validate({
        images: request.images,
        hint: request.hint,
        items: items.map(toValidationItem),
        totals: sumMacros(items),
      });
      if (result.unchanged) return null;
      return result.predictions;
    } catch (err) {
      console.error(
        "[scan] validation pass failed, keeping initial predictions:",
        err instanceof Error ? err.message : err,
      );
      return null;
    }
  }
}

function toValidationItem(it: GroundedItem): ValidationItem {
  return {
    predictedName: it.prediction.name,
    matchedName: it.match?.name ?? null,
    unit: it.prediction.unit,
    amount: it.prediction.amount,
    grams: it.macros.grams,
    macros: it.macros,
    isGrounded: it.isGrounded,
  };
}

function sumMacros(items: GroundedItem[]): ComputedMacros {
  return items.reduce<ComputedMacros>(
    (acc, it) => ({
      grams: acc.grams + it.macros.grams,
      kcal: acc.kcal + it.macros.kcal,
      protein: acc.protein + it.macros.protein,
      fat: acc.fat + it.macros.fat,
      carbs: acc.carbs + it.macros.carbs,
    }),
    { grams: 0, kcal: 0, protein: 0, fat: 0, carbs: 0 },
  );
}
