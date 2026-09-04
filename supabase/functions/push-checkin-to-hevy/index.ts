import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { requireFeatureAccess, recordFeatureUsage } from "../_shared/feature-guard.ts";
import { getHevyApiKey, hevyFetch } from "../_shared/hevy.ts";

interface PushRequest {
  checkin_id: string;
  overwrite: boolean;
}

interface CheckinRow {
  covered_dates: string[];
  weight_kg: number | null;
  shoulders_cm: number | null;
  chest_cm: number | null;
  arm_left_cm: number | null;
  arm_right_cm: number | null;
  waist_cm: number | null;
  hips_cm: number | null;
  thigh_left_cm: number | null;
  thigh_right_cm: number | null;
}

interface PushResponse {
  date: string;
  conflict: boolean;
}

// Maps Strakk's check-in measurement columns to Hevy's /v1/body_measurements fields.
// Strakk has no data for Hevy's lean_mass_kg, fat_percent, neck_cm, forearm, abdomen or calf
// fields — those are simply omitted from the payload (Hevy treats missing fields as null).
function mapMeasurements(row: CheckinRow): Record<string, number> {
  const fields: Record<string, number | null> = {
    weight_kg: row.weight_kg,
    shoulder_cm: row.shoulders_cm,
    chest_cm: row.chest_cm,
    left_bicep_cm: row.arm_left_cm,
    right_bicep_cm: row.arm_right_cm,
    waist: row.waist_cm,
    hips: row.hips_cm,
    left_thigh: row.thigh_left_cm,
    right_thigh: row.thigh_right_cm,
  };
  const out: Record<string, number> = {};
  for (const [key, value] of Object.entries(fields)) {
    if (value != null) out[key] = value;
  }
  return out;
}

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
    const { userId } = await requireUser(req);
    const userJwt = req.headers.get("Authorization")!.replace(/^Bearer\s+/i, "").trim();

    const gate = await requireFeatureAccess(userId, "hevy_export");
    if (gate) return gate;

    let body: PushRequest;
    try {
      body = await req.json();
    } catch {
      return json({ error: "Invalid JSON body" }, 400);
    }

    if (!body.checkin_id) {
      return json({ error: "Missing checkin_id" }, 400);
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const supabase = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: `Bearer ${userJwt}` } },
    });

    const { data: checkin, error: checkinError } = await supabase
      .from("checkins")
      .select(
        "covered_dates, weight_kg, shoulders_cm, chest_cm, arm_left_cm, arm_right_cm, waist_cm, hips_cm, thigh_left_cm, thigh_right_cm",
      )
      .eq("id", body.checkin_id)
      .single();

    if (checkinError || !checkin) {
      return json({ error: "Check-in not found" }, 404);
    }

    const row = checkin as CheckinRow;
    if (!row.covered_dates || row.covered_dates.length === 0) {
      return json({ error: "Check-in has no covered dates" }, 400);
    }
    const date = [...row.covered_dates].sort().at(-1)!;

    const measurements = mapMeasurements(row);
    if (Object.keys(measurements).length === 0) {
      return json({ error: "This check-in has no measurements to push" }, 400);
    }

    let apiKey: string;
    try {
      apiKey = await getHevyApiKey(userJwt);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      return json({ error: msg }, 400);
    }

    if (!body.overwrite) {
      const res = await hevyFetch("/body_measurements", apiKey, {
        method: "POST",
        body: JSON.stringify({ date, ...measurements }),
      });

      if (res.status === 409) {
        const response: PushResponse = { date, conflict: true };
        return json(response, 200);
      }
      if (!res.ok) {
        const text = await res.text();
        throw new Error(`Hevy API POST body_measurements: ${res.status} ${text}`);
      }

      await recordFeatureUsage(userId, "hevy_export");
      const response: PushResponse = { date, conflict: false };
      return json(response, 200);
    }

    const res = await hevyFetch(`/body_measurements/${date}`, apiKey, {
      method: "PUT",
      body: JSON.stringify(measurements),
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(`Hevy API PUT body_measurements/${date}: ${res.status} ${text}`);
    }

    await recordFeatureUsage(userId, "hevy_export");
    const response: PushResponse = { date, conflict: false };
    return json(response, 200);
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
      console.error("[push-checkin-to-hevy] Hevy API error:", message);
      return json({ error: "External service unavailable" }, 502);
    }

    console.error("[push-checkin-to-hevy] error:", message, error instanceof Error ? error.stack : "");
    return json({ error: "Internal server error" }, 500);
  }
});
