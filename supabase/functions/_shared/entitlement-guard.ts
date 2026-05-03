import { hasProEntitlement } from "./entitlement.ts";
import { corsHeaders } from "./cors.ts";

/**
 * Returns a 403 Response if the user lacks Pro entitlement, or null if entitled.
 * Use: `const gate = await requirePro(userId); if (gate) return gate;`
 */
export async function requirePro(userId: string): Promise<Response | null> {
  const isPro = await hasProEntitlement(userId);
  if (!isPro) {
    return new Response(
      JSON.stringify({
        error: "pro_required",
        message: "This feature requires Strakk Pro.",
      }),
      {
        status: 403,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      },
    );
  }
  return null;
}
