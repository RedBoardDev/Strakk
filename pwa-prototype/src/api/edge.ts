import { supabase } from './supabase.ts'

// Typed wrapper around Edge Function calls. Every function is POST-only and
// returns `{ error: string, message?, feature_key?, used?, limit? }` on
// failure — we surface a single readable message to the UI.
export class EdgeError extends Error {
  constructor(
    message: string,
    readonly code: string | null,
    readonly status: number | null,
  ) {
    super(message)
    this.name = 'EdgeError'
  }
}

function readableError(body: unknown, status: number | null): EdgeError {
  const obj = (body ?? {}) as { error?: string; message?: string }
  const code = obj.error ?? null
  switch (code) {
    case 'pro_required':
      return new EdgeError('This feature requires Strakk Pro.', code, status)
    case 'rate_limited':
      return new EdgeError('Too many requests — give it a few seconds.', code, status)
    case 'quota_exceeded':
      return new EdgeError(obj.message ?? 'Monthly limit reached.', code, status)
    default:
      return new EdgeError(obj.message ?? obj.error ?? 'Something went wrong. Try again.', code, status)
  }
}

export async function invokeEdge<T>(name: string, body: Record<string, unknown>): Promise<T> {
  const { data, error } = await supabase.functions.invoke<T>(name, { body })
  if (error) {
    // supabase-js wraps non-2xx into FunctionsHttpError with the Response.
    const ctx = (error as { context?: Response }).context
    if (ctx && typeof ctx.json === 'function') {
      const status = ctx.status
      const payload = await ctx.json().catch(() => null)
      throw readableError(payload, status)
    }
    throw new EdgeError(error.message ?? 'Network error. Check your connection.', null, null)
  }
  return data as T
}

// ---- shared AI shapes (verbatim from the edge functions) -------------------

export type BreakdownItem = {
  name: string
  protein_g: number
  calories_kcal: number
  fat_g: number | null
  carbs_g: number | null
  quantity: string | null
}

export type AnalyzedEntry = BreakdownItem & {
  breakdown: BreakdownItem[] | null
}
