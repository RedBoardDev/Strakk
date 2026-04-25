import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";

// =============================================================================
// Types
// =============================================================================

interface ParseRequest {
  pdf_base64: string;
}

interface ProgramExercise {
  name: string;
  sets: number;
  reps: string;
  weight_kg: number | null;
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

interface ParseResponse {
  program_name: string;
  sessions: WorkoutSession[];
}

// =============================================================================
// Prompt
// =============================================================================

const EXTRACTION_PROMPT = `You are a professional workout program parser. Extract ALL workout sessions from this PDF into structured JSON.

RULES:
1. Preserve the session structure exactly as in the PDF (session name, sections, exercises).
2. Each session typically has sections: Échauffement/Warmup, Renforcement/Strength, Finisher, etc. Use the names from the PDF.
3. For circuit/superset groupings (e.g. "Réaliser les exercices à la suite. Faire X tours"):
   - Assign the SAME superset_group integer to ALL exercises in that circuit.
   - Start superset_group numbering at 1 within each section, increment for each new circuit.
   - Exercises NOT in a circuit have superset_group: null.
4. Exercise type inference:
   - "weight_reps": weight is specified or clearly implied (machines, barbells, dumbbells with load)
   - "bodyweight_reps": bodyweight exercise with rep count (push-ups, pull-ups, dips without added weight)
   - "bodyweight_assisted_reps": assisted bodyweight exercise (assisted pull-ups, assisted dips)
   - "duration": time-based exercises (planks, holds, stretches, cardio machines with duration)
   - "distance_duration": distance + time exercises (sled push with distance, farmer carry)
   - "reps_only": reps but unclear if weighted
5. Equipment category (use exactly these values):
   "barbell", "dumbbell", "kettlebell", "machine", "plate", "resistance_band", "suspension", "other", "none"
6. Primary muscle group (use exactly these values):
   "abdominals", "shoulders", "biceps", "triceps", "forearms", "quadriceps", "hamstrings", "calves",
   "glutes", "abductors", "adductors", "lats", "upper_back", "traps", "lower_back", "chest", "cardio",
   "neck", "full_body", "other"
7. Convert ALL rest times to seconds (e.g. "1'30" = 90, "1'" = 60, "30''" = 30).
8. For rep ranges like "10 à 12" or "10-12", output as string "10-12".
9. For duration-based exercises, put the duration as reps string (e.g. "30s", "5min", "10kcal").
10. For distance-based exercises (e.g. "20m"), put as reps string "20m".
11. weight_kg: the total weight in kg. For "10kg/côté" on machines or barbells, double it to 20.
    For "2,5kg/main" with dumbbells, keep per-hand value as-is (2.5) since Hevy tracks per-dumbbell.
    null if bodyweight or unspecified.
12. Keep exercise names in their ORIGINAL language from the PDF. Do NOT translate.
13. Include ALL exercises from ALL sections (warmup, reinforcement, finisher, etc.).
14. For exercises done "à l'échec" (to failure), set reps to "failure".

OUTPUT: Valid JSON matching this exact schema, nothing else:
{
  "program_name": "string",
  "sessions": [
    {
      "name": "string",
      "sections": [
        {
          "name": "string",
          "exercises": [
            {
              "name": "string",
              "sets": 3,
              "reps": "10-12",
              "weight_kg": 20.0,
              "rest_seconds": 90,
              "notes": "string or null",
              "superset_group": 1,
              "exercise_type": "weight_reps",
              "equipment_category": "machine",
              "muscle_group": "quadriceps"
            }
          ]
        }
      ]
    }
  ]
}`;

// =============================================================================
// Claude API
// =============================================================================

const CLAUDE_MODEL = "claude-sonnet-4-6";
const MAX_RETRIES = 3;
const BASE_DELAY_MS = 2000;

async function callClaude(pdfBase64: string): Promise<string> {
  const apiKey = Deno.env.get("ANTHROPIC_API_KEY");
  if (!apiKey) throw new Error("ANTHROPIC_API_KEY not configured");

  const body = {
    model: CLAUDE_MODEL,
    max_tokens: 16384,
    temperature: 0,
    messages: [
      {
        role: "user",
        content: [
          {
            type: "document",
            source: {
              type: "base64",
              media_type: "application/pdf",
              data: pdfBase64,
            },
          },
          {
            type: "text",
            text: EXTRACTION_PROMPT,
          },
        ],
      },
    ],
  };

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    if (attempt > 0) {
      const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), 15000);
      await new Promise((r) => setTimeout(r, delay));
    }

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(body),
    });

    if ([429, 503, 529].includes(response.status)) {
      continue;
    }

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`Claude API HTTP ${response.status}: ${text}`);
    }

    const json = await response.json();
    const contentArray = json.content as Array<{ type: string; text?: string }>;
    const textBlock = contentArray.find((c) => c.type === "text");
    if (!textBlock?.text) {
      throw new Error("No text in Claude response");
    }

    return textBlock.text;
  }

  throw new Error("Claude API unavailable after retries");
}

