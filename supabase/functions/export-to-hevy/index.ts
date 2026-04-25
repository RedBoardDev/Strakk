import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";

// =============================================================================
// Types — Input
// =============================================================================

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

interface ExportRequest {
  session: WorkoutSession;
  hevy_api_key: string;
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
      exercise_template: {
        title: exercise.name,
        exercise_type: mapExerciseType(exercise.exercise_type),
        equipment_category: mapEquipmentCategory(exercise.equipment_category),
        muscle_group: exercise.muscle_group,
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
  return json.exercise_template.id;
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
  return { id: json.routine.id, title: json.routine.title };
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

// =============================================================================
// Exercise matching
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
};

function findBestMatch(
  exerciseName: string,
  templates: HevyExerciseTemplate[],
): HevyExerciseTemplate | null {
  const norm = normalize(exerciseName);

  // 1. Exact title match
  for (const t of templates) {
    if (normalize(t.title) === norm) return t;
  }

  // 2. Synonym match
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

  // 3. Contains match (either direction)
  for (const t of templates) {
    const nt = normalize(t.title);
    if (nt.includes(norm) || norm.includes(nt)) {
      if (Math.min(nt.length, norm.length) >= 4) return t;
    }
  }

  // 4. Word overlap (at least 2 significant words match)
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
// Set building
// =============================================================================

function buildSets(exercise: ProgramExercise): HevySet[] {
  const sets: HevySet[] = [];
  const repsStr = exercise.reps;

  for (let i = 0; i < exercise.sets; i++) {
    const set: HevySet = { type: "normal" };

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

    if (exercise.weight_kg != null) {
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

  const allExercises = session.sections.flatMap((s) => s.exercises);
  let matched = 0;
  let created = 0;

  const routineExercises: HevyRoutineExercise[] = [];

  for (const exercise of allExercises) {
    let templateId: string;

    const match = findBestMatch(exercise.name, templates);
    if (match) {
      templateId = match.id;
      matched++;
    } else {
      templateId = await createCustomExercise(apiKey, exercise);
      created++;
      templates.push({
        id: templateId,
        title: exercise.name,
        type: exercise.exercise_type,
        primary_muscle_group: exercise.muscle_group,
        secondary_muscle_groups: [],
        is_custom: true,
      });
    }

    routineExercises.push({
      exercise_template_id: templateId,
      superset_id: exercise.superset_group,
      rest_seconds: exercise.rest_seconds > 0 ? exercise.rest_seconds : null,
      notes: exercise.notes,
      sets: buildSets(exercise),
    });
  }

  const routine = await createRoutine(apiKey, session.name, routineExercises);

  return {
    routine_id: routine.id,
    routine_title: routine.title,
    exercises_matched: matched,
    exercises_created: created,
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

    let body: ExportRequest;
    try {
      body = await req.json();
    } catch {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (!body.session || !body.hevy_api_key) {
      return new Response(
        JSON.stringify({ error: "Missing session or hevy_api_key" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    if (typeof body.hevy_api_key !== "string" || body.hevy_api_key.length < 10) {
      return new Response(
        JSON.stringify({ error: "Invalid hevy_api_key format" }),
        {
          status: 400,
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        },
      );
    }

    const result = await exportSession(body.session, body.hevy_api_key);

    return new Response(JSON.stringify(result), {
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

    if (message.includes("Hevy API")) {
      return new Response(JSON.stringify({ error: message }), {
        status: 502,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    console.error("export-to-hevy error:", message);
    return new Response(JSON.stringify({ error: "Internal server error" }), {
      status: 500,
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  }
});
