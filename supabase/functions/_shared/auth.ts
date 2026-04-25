import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

/**
 * Validates the JWT from the Authorization header and returns the authenticated user id.
 * Throws an error response-ready message if auth fails.
 */
/**
 * Validates the JWT from the Authorization header and returns the authenticated user.
 *
 * IMPORTANT: we MUST pass the token explicitly to `getUser(token)`. Calling
 * `getUser()` without an argument makes the server-side client look up a
 * non-existent local session and fails with "Invalid or expired token".
 */
export async function requireUser(req: Request): Promise<{ userId: string; email: string | null }> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    throw new Error("Missing Authorization header");
  }

  const token = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!token) {
    throw new Error("Empty Bearer token");
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Supabase env vars not configured");
  }

  // We only need this client for the auth.getUser(token) call.
  const supabase = createClient(supabaseUrl, supabaseAnonKey);

  const { data, error } = await supabase.auth.getUser(token);
  if (error || !data.user) {
    console.error("[auth] getUser failed:", error?.message ?? "no user");
    throw new Error("Invalid or expired token");
  }

  return { userId: data.user.id, email: data.user.email ?? null };
}
