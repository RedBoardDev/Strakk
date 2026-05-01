import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit, checkPayloadSize } from "../_shared/rate-limit.ts";

// =============================================================================
// Types — Input
// =============================================================================

interface ProgramExercise {
  name: string;
  sets: number;
  reps: string;
  weight_kg: number | null;
  weight_per_set: (number | null)[];
  rest_seconds: number;
  notes: string | null;
  superset_group: number | null;
  exercise_type: string;
  equipment_category: string;
  muscle_group: string;
}

interface ExerciseSection {
  name: string;
  exercises: ProgramExercise[];
}

interface WorkoutSession {
  name: string;
  sections: ExerciseSection[];
}

interface ExportRequest {
  session: WorkoutSession;
  // hevy_api_key removed — read from DB server-side
}

// =============================================================================
// Types — Hevy API
// =============================================================================

interface HevyExerciseTemplate {
  id: string;
  title: string;
  type: string;
  primary_muscle_group: string;
  secondary_muscle_groups: string[];
  is_custom: boolean;
}

interface HevySet {
  type: string;
  weight_kg?: number | null;
  reps?: number | null;
  duration_seconds?: number | null;
  distance_meters?: number | null;
  rep_range?: { start: number; end: number };
}

interface HevyRoutineExercise {
  exercise_template_id: string;
  superset_id: number | null;
  rest_seconds: number | null;
  notes: string | null;
  sets: HevySet[];
}

// =============================================================================
// Types — Response
// =============================================================================

interface ExportResponse {
  routine_id: string;
  routine_title: string;
  exercises_matched: number;
  exercises_created: number;
  exercises_matched_by_algo: number;
  exercises_matched_by_ai: number;
}

// =============================================================================
// API key retrieval (server-side, via Vault RPC)
// =============================================================================

async function getHevyApiKey(userJwt: string): Promise<string> {
  // Call get_hevy_api_key() as the authenticated user so auth.uid() resolves correctly.
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: `Bearer ${userJwt}` } } },
  );
  const { data, error } = await supabase.rpc("get_hevy_api_key");
  if (error || !data) {
    throw new Error("Hevy API key not configured. Add it in Settings.");
  }
  return data as string;
}

// =============================================================================
// Hevy API helpers
// =============================================================================

const HEVY_BASE = "https://api.hevyapp.com/v1";

async function hevyFetch(
  path: string,
  apiKey: string,
  options: RequestInit = {},
): Promise<Response> {
  const res = await fetch(`${HEVY_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "api-key": apiKey,
      ...(options.headers ?? {}),
    },
  });
  return res;
}

async function fetchAllExerciseTemplates(
  apiKey: string,
): Promise<HevyExerciseTemplate[]> {
  const all: HevyExerciseTemplate[] = [];
  let page = 1;
  const pageSize = 100;

  while (true) {
    const res = await hevyFetch(
      `/exercise_templates?page=${page}&pageSize=${pageSize}`,
      apiKey,
    );
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Hevy API GET exercise_templates: ${res.status} ${text}`);
    }
    const json = await res.json();
    const templates = json.exercise_templates as HevyExerciseTemplate[];
    if (!templates || templates.length === 0) break;
    all.push(...templates);
    if (templates.length < pageSize) break;
    page++;
  }

  return all;
}

