/**
 * Debug FDC API: check nutrient format for Foundation vs SR Legacy vs Branded.
 * Usage: deno run --allow-net --allow-env --env=.env debug-fdc.ts
 */

const USDA_API_KEY = Deno.env.get("USDA_API_KEY");
const FDC_BASE = "https://api.nal.usda.gov/fdc/v1";

async function fetchSample(dataType: string, count = 2) {
  const url = `${FDC_BASE}/foods/list?dataType=${encodeURIComponent(dataType)}&pageSize=${count}&pageNumber=1&api_key=${USDA_API_KEY}`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${res.status}: ${await res.text()}`);
  return res.json();
}

async function main() {
  for (const dt of ["SR Legacy", "Foundation", "Survey (FNDDS)", "Branded"]) {
    console.log(`\n========== ${dt} ==========`);
    const foods = await fetchSample(dt, 2);
    for (const f of foods) {
      console.log(`\n  ${f.description} (fdcId=${f.fdcId})`);
      console.log(`  dataType: ${f.dataType}`);
      console.log(`  foodNutrients (${f.foodNutrients?.length ?? 0} items):`);
      for (const n of (f.foodNutrients || []).slice(0, 8)) {
        console.log(`    nutrientId=${n.nutrientId} number="${n.number}" name="${n.name}" amount=${n.amount} value=${n.value} unitName=${n.unitName}`);
      }
      if (f.foodNutrients?.length > 8) {
        console.log(`    ... and ${f.foodNutrients.length - 8} more`);
      }
    }
  }

  // Check total branded count
  console.log("\n========== Branded count ==========");
  const branded = await fetchSample("Branded", 1);
  const countUrl = `${FDC_BASE}/foods/list?dataType=Branded&pageSize=1&pageNumber=1&api_key=${USDA_API_KEY}`;
  const countRes = await fetch(countUrl);
  const countData = await countRes.json();
  console.log(`First branded item: ${countData[0]?.description}`);
  // Check high page numbers to estimate total
  for (const page of [1000, 2000, 2500]) {
    const url = `${FDC_BASE}/foods/list?dataType=Branded&pageSize=200&pageNumber=${page}&api_key=${USDA_API_KEY}`;
    const res = await fetch(url);
    const data = await res.json();
    console.log(`  Branded page ${page}: ${Array.isArray(data) ? data.length : 0} items`);
  }
}

main().catch(console.error);
