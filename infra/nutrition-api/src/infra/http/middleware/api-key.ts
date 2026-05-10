import type { Context, Next } from "@hono/hono";

/**
 * Middleware that validates the x-api-key header against the expected API key.
 * Returns 401 if missing or invalid.
 */
export function apiKeyMiddleware(expectedApiKey: string) {
  return async (c: Context, next: Next) => {
    const provided = c.req.header("x-api-key");

    if (!provided) {
      return c.json({ error: "Missing x-api-key header" }, 401);
    }

    if (provided !== expectedApiKey) {
      return c.json({ error: "Invalid API key" }, 401);
    }

    await next();
  };
}
