import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { getHevyApiKey, hevyFetch } from "../_shared/hevy.ts";

// =============================================================================
// Types
// =============================================================================

interface FetchRequest {
  start_date: string; // ISO date e.g. "2026-05-05"
  end_date: string;   // ISO date e.g. "2026-05-11"
}

interface HevyWorkoutSet {
  type: string;
  weight_kg: number | null;
  reps: number | null;
  duration_seconds: number | null;
  rpe: number | null;
}

interface HevyWorkoutExercise {
  title: string;
  exercise_template_id: string;
  superset_id: number | null;
  sets: HevyWorkoutSet[];
}

interface HevyWorkoutRaw {
  id: string;
  title: string;
  start_time: string;
  end_time: string;
  exercises: HevyWorkoutExercise[];
}

interface WorkoutExerciseOut {
  name: string;
  muscle_group: string;
  superset_id: number | null;
  sets: {
    type: string;
    weight_kg: number | null;
    reps: number | null;
    duration_seconds: number | null;
    rpe: number | null;
  }[];
}

interface WorkoutOut {
  id: string;
  title: string;
  date: string;
  duration_minutes: number;
  total_volume_kg: number;
  exercises: WorkoutExerciseOut[];
}

// =============================================================================
// Hevy API: fetch exercise templates for muscle group resolution
// =============================================================================

interface HevyExerciseTemplate {
  id: string;
  title: string;
  primary_muscle_group: string;
}

async function fetchExerciseTemplatesMap(
  apiKey: string,
): Promise<Map<string, HevyExerciseTemplate>> {
  const map = new Map<string, HevyExerciseTemplate>();
  let page = 1;
  const pageSize = 100;

  while (true) {
    const res = await hevyFetch(
      `/exercise_templates?page=${page}&pageSize=${pageSize}`,
      apiKey,
    );
    if (!res.ok) break;
    const json = await res.json();
    const templates = json.exercise_templates as HevyExerciseTemplate[];
    if (!templates || templates.length === 0) break;
    for (const t of templates) {
      map.set(t.id, t);
    }
    if (templates.length < pageSize) break;
    page++;
  }

  return map;
}

// =============================================================================
// Hevy API: paginated workout fetch with date filtering
// =============================================================================

async function fetchWorkoutsInRange(
  apiKey: string,
  startDate: string,
  endDate: string,
): Promise<HevyWorkoutRaw[]> {
  const startTs = new Date(`${startDate}T00:00:00Z`).getTime();
  const endTs = new Date(`${endDate}T23:59:59Z`).getTime();
  const results: HevyWorkoutRaw[] = [];
  let page = 1;
  const pageSize = 10;

  console.log(`[fetch-hevy] range: ${startDate} (${startTs}) .. ${endDate} (${endTs})`);

  while (true) {
    const url = `/workouts?page=${page}&pageSize=${pageSize}`;
    const res = await hevyFetch(url, apiKey);

    // Hevy returns 404 for pages beyond page_count — treat as end
    if (res.status === 404) break;

    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Hevy API GET /workouts: ${res.status} ${text}`);
    }

    const json = await res.json();
    const pageCount = (json.page_count as number) ?? 1;
    const workouts = (json.workouts ?? []) as HevyWorkoutRaw[];

    if (!workouts || workouts.length === 0) break;

    for (const w of workouts) {
      const workoutTs = new Date(w.start_time).getTime();
      if (workoutTs >= startTs && workoutTs <= endTs) {
        results.push(w);
      }
    }

    // Stop if we've reached the last page
    if (page >= pageCount) break;
    page++;
  }

  return results;
}

// =============================================================================
// Transform raw Hevy data to stable output contract
// =============================================================================

function mapWorkout(
  raw: HevyWorkoutRaw,
  templates: Map<string, HevyExerciseTemplate>,
): WorkoutOut {
  const startMs = new Date(raw.start_time).getTime();
  const endMs = new Date(raw.end_time).getTime();
  const durationMinutes = Math.round((endMs - startMs) / 60_000);

  let totalVolume = 0;
  const exercises: WorkoutExerciseOut[] = raw.exercises.map((ex) => {
    const template = templates.get(ex.exercise_template_id);
    const sets = ex.sets.map((s) => {
      const volume = (s.weight_kg ?? 0) * (s.reps ?? 0);
      totalVolume += volume;
      return {
        type: s.type,
        weight_kg: s.weight_kg,
        reps: s.reps,
        duration_seconds: s.duration_seconds,
        rpe: s.rpe,
      };
    });

    return {
      name: template?.title ?? `Exercise ${ex.exercise_template_id}`,
      muscle_group: template?.primary_muscle_group ?? "other",
      superset_id: ex.superset_id,
      sets,
    };
  });

  return {
    id: raw.id,
    title: raw.title,
    date: raw.start_time.substring(0, 10),
    duration_minutes: durationMinutes,
    total_volume_kg: Math.round(totalVolume * 100) / 100,
    exercises,
  };
}

// =============================================================================
// Entry point
// =============================================================================

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  if (req.method !== "POST") {
    return new Response(
      JSON.stringify({ error: "Method not allowed" }),
      { status: 405, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }

  try {
    console.log("[fetch-hevy] function invoked");
    await requireUser(req);
    console.log("[fetch-hevy] user authenticated");

    const authHeader = req.headers.get("Authorization") ?? "";
    const jwt = authHeader.replace(/^Bearer\s+/i, "").trim();

    const body = (await req.json()) as FetchRequest;
    console.log(`[fetch-hevy] body: ${JSON.stringify(body)}`);
    if (!body.start_date || !body.end_date) {
      return new Response(
        JSON.stringify({ error: "start_date and end_date are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    let apiKey: string;
    try {
      apiKey = await getHevyApiKey(jwt);
      console.log(`[fetch-hevy] got API key (length=${apiKey.length})`);
    } catch (keyErr: unknown) {
      const keyMsg = keyErr instanceof Error ? keyErr.message : "unknown";
      console.log(`[fetch-hevy] getHevyApiKey failed: ${keyMsg} — returning empty`);
      return new Response(
        JSON.stringify({ workouts: [] }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
      );
    }

    const [rawWorkouts, templates] = await Promise.all([
      fetchWorkoutsInRange(apiKey, body.start_date, body.end_date),
      fetchExerciseTemplatesMap(apiKey),
    ]);

    const workouts = rawWorkouts.map((w) => mapWorkout(w, templates));
    console.log(`[fetch-hevy] returning ${workouts.length} workouts`);

    return new Response(
      JSON.stringify({ workouts }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : "Unknown error";
    console.error("fetch-hevy-workouts error:", message);

    const status = message.includes("expired") || message.includes("sign in") ? 401 : 500;
    return new Response(
      JSON.stringify({ error: message }),
      { status, headers: { ...corsHeaders, "Content-Type": "application/json" } },
    );
  }
});
