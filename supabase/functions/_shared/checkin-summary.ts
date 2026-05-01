import { callClaude, HAIKU_MODEL } from "./claude.ts";

const SYSTEM_PROMPT = `Tu es un analyste nutritionnel factuel. Tu rédiges le résumé d'une période de bilan pour qu'un coach comprenne rapidement l'alimentation et l'état général de son athlète sans lire chaque repas.

RÈGLES :
- Réponds en français, 3 à 5 phrases.
- Ton neutre et factuel. Zéro encouragement, zéro félicitation.
- Décris les aliments fréquents (ce que la personne mange vraiment).
- Évalue la régularité des apports protéiques jour après jour (stable, variable, irrégulier).
- Compare les macros aux objectifs si fournis, sans jugement de valeur.
- Commente l'hydratation : fréquence et niveau moyen vs objectif.
- Si le poids est fourni, mentionne-le factuellement.
- Si des ressentis (tags ou texte) sont fournis, croise-les avec les données nutritionnelles quand c'est pertinent (ex: fatigue + protéines basses, récupération + calories suffisantes).
- Si nutrition_days < 3, signale que les données sont partielles.
- Ne mentionne JAMAIS que tu es une IA ou un modèle.
- Pas de bullet points, pas de markdown. Texte fluide continu.`;

export interface CheckinSummaryInput {
  avgProtein: number;
  avgCalories: number;
  avgFat: number;
  avgCarbs: number;
  avgWater: number;
  nutritionDays: number;
  topFoods: string[];
  proteinPerDay: number[];
  daysWithWater: number;
  weightKg: number | null;
  feelingTags: string[];
  mentalFeeling: string;
  physicalFeeling: string;
  goals: {
    proteinGoal: number | null;
    calorieGoal: number | null;
    waterGoal: number | null;
  } | null;
}

export async function generateCheckinSummary(input: CheckinSummaryInput): Promise<string> {
  const goals = input.goals;

  const proteinLine = goals?.proteinGoal
    ? `- Protéines : ${input.avgProtein.toFixed(0)}g/j (objectif : ${goals.proteinGoal}g)`
    : `- Protéines : ${input.avgProtein.toFixed(0)}g/j`;
  const calorieLine = goals?.calorieGoal
    ? `- Calories : ${input.avgCalories.toFixed(0)} kcal/j (objectif : ${goals.calorieGoal} kcal)`
    : `- Calories : ${input.avgCalories.toFixed(0)} kcal/j`;
  const waterLine = goals?.waterGoal
    ? `- Eau : ${input.avgWater} mL/j (objectif : ${goals.waterGoal} mL) — ${input.daysWithWater}/${input.nutritionDays} jours avec eau`
    : `- Eau : ${input.avgWater} mL/j — ${input.daysWithWater}/${input.nutritionDays} jours avec eau`;

  const foodsLine = input.topFoods.length > 0
    ? `Aliments les plus consommés : ${input.topFoods.join(", ")}.`
    : "Aucun aliment nommé disponible.";

  const proteinTrendLine = input.proteinPerDay.length > 0
    ? `Protéines jour par jour : ${input.proteinPerDay.join(", ")}g.`
    : "";

  const weightLine = input.weightKg != null
    ? `\nPoids déclaré : ${input.weightKg.toFixed(1)} kg.`
    : "";

  const feelingTagsLine = input.feelingTags.length > 0
    ? `\nRessentis (tags) : ${input.feelingTags.join(", ")}.`
    : "";

  const mentalLine = input.mentalFeeling.trim()
    ? `\nRessenti mental : "${input.mentalFeeling.trim()}"`
    : "";

  const physicalLine = input.physicalFeeling.trim()
    ? `\nRessenti physique : "${input.physicalFeeling.trim()}"`
    : "";

  const userMessage = `Période : ${input.nutritionDays} jours de données.

${foodsLine}
${proteinTrendLine}

Moyennes quotidiennes :
${proteinLine}
${calorieLine}
- Lipides : ${input.avgFat.toFixed(0)}g/j
- Glucides : ${input.avgCarbs.toFixed(0)}g/j
${waterLine}
${weightLine}${feelingTagsLine}${mentalLine}${physicalLine}

Rédige le résumé factuel.`;

  return await callClaude({
    model: HAIKU_MODEL,
    system: SYSTEM_PROMPT,
    messages: [{ role: "user", content: [{ type: "text", text: userMessage }] }],
    maxTokens: 512,
    temperature: 0.3,
  });
}
