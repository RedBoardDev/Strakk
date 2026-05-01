import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit, checkPayloadSize } from "../_shared/rate-limit.ts";
import { callClaude, stripMarkdownFences, HAIKU_MODEL } from "../_shared/claude.ts";

interface GoalsRequest {
  weight_kg: number;
  height_cm?: number | null;
  age?: number | null;
  biological_sex?: string | null;
  fitness_goal?: string | null;
  training_frequency_per_week?: number | null;
  training_types?: string[] | null;
  training_intensity?: string | null;
  daily_activity_level?: string | null;
}

interface GoalsResponse {
  protein_g: number;
  calories_kcal: number;
  fat_g: number;
  carbs_g: number;
  water_ml: number;
  reasoning: string;
}

const SYSTEM_PROMPT = `You are a certified sports nutritionist. Given a user's physical profile and activity level, calculate their recommended daily nutrition targets.

Rules:
- Use the Mifflin-St Jeor equation for BMR when height, age, and sex are available
- Apply appropriate activity multipliers for TDEE:
  - Sedentary (office job, no training): 1.2
  - Light (1-2 sessions/week, light intensity): 1.375
  - Moderate (3-4 sessions/week, moderate intensity): 1.55
  - Active (5-6 sessions/week, intense): 1.725
  - Very active (daily intense training + physical job): 1.9
- Adjust based on the user's fitness goal:
  - lose_fat: TDEE - 300 to 500 kcal deficit
  - gain_muscle: TDEE + 200 to 400 kcal surplus
  - maintain / just_track: TDEE
- Protein: 1.6-2.2g per kg bodyweight for active individuals, 1.2-1.6g for sedentary
- Fat: 25-35% of total calories
- Carbs: remaining calories after protein and fat
- Water: 30-40ml per kg bodyweight, adjusted for activity
- If data is missing, use reasonable defaults for an average adult
- Round all values to clean numbers (multiples of 5 for grams, 50 for calories, 100 for water)

Return ONLY a JSON object with these exact keys:
protein_g, calories_kcal, fat_g, carbs_g, water_ml, reasoning

The reasoning field should be 1-2 sentences in French explaining the recommendation.`;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function buildUserMessage(input: GoalsRequest): string {
  const parts: string[] = [`Weight: ${input.weight_kg} kg`];

  if (input.height_cm) parts.push(`Height: ${input.height_cm} cm`);
  if (input.age) parts.push(`Age: ${input.age}`);
  if (input.biological_sex) parts.push(`Biological sex: ${input.biological_sex}`);
  if (input.fitness_goal) parts.push(`Fitness goal: ${input.fitness_goal}`);
  if (input.training_frequency_per_week != null) {
    parts.push(`Training frequency: ${input.training_frequency_per_week} sessions/week`);
  }
  if (input.training_types?.length) {
    parts.push(`Training types: ${input.training_types.join(", ")}`);
  }
  if (input.training_intensity) parts.push(`Training intensity: ${input.training_intensity}`);
  if (input.daily_activity_level) parts.push(`Daily activity level: ${input.daily_activity_level}`);

  return parts.join("\n");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  try {
    if (checkPayloadSize(req, 8 * 1024) === -1) {
      return jsonResponse({ error: "Payload too large" }, 413);
    }

    const { userId } = await requireUser(req);

    if (!(await checkRateLimit(userId, "calculate-goals", 3, 600))) {
      return jsonResponse({ error: "Rate limit exceeded" }, 429);
    }

    let body: GoalsRequest;
    try {
      body = await req.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    if (typeof body.weight_kg !== "number" || body.weight_kg <= 0) {
      return jsonResponse({ error: "weight_kg is required and must be a positive number" }, 400);
    }

    const userMessage = buildUserMessage(body);

    const rawResponse = await callClaude({
      messages: [{ role: "user", content: [{ type: "text", text: userMessage }] }],
      system: SYSTEM_PROMPT,
      model: HAIKU_MODEL,
      maxTokens: 512,
      temperature: 0.2,
    });

    const cleaned = stripMarkdownFences(rawResponse);
    let parsed: GoalsResponse;
    try {
      parsed = JSON.parse(cleaned);
    } catch {
      console.error("Failed to parse Claude response:", cleaned);
      return jsonResponse({ error: "AI returned invalid response" }, 502);
    }

    if (
      typeof parsed.protein_g !== "number" ||
      typeof parsed.calories_kcal !== "number" ||
      typeof parsed.fat_g !== "number" ||
      typeof parsed.carbs_g !== "number" ||
      typeof parsed.water_ml !== "number"
    ) {
      console.error("Claude response missing fields:", parsed);
      return jsonResponse({ error: "AI returned incomplete response" }, 502);
    }

    return jsonResponse({
      protein_g: parsed.protein_g,
      calories_kcal: parsed.calories_kcal,
      fat_g: parsed.fat_g,
      carbs_g: parsed.carbs_g,
      water_ml: parsed.water_ml,
      reasoning: parsed.reasoning ?? "",
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token") ||
      message.includes("Empty Bearer")
    ) {
      return jsonResponse({ error: message }, 401);
    }

    if (message.includes("Claude API") || message.includes("Claude returned")) {
      return jsonResponse({ error: message }, 502);
    }

    console.error("calculate-goals error:", message);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});
