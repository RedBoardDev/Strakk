// OpenAI embedding helper for the import pipeline.
// Mirrors the adapter logic but runs standalone (reads env directly).

const OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";
const EMBEDDING_MODEL = "text-embedding-3-small";
const EMBEDDING_DIMENSIONS = 1536;
const BATCH_SIZE = 100;
const MAX_RETRIES = 4;
const BASE_DELAY_MS = 1000;
const MAX_DELAY_MS = 15000;
const RETRYABLE_STATUSES = new Set([429, 500, 502, 503, 504]);

interface OpenAiEmbeddingObject {
  object: "embedding";
  embedding: number[];
  index: number;
}

interface OpenAiEmbeddingResponse {
  object: "list";
  data: OpenAiEmbeddingObject[];
  model: string;
  usage: { prompt_tokens: number; total_tokens: number };
}

let _apiKey: string | undefined;

function getApiKey(): string {
  if (!_apiKey) {
    _apiKey = Deno.env.get("OPENAI_API_KEY");
    if (!_apiKey) throw new Error("OPENAI_API_KEY not configured");
  }
  return _apiKey;
}

/**
 * Embeds an array of texts using OpenAI text-embedding-3-small.
 * Handles batching (max 100 per call) and exponential retry.
 */
export async function embedTexts(texts: string[]): Promise<number[][]> {
  if (texts.length === 0) return [];

  const results: number[][] = new Array(texts.length);

  for (let batchStart = 0; batchStart < texts.length; batchStart += BATCH_SIZE) {
    const batch = texts.slice(batchStart, batchStart + BATCH_SIZE);
    const embeddings = await embedBatch(batch);

    for (let i = 0; i < embeddings.length; i++) {
      results[batchStart + i] = embeddings[i];
    }
  }

  return results;
}

async function embedBatch(texts: string[]): Promise<number[][]> {
  const apiKey = getApiKey();
  const requestBody = {
    model: EMBEDDING_MODEL,
    input: texts,
    dimensions: EMBEDDING_DIMENSIONS,
  };

  for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
    if (attempt > 0) {
      const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), MAX_DELAY_MS);
      console.log(`  [embedding] Retry ${attempt}/${MAX_RETRIES}, waiting ${delay}ms...`);
      await new Promise((r) => setTimeout(r, delay));
    }

    const response = await fetch(OPENAI_EMBEDDINGS_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
      },
      body: JSON.stringify(requestBody),
    });

    if (RETRYABLE_STATUSES.has(response.status)) {
      console.warn(`  [embedding] Retryable HTTP ${response.status}`);
      continue;
    }

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`OpenAI Embeddings HTTP ${response.status}: ${text.slice(0, 500)}`);
    }

    const json: OpenAiEmbeddingResponse = await response.json();
    const sorted = [...json.data].sort((a, b) => a.index - b.index);
    return sorted.map((obj) => obj.embedding);
  }

  throw new Error("OpenAI Embeddings API unavailable after retries");
}
