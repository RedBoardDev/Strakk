/**
 * Check if Foundation Foods have standard macronutrient numbers (203, 204, 205, 208).
 * Usage: deno run --allow-net --allow-env --env=.env debug-foundation-nutrients.ts
 */

const USDA_API_KEY = Deno.env.get("USDA_API_KEY");
const FDC_BASE = "https://api.nal.usda.gov/fdc/v1";

const MACRO_NUMBERS = ["203", "204", "205", "208"];

async function main() {
  const url = `${FDC_BASE}/foods/list?dataType=Foundation&pageSize=10&pageNumber=1&api_key=${USDA_API_KEY}`;
  const res = await fetch(url);
  const foods = await res.json();

  for (const f of foods) {
    console.log(`\n${f.description} (${f.foodNutrients?.length} nutrients)`);
    const allNumbers = (f.foodNutrients || []).map((n: { number: string }) => n.number);

    for (const target of MACRO_NUMBERS) {
      const found = f.foodNutrients?.find((n: { number: string }) => n.number === target);
      if (found) {
        console.log(`  ✅ ${target} (${found.name}): ${found.amount} ${found.unitName}`);
      } else {
        console.log(`  ❌ ${target}: NOT FOUND`);
      }
    }

    // Show what energy-related numbers exist
    const energyNutrients = f.foodNutrients?.filter(
      (n: { name: string }) => n.name?.toLowerCase().includes("energy") || n.name?.toLowerCase().includes("protein") || n.name?.toLowerCase().includes("lipid") || n.name?.toLowerCase().includes("carbohydrate")
    );
    if (energyNutrients?.length) {
      console.log("  Energy/macro nutrients found:");
      for (const n of energyNutrients) {
        console.log(`    number="${n.number}" name="${n.name}" amount=${n.amount} ${n.unitName}`);
      }
    }
  }
}

main().catch(console.error);
