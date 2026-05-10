const SUPABASE_URL = Deno.env.get("SUPABASE_URL");
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

if (!SUPABASE_URL) throw new Error("SUPABASE_URL not set");
if (!SERVICE_ROLE_KEY) throw new Error("SUPABASE_SERVICE_ROLE_KEY not set");

export const headers = {
  "apikey": SERVICE_ROLE_KEY,
  "Authorization": `Bearer ${SERVICE_ROLE_KEY}`,
  "Content-Type": "application/json",
  "Prefer": "return=minimal",
};

export async function rpc<T>(fn: string, params: Record<string, unknown>): Promise<T> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${fn}`, {
    method: "POST",
    headers,
    body: JSON.stringify(params),
  });
  if (!res.ok) throw new Error(`RPC ${fn} failed ${res.status}: ${await res.text()}`);
  return res.json() as Promise<T>;
}

export async function select<T>(
  table: string,
  query: string,
): Promise<T[]> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?${query}`, {
    headers: { ...headers, "Prefer": "return=representation" },
  });
  if (!res.ok) throw new Error(`SELECT ${table} failed ${res.status}: ${await res.text()}`);
  return res.json() as Promise<T[]>;
}

export async function upsertBatch(
  table: string,
  rows: Record<string, unknown>[],
  onConflict?: string,
  merge = true,
): Promise<void> {
  const resolution = merge ? "merge-duplicates" : "ignore-duplicates";
  const prefer = onConflict
    ? `resolution=${resolution},return=minimal`
    : "return=minimal";
  const res = await fetch(
    `${SUPABASE_URL}/rest/v1/${table}${onConflict ? `?on_conflict=${onConflict}` : ""}`,
    {
      method: "POST",
      headers: { ...headers, "Prefer": prefer },
      body: JSON.stringify(rows),
    },
  );
  if (!res.ok) throw new Error(`UPSERT ${table} failed ${res.status}: ${await res.text()}`);
}

export async function updateById(
  table: string,
  id: number | string,
  data: Record<string, unknown>,
): Promise<void> {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${table}?id=eq.${id}`, {
    method: "PATCH",
    headers,
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`PATCH ${table}/${id} failed ${res.status}: ${await res.text()}`);
}

export async function sql(query: string): Promise<unknown> {
  return rpc("pg_execute", { query });
}
