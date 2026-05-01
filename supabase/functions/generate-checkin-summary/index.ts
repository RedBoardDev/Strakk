import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit, checkPayloadSize } from "../_shared/rate-limit.ts";
import { generateCheckinSummary, CheckinSummaryInput } from "../_shared/checkin-summary.ts";

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  try {
    if (checkPayloadSize(req, 256 * 1024) === -1) {
      return jsonResponse({ error: "Payload too large" }, 413);
    }

    const { userId } = await requireUser(req);

    if (!(await checkRateLimit(userId, "generate-checkin-summary", 5, 60))) {
      return jsonResponse({ error: "Rate limit exceeded" }, 429);
    }

    let body: Record<string, unknown>;
    try {
      body = await req.json();
    } catch {
      return jsonResponse({ error: "Invalid JSON body" }, 400);
    }

    const input = parseInput(body);
    const summary = await generateCheckinSummary(input);
    return jsonResponse({ summary });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token") ||
      message.includes("Empty Bearer")
    ) {
      return jsonResponse({ error: message }, 401);
    }

    if (message.startsWith("BAD_INPUT:")) {
      return jsonResponse({ error: message.slice(10).trim() }, 400);
    }

    if (message.includes("Claude API") || message.includes("Claude returned")) {
      return jsonResponse({ error: message }, 502);
    }

    console.error("generate-checkin-summary error:", message);
    return jsonResponse({ error: "Internal server error" }, 500);
  }
});

function parseInput(body: Record<string, unknown>): CheckinSummaryInput {
  const avgProtein = body.avg_protein;
  const avgCalories = body.avg_calories;
  const avgFat = body.avg_fat;
  const avgCarbs = body.avg_carbs;
  const avgWater = body.avg_water;
  const nutritionDays = body.nutrition_days;

  if (typeof avgProtein !== "number" || typeof avgCalories !== "number") {
    throw new Error("BAD_INPUT: avg_protein and avg_calories are required numbers");
  }

  const rawFoods = body.top_foods;
  const topFoods: string[] = Array.isArray(rawFoods)
    ? rawFoods.filter((f): f is string => typeof f === "string")
    : [];

  const rawProteinPerDay = body.protein_per_day;
  const proteinPerDay: number[] = Array.isArray(rawProteinPerDay)
    ? rawProteinPerDay.filter((v): v is number => typeof v === "number")
    : [];

  const daysWithWater = typeof body.days_with_water === "number" ? body.days_with_water : 0;

  const weightKg = typeof body.weight_kg === "number" ? body.weight_kg : null;

  const rawTags = body.feeling_tags;
  const feelingTags: string[] = Array.isArray(rawTags)
    ? rawTags.filter((t): t is string => typeof t === "string")
    : [];

  const mentalFeeling = typeof body.mental_feeling === "string" ? body.mental_feeling : "";
  const physicalFeeling = typeof body.physical_feeling === "string" ? body.physical_feeling : "";

  const goals = body.goals as Record<string, unknown> | undefined;

  return {
    avgProtein,
    avgCalories,
    avgFat: typeof avgFat === "number" ? avgFat : 0,
    avgCarbs: typeof avgCarbs === "number" ? avgCarbs : 0,
    avgWater: typeof avgWater === "number" ? avgWater : 0,
    nutritionDays: typeof nutritionDays === "number" ? nutritionDays : 0,
    topFoods,
    proteinPerDay,
    daysWithWater,
    weightKg,
    feelingTags,
    mentalFeeling,
    physicalFeeling,
    goals: goals ? {
      proteinGoal: typeof goals.protein_goal === "number" ? goals.protein_goal : null,
      calorieGoal: typeof goals.calorie_goal === "number" ? goals.calorie_goal : null,
      waterGoal: typeof goals.water_goal === "number" ? goals.water_goal : null,
    } : null,
  };
}
