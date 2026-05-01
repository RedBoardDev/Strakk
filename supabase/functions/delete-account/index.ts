import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";
import { corsHeaders } from "../_shared/cors.ts";
import { requireUser } from "../_shared/auth.ts";
import { checkRateLimit } from "../_shared/rate-limit.ts";

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

/**
 * Lists all file paths under a given Storage prefix and deletes them in batch.
 * Errors are logged but never thrown — user deletion must proceed regardless.
 */
async function purgeStoragePrefix(
  adminClient: ReturnType<typeof createClient>,
  bucket: string,
  prefix: string,
): Promise<void> {
  const { data: files, error: listError } = await adminClient.storage
    .from(bucket)
    .list(prefix, { limit: 1000 });

  if (listError) {
    console.error(`[delete-account] storage list error (${bucket}/${prefix}):`, listError.message);
    return;
  }

  if (!files || files.length === 0) {
    return;
  }

  const paths = files.map((f) => `${prefix}/${f.name}`);
  const { error: removeError } = await adminClient.storage.from(bucket).remove(paths);

  if (removeError) {
    console.error(`[delete-account] storage remove error (${bucket}/${prefix}):`, removeError.message);
  }
}

Deno.serve(async (req: Request) => {
  // 1. CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { headers: corsHeaders });
  }

  // 2. Method gating
  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405);
  }

  // 3. Auth — must happen before service-role operations
  let userId: string;
  try {
    ({ userId } = await requireUser(req));
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    return json({ error: msg }, 401);
  }

  // 4. Rate limiting — 1 request per 60 seconds per user
  if (!(await checkRateLimit(userId, "delete-account", 1, 60))) {
    return json({ error: "Rate limit exceeded. Please wait before retrying." }, 429);
  }

  // 5. Build service-role client for admin operations
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !serviceRoleKey) {
    console.error("[delete-account] missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY");
    return json({ error: "Internal server error" }, 500);
  }

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  try {
    // 6a. Purge meal photos — log errors, continue on failure
    await purgeStoragePrefix(adminClient, "meal-photos", userId);

    // 6b. Purge check-in photos — log errors, continue on failure
    await purgeStoragePrefix(adminClient, "checkin-photos", userId);

    // 6c. Delete the auth user — cascades to profiles (which triggers
    //     cleanup_profile_vault_secret to wipe the Hevy Vault secret),
    //     meals, meal_entries, water_entries, checkins, checkin_photos
    //     via ON DELETE CASCADE.
    const { error: deleteError } = await adminClient.auth.admin.deleteUser(userId);
    if (deleteError) {
      console.error("[delete-account] deleteUser error:", deleteError.message);
      return json({ error: "Failed to delete account. Please try again." }, 500);
    }

    return json({ success: true }, 200);
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    console.error("[delete-account] unexpected error:", msg);
    return json({ error: "Internal server error" }, 500);
  }
});
