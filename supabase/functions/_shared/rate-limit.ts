let kvInstance: Deno.Kv | null = null;

async function getKv(): Promise<Deno.Kv | null> {
  if (kvInstance) return kvInstance;
  try {
    kvInstance = await Deno.openKv();
    return kvInstance;
  } catch {
    return null;
  }
}

/**
 * Sliding-window rate limiter backed by Deno KV.
 *
 * Returns `true` if the request is allowed, `false` if over the limit.
 * Degrades open (allows) if KV is unavailable.
 */
export async function checkRateLimit(
  userId: string,
  functionName: string,
  maxRequests: number,
  windowSeconds: number,
): Promise<boolean> {
  const kv = await getKv();
  if (!kv) return true;

  const key = ["rate-limit", functionName, userId];
  const now = Date.now();
  const windowMs = windowSeconds * 1000;

  const entry = await kv.get<{ timestamps: number[] }>(key);
  const timestamps = (entry.value?.timestamps ?? []).filter(t => now - t < windowMs);
  if (timestamps.length >= maxRequests) return false;
  timestamps.push(now);

  const res = await kv.atomic()
    .check(entry)
    .set(key, { timestamps }, { expireIn: windowMs })
    .commit();

  if (!res.ok) return true; // conflict = concurrent write, degrade open
  return true;
}

/**
 * Checks Content-Length against a maximum. Returns the body size in bytes
 * if acceptable, or -1 if the request should be rejected.
 */
export function checkPayloadSize(req: Request, maxBytes: number): number {
  const cl = req.headers.get("content-length");
  if (cl) {
    const size = parseInt(cl, 10);
    if (Number.isFinite(size) && size > maxBytes) return -1;
    return size;
  }
  return 0;
}