// =============================================================================
// Response parsing + coercion
// =============================================================================

function stripMarkdownFences(text: string): string {
  const match = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (match && match[1]) return match[1].trim();
  return text.trim();
}

function coerceNumber(v: unknown, fallback = 0): number {
  if (typeof v === "number" && !Number.isNaN(v)) return v;
  if (typeof v === "string") {
    const parsed = Number(v);
    if (!Number.isNaN(parsed)) return parsed;
  }
  return fallback;
}

function coerceString(v: unknown, fallback = ""): string {
  return typeof v === "string" ? v : fallback;
}

function coerceNullableNumber(v: unknown): number | null {
  if (v === null || v === undefined) return null;
  if (typeof v === "number" && !Number.isNaN(v)) return v;
  if (typeof v === "string") {
    const parsed = Number(v);
    if (!Number.isNaN(parsed)) return parsed;
  }
  return null;
}

function coerceNullableInt(v: unknown): number | null {
  const n = coerceNullableNumber(v);
  return n !== null ? Math.round(n) : null;
}

function coerceExercise(raw: unknown): ProgramExercise {
  const r = (raw ?? {}) as Record<string, unknown>;
  return {
    name: coerceString(r.name, "Unknown exercise"),
    sets: Math.max(1, coerceNumber(r.sets, 1)),
    reps: coerceString(r.reps, "1"),
    weight_kg: coerceNullableNumber(r.weight_kg),
    rest_seconds: coerceNumber(r.rest_seconds, 0),
    notes: r.notes != null ? coerceString(r.notes) : null,
    superset_group: coerceNullableInt(r.superset_group),
    exercise_type: coerceString(r.exercise_type, "weight_reps"),
    equipment_category: coerceString(r.equipment_category, "other"),
    muscle_group: coerceString(r.muscle_group, "other"),
  };
}

function coerceSection(raw: unknown): ExerciseSection {
  const r = (raw ?? {}) as Record<string, unknown>;
  const exercises = Array.isArray(r.exercises) ? r.exercises.map(coerceExercise) : [];
  return {
    name: coerceString(r.name, "Exercises"),
    exercises,
  };
}

function coerceSession(raw: unknown): WorkoutSession {
  const r = (raw ?? {}) as Record<string, unknown>;
  const sections = Array.isArray(r.sections) ? r.sections.map(coerceSection) : [];
  return {
    name: coerceString(r.name, "Session"),
    sections,
  };
}

function parseClaudeResponse(text: string): ParseResponse {
  const stripped = stripMarkdownFences(text);
  let parsed: unknown;
  try {
    parsed = JSON.parse(stripped);
  } catch (_err) {
    throw new Error(`Failed to parse Claude JSON: ${stripped.slice(0, 300)}`);
  }

  const obj = (parsed ?? {}) as Record<string, unknown>;
  const rawSessions = Array.isArray(obj.sessions) ? obj.sessions : [];

  return {
    program_name: coerceString(obj.program_name, "Workout Program"),
    sessions: rawSessions.map(coerceSession),
  };
}

// =============================================================================
// Handler
// =============================================================================

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), {
      status: 405,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }

  try {
    await requireUser(req);

    let body: ParseRequest;
    try {
      body = await req.json();
    } catch {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!body.pdf_base64 || typeof body.pdf_base64 !== "string") {
      return new Response(
        JSON.stringify({ error: "Missing or invalid pdf_base64" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    const raw = await callClaude(body.pdf_base64);
    const parsed = parseClaudeResponse(raw);

    if (parsed.sessions.length === 0) {
      return new Response(
        JSON.stringify({ error: "No workout sessions found in the PDF" }),
        {
          status: 422,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    return new Response(JSON.stringify(parsed), {
      status: 200,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);

    if (
      message.includes("Authorization") ||
      message.includes("Invalid or expired token")
    ) {
      return new Response(JSON.stringify({ error: message }), {
        status: 401,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (message.includes("Claude API") || message.includes("parse Claude")) {
      return new Response(JSON.stringify({ error: message }), {
        status: 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    console.error("parse-workout-pdf error:", message);
    return new Response(JSON.stringify({ error: "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
