import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

export const HEVY_BASE = "https://api.hevyapp.com/v1";

/**
 * Retrieves the user's decrypted Hevy API key via Vault RPC.
 * Throws if the key is not configured.
 */
export async function getHevyApiKey(userJwt: string): Promise<string> {
  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_ANON_KEY")!,
    { global: { headers: { Authorization: `Bearer ${userJwt}` } } },
  );
  const { data, error } = await supabase.rpc("get_hevy_api_key");
  if (error || !data) {
    throw new Error("Hevy API key not configured. Add it in Settings.");
  }
  return data as string;
}

/**
 * Authenticated fetch wrapper for the Hevy API.
 */
export async function hevyFetch(
  path: string,
  apiKey: string,
  options: RequestInit = {},
): Promise<Response> {
  return await fetch(`${HEVY_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "api-key": apiKey,
      ...(options.headers ?? {}),
    },
  });
}
