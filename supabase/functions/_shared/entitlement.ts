import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const PRO_STATUSES = ["trial", "active", "payment_failed"];

/**
 * Checks if a user has an active Pro entitlement.
 * Uses service_role to bypass RLS. Fails closed (returns false on error).
 */
export async function hasProEntitlement(userId: string): Promise<boolean> {
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    throw new Error("Supabase env vars not configured");
  }

  const supabase = createClient(supabaseUrl, serviceRoleKey);

  const { data, error } = await supabase
    .from("subscriptions")
    .select("status")
    .eq("user_id", userId)
    .maybeSingle();

  if (error) {
    console.error("[entitlement] DB error:", error.message);
    return false;
  }

  if (!data) return false;

  return PRO_STATUSES.includes(data.status);
}
