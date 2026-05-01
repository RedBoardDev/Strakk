import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit, checkPayloadSize } from "../_shared/rate-limit.ts";
import { callClaude, stripMarkdownFences, type ClaudeContent } from "../_shared/claude.ts";

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
11. weight_kg: set to the most common value in weight_per_set (see rule 16), or null if empty.
12. Keep exercise names in their ORIGINAL language from the PDF. Do NOT translate.
13. Include ALL exercises from ALL sections (warmup, reinforcement, finisher, etc.).
14. For exercises done "à l'échec" (to failure), set reps to "failure".
15. notes: Combine key coaching cues (max 2 sentences), tempo instructions, and grip/stance
    variations into a single notes string separated by " | ". Do NOT include full paragraphs.
    Examples:
    - "Tempo: slow and controlled | Pull bar to upper chest, squeeze shoulder blades"
    - "S1+S2: pronation grip, S3+S4: supination grip | Hold eccentric as long as possible"
    If no coaching info is present, set notes to null.
16. weight_per_set: array of weights in kg, one per set. Length MUST equal sets count.
    - If the PDF says "S1+S2: 10kg, S3: 15kg" with 3 sets → [10, 10, 15]
    - If the PDF says "10kg/cote" on a machine with 3 sets → [20, 20, 20] (doubled for bilateral machines)
    - If the PDF says "2,5kg/main" with dumbbells → [2.5, 2.5, 2.5] (per-hand, NOT doubled)
    - If no weight specified → []
    Also set weight_kg to the most common value in the array (or null if empty).

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
              "weight_kg": 15.0,
              "weight_per_set": [10.0, 10.0, 15.0],
              "rest_seconds": 90,
              "notes": "Tempo: slow and controlled | Pull bar to upper chest",
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
// Claude invocation (wraps _shared/callClaude with PDF-specific payload)
// =============================================================================

// The PDF document content block is not part of the shared ClaudeContent union
// (which only covers text and image). We cast it so callClaude can forward it
// to the API unchanged — the shape is still valid at runtime.
type PdfDocumentBlock = {
  type: "document";
  source: { type: "base64"; media_type: "application/pdf"; data: string };
};

async function parsePdfWithClaude(pdfBase64: string): Promise<string> {
  const pdfBlock: PdfDocumentBlock = {
    type: "document",
    source: { type: "base64", media_type: "application/pdf", data: pdfBase64 },
  };
  const textBlock: ClaudeContent = { type: "text", text: EXTRACTION_PROMPT };

  return await callClaude({
    maxTokens: 16384,
    temperature: 0,
    messages: [
      {
        role: "user",
        content: [pdfBlock as unknown as ClaudeContent, textBlock],
      },
    ],
  });
}

// =============================================================================
// Response parsing + coercion
// =============================================================================

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

function coerceWeightPerSet(raw: unknown, sets: number): (number | null)[] {
  if (!Array.isArray(raw)) return [];
  const coerced = raw.map((v) => coerceNullableNumber(v));
  if (coerced.length === sets) return coerced;
  if (coerced.length === 0) return [];
  // Pad with last value or truncate to match sets count
  const last = coerced[coerced.length - 1];
  while (coerced.length < sets) coerced.push(last);
  return coerced.slice(0, sets);
}

function coerceExercise(raw: unknown): ProgramExercise {
  const r = (raw ?? {}) as Record<string, unknown>;
  const sets = Math.max(1, coerceNumber(r.sets, 1));
  const weight_per_set = coerceWeightPerSet(r.weight_per_set, sets);
  return {
    name: coerceString(r.name, "Unknown exercise"),
    sets,
    reps: coerceString(r.reps, "1"),
    weight_kg: coerceNullableNumber(r.weight_kg),
    weight_per_set,
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
    if (checkPayloadSize(req, 10 * 1024 * 1024) === -1) {
      return new Response(JSON.stringify({ error: "Payload too large (max 10 MB)" }), {
        status: 413,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    const { userId } = await requireUser(req);

    if (!(await checkRateLimit(userId, "parse-workout-pdf", 5, 60))) {
      return new Response(JSON.stringify({ error: "Rate limit exceeded" }), {
        status: 429,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

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

    if (body.pdf_base64.length > 10 * 1024 * 1024) {
      return new Response(
        JSON.stringify({ error: "pdf_base64 too large (max ~7.5 MB decoded)" }),
        {
          status: 413,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    const raw = await parsePdfWithClaude(body.pdf_base64);
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
