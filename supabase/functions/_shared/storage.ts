// Server-side Supabase Storage access using the service-role key.
// Downloads a photo from the private `meal-photos` bucket and returns
// its base64-encoded JPEG payload, ready to be sent to Claude Vision.

import { createClient, SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const BUCKET = "meal-photos";

let cachedAdmin: SupabaseClient | null = null;

function getAdminClient(): SupabaseClient {
  if (cachedAdmin) return cachedAdmin;

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) {
    throw new Error("SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY missing");
  }
  cachedAdmin = createClient(url, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return cachedAdmin;
}

/**
 * Download a photo from the `meal-photos` bucket and return it as base64 (no data URI prefix).
 * The caller must verify that `path` belongs to the authenticated user BEFORE calling this.
 * Enforced convention: `path = {userId}/{draftId_or_mealId}/{entryId}.jpg`.
 */
export async function downloadPhotoAsBase64(path: string): Promise<string> {
  const admin = getAdminClient();
  const { data, error } = await admin.storage.from(BUCKET).download(path);
  if (error) {
    throw new Error(`Storage download failed for ${path}: ${error.message}`);
  }
  const bytes = new Uint8Array(await data.arrayBuffer());
  return encodeBase64(bytes);
}

/**
 * Delete a list of objects from the bucket. Best-effort — logs but does not throw
 * when a single object is missing.
 */
export async function deletePhotos(paths: string[]): Promise<void> {
  if (paths.length === 0) return;
  const admin = getAdminClient();
  const { error } = await admin.storage.from(BUCKET).remove(paths);
  if (error) {
    console.warn(`Storage delete failed: ${error.message}`);
  }
}

/**
 * Verify that a storage path starts with the given userId segment.
 * Used to prevent cross-user path injection when a client submits storage paths.
 */
export function assertOwnedPath(path: string, userId: string): void {
  const firstSegment = path.split("/")[0];
  if (firstSegment !== userId) {
    throw new Error(`Path "${path}" does not belong to user ${userId}`);
  }
}

// Inlined base64 encoder (Deno std is a moving target; keep this module self-contained).
function encodeBase64(bytes: Uint8Array): string {
  let binary = "";
  const chunkSize = 0x8000;
  for (let i = 0; i < bytes.length; i += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkSize));
  }
  return btoa(binary);
}
