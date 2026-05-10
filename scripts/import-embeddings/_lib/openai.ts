const API_KEY = Deno.env.get("OPENAI_API_KEY");
if (!API_KEY) throw new Error("OPENAI_API_KEY not set");

const EMBED_URL = "https://api.openai.com/v1/embeddings";
const MODEL = "text-embedding-3-small";
const DIMENSIONS = 1536;

export async function embedBatch(texts: string[]): Promise<number[][]> {
  for (let attempt = 0; attempt < 5; attempt++) {
    const res = await fetch(EMBED_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ model: MODEL, input: texts, dimensions: DIMENSIONS }),
    });

    if (res.status === 429 || res.status >= 500) {
      const delay = Math.pow(2, attempt) * 1000;
      console.warn(`OpenAI ${res.status} — retry in ${delay}ms`);
      await new Promise((r) => setTimeout(r, delay));
      continue;
    }

    if (!res.ok) {
      const body = await res.text();
      throw new Error(`OpenAI error ${res.status}: ${body}`);
    }

    const json = await res.json();
    return (json.data as { embedding: number[] }[]).map((d) => d.embedding);
  }
  throw new Error("OpenAI embeddings failed after 5 retries");
}

export async function embedBatched(
  texts: string[],
  batchSize = 100,
  onProgress?: (done: number, total: number) => void,
): Promise<number[][]> {
  const results: number[][] = [];
  for (let i = 0; i < texts.length; i += batchSize) {
    const batch = texts.slice(i, i + batchSize);
    const embeddings = await embedBatch(batch);
    results.push(...embeddings);
    onProgress?.(Math.min(i + batchSize, texts.length), texts.length);
    // Small pause to stay within rate limits
    if (i + batchSize < texts.length) await new Promise((r) => setTimeout(r, 200));
  }
  return results;
}