async function createCustomExercise(
  apiKey: string,
  exercise: ProgramExercise,
): Promise<string> {
  const res = await hevyFetch("/exercise_templates", apiKey, {
    method: "POST",
    body: JSON.stringify({
      exercise: {
        title: exercise.name,
        exercise_type: mapExerciseType(exercise.exercise_type),
        equipment_category: mapEquipmentCategory(exercise.equipment_category),
        muscle_group: mapMuscleGroup(exercise.muscle_group),
        other_muscles: [],
      },
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(
      `Hevy API POST exercise_templates (${exercise.name}): ${res.status} ${text}`,
    );
  }

  const json = await res.json();
  return String(json.id);
}

async function createRoutine(
  apiKey: string,
  title: string,
  exercises: HevyRoutineExercise[],
): Promise<{ id: string; title: string }> {
  const res = await hevyFetch("/routines", apiKey, {
    method: "POST",
    body: JSON.stringify({
      routine: {
        title,
        folder_id: null,
        notes: "",
        exercises,
      },
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Hevy API POST routines: ${res.status} ${text}`);
  }

  const json = await res.json();
  // Hevy returns either the Routine directly, wrapped in `routine`, or as an array.
  let r = json.routine ?? json;
  if (Array.isArray(r)) r = r[0];
  if (!r || typeof r !== "object") {
    throw new Error(`Hevy API POST routines: unexpected response shape ${JSON.stringify(json)}`);
  }
  return {
    id: r.id != null ? String(r.id) : "",
    title: r.title != null ? String(r.title) : title,
  };
}

// =============================================================================
// Exercise type / equipment mapping to Hevy enums
// =============================================================================

function mapExerciseType(type: string): string {
  const map: Record<string, string> = {
    weight_reps: "weight_reps",
    bodyweight_reps: "bodyweight_reps",
    bodyweight_assisted_reps: "bodyweight_assisted_reps",
    duration: "duration",
    reps_only: "reps_only",
    distance_duration: "distance_duration",
    weight_duration: "weight_duration",
    short_distance_weight: "short_distance_weight",
  };
  return map[type] ?? "weight_reps";
}

function mapEquipmentCategory(cat: string): string {
  const map: Record<string, string> = {
    barbell: "barbell",
    dumbbell: "dumbbell",
    kettlebell: "kettlebell",
    machine: "machine",
    plate: "plate",
    resistance_band: "resistance_band",
    suspension: "suspension",
    cable: "machine",
    bodyweight: "none",
    other: "other",
    none: "none",
  };
  return map[cat] ?? "other";
}

// Hevy muscle_group enum (POST /v1/exercise_templates):
// abdominals|shoulders|biceps|triceps|forearms|quadriceps|hamstrings|calves|
// glutes|abductors|adductors|lats|upper_back|traps|lower_back|chest|cardio|neck|full_body|other
const HEVY_MUSCLE_GROUPS = new Set([
  "abdominals", "shoulders", "biceps", "triceps", "forearms",
  "quadriceps", "hamstrings", "calves", "glutes", "abductors", "adductors",
  "lats", "upper_back", "traps", "lower_back", "chest", "cardio", "neck",
  "full_body", "other",
]);

function mapMuscleGroup(group: string): string {
  const norm = (group ?? "").toLowerCase().trim().replace(/\s+/g, "_");
  if (HEVY_MUSCLE_GROUPS.has(norm)) return norm;
  // Common French/synonym mappings
  const synonyms: Record<string, string> = {
    abs: "abdominals",
    abdos: "abdominals",
    pectoraux: "chest",
    pecs: "chest",
    epaules: "shoulders",
    deltoides: "shoulders",
    dos: "upper_back",
    grand_dorsal: "lats",
    cuisses: "quadriceps",
    quadris: "quadriceps",
    ischios: "hamstrings",
    mollets: "calves",
    fessiers: "glutes",
    avant_bras: "forearms",
    trapezes: "traps",
    lombaires: "lower_back",
    cou: "neck",
    cardio_full: "cardio",
    corps_entier: "full_body",
  };
  return synonyms[norm] ?? "other";
}

// =============================================================================
// Exercise matching — Phase 1: Algorithmic
// =============================================================================

function normalize(s: string): string {
  return s
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

const SYNONYMS: Record<string, string[]> = {
  // English gym staples
  "bench press": ["barbell bench press", "bench press (barbell)"],
  "chest press": ["machine chest press", "chest press machine"],
  "shoulder press": [
    "machine shoulder press",
    "overhead press (machine)",
    "shoulder press machine",
  ],
  "leg press": ["leg press (machine)", "machine leg press", "leg press 45"],
  "leg curl": [
    "seated leg curl",
    "leg curl (machine)",
    "seated leg curl (machine)",
  ],
  "leg extension": ["leg extension (machine)", "machine leg extension"],
  "lat pulldown": [
    "lat pulldown (cable)",
    "cable lat pulldown",
    "tirage vertical",
  ],
  "low row": [
    "seated row",
    "seated row (cable)",
    "cable row",
    "rowing machine",
    "seated cable row",
  ],
  "reverse fly": [
    "reverse machine fly",
    "reverse pec deck",
    "pec deck reverse",
    "machine reverse fly",
  ],
  "lateral raise": [
    "lateral raise (machine)",
    "machine lateral raise",
    "elevation laterale",
  ],
  "triceps pushdown": [
    "tricep pushdown",
    "cable tricep pushdown",
    "extension triceps",
    "tricep extension",
    "triceps extension",
    "cable pushdown",
  ],
  "hammer curl": ["curl marteau", "dumbbell hammer curl"],
  "russian twist": ["russian twist (weighted)"],
  "farmer carry": ["farmer walk", "farmers walk", "farmers carry"],
  "pull up": ["pull-up", "chin up", "traction", "pullup"],
  dips: ["dip", "chest dip", "tricep dip"],
  "hip abduction": [
    "hip abduction (machine)",
    "machine hip abduction",
    "abduction machine",
  ],
  squat: ["v squat", "v-squat", "hack squat", "barbell squat"],
  lunge: ["walking lunge", "fentes", "fentes marchees", "dumbbell lunge"],
  "sled push": ["sled leg press", "prowler push"],
  "bird dog": ["bird dog planche"],
  "wall sit": ["chaise", "wall squat"],
  plank: ["planche"],
  "hanging knee raise": ["hanging l-sit", "hanging leg raise"],
  "push up": ["push-up", "pompe", "pompes", "pushup"],

  // French → English additions from Thomas's PDF
  "v squat": ["v-squat", "hack squat (machine)", "hack squat machine"],
  "press 45": ["leg press 45", "leg press (machine)", "machine leg press", "leg press"],
  "fentes marchees": ["walking lunge", "walking lunges", "dumbbell walking lunge", "lunge (dumbbell)"],
  "leg curl assis": ["seated leg curl", "seated leg curl (machine)", "leg curl (machine)"],
  "abduction machine": ["hip abduction (machine)", "machine hip abduction", "hip abduction"],
  "traction delestee": ["assisted pull up", "assisted pull-up", "pull up (assisted)", "pull up (band)"],
  "dips deleste": ["assisted dip", "assisted dips", "dip (assisted)", "chest dip (assisted)"],
  "tirage vertical poulie": ["lat pulldown (cable)", "lat pulldown", "cable lat pulldown"],
  "bench machine": ["chest press (machine)", "machine chest press", "chest press"],
  "row large uni": ["single arm row", "one arm row (machine)", "seated row (machine)"],
  "elevation laterale machine": ["lateral raise (machine)", "machine lateral raise", "lateral raise"],
  "curl marteau": ["hammer curl", "dumbbell hammer curl", "hammer curl (dumbbell)"],
  "extension triceps": ["tricep pushdown", "cable tricep pushdown", "triceps pushdown (cable)", "tricep extension"],
  "pompe": ["push up", "push-up", "pushup"],
  "dos nageur": ["prone y raise", "swimmers"],
  "chaise leste": ["wall sit", "wall sit (weighted)", "wall squat"],
  "l sit box": ["l-sit", "l sit"],
  "echo bike": ["assault bike", "air bike", "fan bike", "echo bike"],
  "shoulder tap": ["plank shoulder tap", "shoulder tap plank"],
  "escalier": ["stairmaster", "stair climber", "stair machine", "stairmill"],
  "rameur": ["rowing machine", "rower", "concept 2"],
  "pullover": ["dumbbell pullover", "pullover (dumbbell)"],
};

function algoMatch(
  exercise: ProgramExercise,
  templates: HevyExerciseTemplate[],
): HevyExerciseTemplate | null {
  const norm = normalize(exercise.name);

  // Step 1: Exact title match
  for (const t of templates) {
    if (normalize(t.title) === norm) return t;
  }

  // Step 2: Synonym match
  for (const [key, synonyms] of Object.entries(SYNONYMS)) {
    const allNames = [key, ...synonyms];
    const normAll = allNames.map(normalize);
    if (normAll.some((s) => s === norm || norm.includes(s) || s.includes(norm))) {
      for (const t of templates) {
        const nt = normalize(t.title);
        if (normAll.some((s) => nt === s || nt.includes(s) || s.includes(nt))) {
          return t;
        }
      }
    }
  }

  // Step 3: Contains match (either direction, minimum 4 chars)
  for (const t of templates) {
    const nt = normalize(t.title);
    if (nt.includes(norm) || norm.includes(nt)) {
      if (Math.min(nt.length, norm.length) >= 4) return t;
    }
  }

  // Step 4: Word overlap (at least 2 significant words match)
  const normWords = norm.split(" ").filter((w) => w.length > 2);
  if (normWords.length >= 2) {
    let bestTemplate: HevyExerciseTemplate | null = null;
    let bestOverlap = 0;
    for (const t of templates) {
      const tWords = normalize(t.title).split(" ").filter((w) => w.length > 2);
      const overlap = normWords.filter((w) => tWords.includes(w)).length;
      if (overlap >= 2 && overlap > bestOverlap) {
        bestOverlap = overlap;
        bestTemplate = t;
      }
    }
    if (bestTemplate) return bestTemplate;
  }

  return null;
}

// =============================================================================
// Exercise matching — Phase 2: AI (Claude)
// =============================================================================

async function aiMatchExercises(
  unmatched: { exercise: ProgramExercise; index: number }[],
  templates: HevyExerciseTemplate[],
): Promise<Map<number, string>> {
  // Filter templates to relevant muscle groups to keep prompt small
  const relevantMuscles = new Set(unmatched.map((u) => u.exercise.muscle_group));
  const filteredTemplates = templates.filter(
    (t) => relevantMuscles.has(t.primary_muscle_group) || !t.is_custom,
  );

  const prompt =
    `You are an expert exercise matcher for the Hevy fitness app.

Given a list of exercises from a workout PDF and a list of available Hevy exercise templates,
match each exercise to the BEST template. Consider:
1. The exercise name (may be in French or abbreviated)
2. The exercise type (weight_reps, bodyweight_reps, duration, etc.)
3. The equipment category
4. The primary muscle group

RULES:
- Only match if you are confident (>80% sure) it is the same exercise.
- If no good match exists, set template_id to null.
- Never invent template IDs — only use IDs from the available_templates list.
- Consider French-English translations (e.g. "Traction" = "Pull Up").
- Consider that machine names may vary between gyms.
- Return ONLY valid JSON, no explanation, no markdown code fence.
- Numbers MUST be standard decimals (e.g. 0.95). NEVER use scientific notation or trailing dots.
- Confidence MUST be a number between 0 and 1 with at most 2 decimal places.

OUTPUT format (exact):
[
  { "exercise_name": "...", "template_id": "abc123" | null, "confidence": 0.95 }
]`;

  const body = {
    model: "claude-sonnet-4-6",
    max_tokens: 4096,
    temperature: 0,
    messages: [
      {
        role: "user",
        content: `${prompt}\n\nUnmatched exercises:\n${
          JSON.stringify(
            unmatched.map((u) => ({
              name: u.exercise.name,
              exercise_type: u.exercise.exercise_type,
              equipment_category: u.exercise.equipment_category,
              muscle_group: u.exercise.muscle_group,
            })),
          )
        }\n\nAvailable templates:\n${
          JSON.stringify(
            filteredTemplates.map((t) => ({
              id: t.id,
              title: t.title,
              type: t.type,
              primary_muscle_group: t.primary_muscle_group,
            })),
          )
        }`,
      },
    ],
  };

  let res: Response;
  try {
    res = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": Deno.env.get("ANTHROPIC_API_KEY")!,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(body),
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`Claude API network error during exercise matching: ${msg}`);
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(
      `Claude API error during exercise matching: ${res.status} ${text}`,
    );
  }

  let json: { content: Array<{ text: string }> };
  try {
    json = await res.json();
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err);
    throw new Error(`Claude API returned invalid JSON during exercise matching: ${msg}`);
  }

  const content = json.content[0]?.text;
  if (!content) {
    throw new Error("Claude returned empty response during exercise matching");
  }

  // Extract the first JSON array from Claude's response — Claude sometimes wraps
  // it in markdown, prose, or whitespace. Bracket-match to find the array bounds.
  const cleaned = content.replace(/```json?\n?/g, "").replace(/```/g, "").trim();
  const arrStart = cleaned.indexOf("[");
  let arrEnd = -1;
  if (arrStart !== -1) {
    let depth = 0, inStr = false, esc = false;
    for (let i = arrStart; i < cleaned.length; i++) {
      const c = cleaned[i];
      if (esc) { esc = false; continue; }
      if (c === "\\" && inStr) { esc = true; continue; }
      if (c === '"') { inStr = !inStr; continue; }
      if (inStr) continue;
      if (c === "[") depth++;
      else if (c === "]") { depth--; if (depth === 0) { arrEnd = i; break; } }
    }
  }
  if (arrStart === -1 || arrEnd === -1) {
    console.error("[aiMatchExercises] Claude raw response:", content);
    throw new Error("Claude API error during exercise matching: response did not contain a JSON array");
  }
  const jsonStr = cleaned.substring(arrStart, arrEnd + 1);

  let matches: Array<{
    exercise_name: string;
    template_id: string | null;
    confidence: number;
  }>;
  try {
    matches = JSON.parse(jsonStr);
  } catch (_err) {
    // Attempt to repair common Claude JSON quirks: trailing commas, malformed
    // exponents (e.g. "1e," -> "1,"), unquoted special values.
    const repaired = jsonStr
      .replace(/,\s*([}\]])/g, "$1")                 // trailing commas
      .replace(/(\d+)e(?![+\-\d])/g, "$1")           // "1e" with no exponent number → "1"
      .replace(/(\d+)\.(?=[,}\]\s])/g, "$1");        // "1." with nothing after → "1"
    try {
      matches = JSON.parse(repaired);
      console.warn("[aiMatchExercises] Used repaired JSON (original had quirks)");
    } catch (err2) {
      const msg = err2 instanceof Error ? err2.message : String(err2);
      console.error("[aiMatchExercises] Failed to parse. Original:", jsonStr);
      console.error("[aiMatchExercises] Repaired attempt:", repaired);
      throw new Error(`Claude API error during exercise matching: unparseable JSON (${msg})`);
    }
  }

  const templateIds = new Set(templates.map((t) => t.id));
  const result = new Map<number, string>();

  for (let i = 0; i < matches.length && i < unmatched.length; i++) {
    const m = matches[i];
    if (m.template_id && m.confidence >= 0.7 && templateIds.has(m.template_id)) {
      result.set(unmatched[i].index, m.template_id);
    }
  }

  return result;
}

// =============================================================================
// Warmup section detection
// =============================================================================

function isWarmupSection(sectionName: string): boolean {
  const norm = normalize(sectionName);
  return ["echauffement", "warmup", "mobilite", "mobility", "warm up"].some(
    (w) => norm.includes(w),
  );
}

// =============================================================================
// Set building
// =============================================================================

function buildSets(exercise: ProgramExercise, warmup: boolean): HevySet[] {
  const sets: HevySet[] = [];
  const repsStr = exercise.reps;

  for (let i = 0; i < exercise.sets; i++) {
    const set: HevySet = { type: warmup ? "warmup" : "normal" };

    if (repsStr === "failure") {
      set.type = "failure";
      set.reps = 0;
    } else if (/^\d+-\d+$/.test(repsStr)) {
      const [start, end] = repsStr.split("-").map(Number);
      set.rep_range = { start, end };
    } else if (/^\d+s$/i.test(repsStr)) {
      set.duration_seconds = parseInt(repsStr);
    } else if (/^\d+min$/i.test(repsStr)) {
      set.duration_seconds = parseInt(repsStr) * 60;
    } else if (/^\d+m$/i.test(repsStr)) {
      set.distance_meters = parseInt(repsStr);
    } else if (/^\d+kcal$/i.test(repsStr)) {
      set.duration_seconds = 300;
    } else {
      const n = parseInt(repsStr);
      if (!isNaN(n)) {
        set.reps = n;
      }
    }

    // Per-set weight (new) with fallback to uniform weight
    if (
      exercise.weight_per_set &&
      exercise.weight_per_set.length > i &&
      exercise.weight_per_set[i] != null
    ) {
      set.weight_kg = exercise.weight_per_set[i];
    } else if (exercise.weight_kg != null) {
      set.weight_kg = exercise.weight_kg;
    }

    sets.push(set);
  }

  return sets;
}

// =============================================================================
// Main export logic
// =============================================================================

async function exportSession(
  session: WorkoutSession,
  apiKey: string,
): Promise<ExportResponse> {
  const templates = await fetchAllExerciseTemplates(apiKey);

  // Flatten exercises while retaining section context for warmup detection
  const indexedExercises: { exercise: ProgramExercise; index: number; warmup: boolean }[] = [];
  for (const section of session.sections) {
    const warmup = isWarmupSection(section.name);
    for (const exercise of section.exercises) {
      indexedExercises.push({ exercise, index: indexedExercises.length, warmup });
    }
  }

  // Phase 1: Algorithmic matching
  const matched: { entry: typeof indexedExercises[0]; templateId: string }[] = [];
  const unmatched: { exercise: ProgramExercise; index: number }[] = [];

  for (const entry of indexedExercises) {
    const match = algoMatch(entry.exercise, templates);
    if (match) {
      matched.push({ entry, templateId: match.id });
    } else {
      unmatched.push({ exercise: entry.exercise, index: entry.index });
    }
  }

  const matchedByAlgo = matched.length;
  let matchedByAi = 0;
  let created = 0;

  // Phase 2: AI matching for unmatched exercises
  // Any error here is a hard fail — no silent fallback
  const aiResults = new Map<number, string>();
  if (unmatched.length > 0) {
    try {
      const result = await aiMatchExercises(unmatched, templates);
      for (const [idx, templateId] of result.entries()) {
        aiResults.set(idx, templateId);
      }
      matchedByAi = aiResults.size;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      console.error("[export-to-hevy] AI matching failed:", msg);
      throw err;
    }
  }

  // Phase 3: Auto-create custom exercises for still-unmatched
  const templateIdByIndex = new Map<number, string>();

  for (const { entry, templateId } of matched) {
    templateIdByIndex.set(entry.index, templateId);
  }

  for (const { exercise, index } of unmatched) {
    if (aiResults.has(index)) {
      templateIdByIndex.set(index, aiResults.get(index)!);
    } else {
      const newId = await createCustomExercise(apiKey, exercise);
      created++;
      templateIdByIndex.set(index, newId);
      templates.push({
        id: newId,
        title: exercise.name,
        type: exercise.exercise_type,
        primary_muscle_group: exercise.muscle_group,
        secondary_muscle_groups: [],
        is_custom: true,
      });
    }
  }

  // Build routine exercises in original order
  const routineExercises: HevyRoutineExercise[] = indexedExercises.map(
    ({ exercise, index, warmup }) => ({
      exercise_template_id: templateIdByIndex.get(index)!,
      superset_id: exercise.superset_group,
      rest_seconds: exercise.rest_seconds > 0 ? exercise.rest_seconds : null,
      notes: exercise.notes,
      sets: buildSets(exercise, warmup),
    }),
  );

  const routine = await createRoutine(apiKey, session.name, routineExercises);

  return {
    routine_id: routine.id,
    routine_title: routine.title,
    exercises_matched: matchedByAlgo + matchedByAi,
    exercises_created: created,
    exercises_matched_by_algo: matchedByAlgo,
    exercises_matched_by_ai: matchedByAi,
  };
}

// =============================================================================
// Handler
// =============================================================================

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  try {
    if (checkPayloadSize(req, 1 * 1024 * 1024) === -1) {
      return json({ error: "Payload too large" }, 413);
    }

    const { userId } = await requireUser(req);
    const userJwt = req.headers.get("Authorization")!.replace(/^Bearer\s+/i, "").trim();

    if (!(await checkRateLimit(userId, "export-to-hevy", 5, 60))) {
      return json({ error: "Rate limit exceeded" }, 429);
    }

    let hevyApiKey: string;
    try {
      hevyApiKey = await getHevyApiKey(userJwt);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      return json({ error: msg }, 400);
    }

    let body: ExportRequest;
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON body" }, 400);
    }

    if (!body.session) {
      return json({ error: "Missing session" }, 400);
    }

    const result = await exportSession(body.session, hevyApiKey);

    return json(result, 200);
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token") ||
      message.includes("Missing Authorization") ||
      message.includes("Empty Bearer token")
    ) {
      return json({ error: message }, 401);
    }

    if (message.includes("Hevy API")) {
      console.error("[export-to-hevy] Hevy API error:", message);
      return json({ error: "External service unavailable" }, 502);
    }

    if (message.includes("Claude API")) {
      console.error("[export-to-hevy] Claude API error:", message);
      return json({ error: "AI service error" }, 500);
    }

    console.error("[export-to-hevy] error:", message, error instanceof Error ? error.stack : "");
    return json({ error: "Internal server error" }, 500);
  }
});
