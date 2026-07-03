// Shared input parsing for numeric fields (no <input type="number"> — it brings
// the iOS spinner and scroll-hijack; we sanitize free text instead).

// Integer field: keep digits only, clamp to a max. Empty → 0.
export function parseIntClamped(raw: string, max: number): number {
  const cleaned = raw.replace(/[^\d]/g, '')
  if (cleaned === '') return 0
  return Math.min(max, parseInt(cleaned, 10))
}

// Decimal field mid-typing: keep digits + one dot, clamp, preserve the raw
// string so partial input like "12." survives the controlled round-trip.
export function sanitizeDecimal(raw: string, max: number): string {
  const cleaned = raw.replace(/[^\d.]/g, '').replace(/(\..*)\./g, '$1')
  if (cleaned === '') return ''
  const num = parseFloat(cleaned)
  if (Number.isNaN(num)) return ''
  return num > max ? String(max) : cleaned
}
