import type { EmbeddingPort } from "../../domain/port/out/embedding.port.ts";

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

interface OpenAiEmbeddingConfig {
  openaiApiKey: string;
}

export class OpenAiEmbeddingAdapter implements EmbeddingPort {
  private readonly apiKey: string;

  constructor(config: OpenAiEmbeddingConfig) {
    this.apiKey = config.openaiApiKey;
  }

  async embed(texts: string[]): Promise<number[][]> {
    if (texts.length === 0) return [];

    const results: number[][] = new Array(texts.length);

    for (let batchStart = 0; batchStart < texts.length; batchStart += BATCH_SIZE) {
      const batch = texts.slice(batchStart, batchStart + BATCH_SIZE);
      const embeddings = await this.embedBatch(batch);

      for (let i = 0; i < embeddings.length; i++) {
        results[batchStart + i] = embeddings[i];
      }
    }

    return results;
  }

  private async embedBatch(texts: string[]): Promise<number[][]> {
    const requestBody = {
      model: EMBEDDING_MODEL,
      input: texts,
      dimensions: EMBEDDING_DIMENSIONS,
    };

    for (let attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      if (attempt > 0) {
        const delay = Math.min(BASE_DELAY_MS * 2 ** (attempt - 1), MAX_DELAY_MS);
        await new Promise((r) => setTimeout(r, delay));
      }

      let response: Response;
      try {
        response = await fetch(OPENAI_EMBEDDINGS_URL, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${this.apiKey}`,
          },
          body: JSON.stringify(requestBody),
        });
      } catch (err) {
        if (attempt === MAX_RETRIES) {
          throw new Error(`OpenAI Embeddings network error: ${err instanceof Error ? err.message : String(err)}`);
        }
        continue;
      }

      if (RETRYABLE_STATUSES.has(response.status)) {
        console.warn(`[embedding] Retryable HTTP ${response.status}, attempt ${attempt + 1}`);
        if (attempt === MAX_RETRIES) {
          throw new Error(`OpenAI Embeddings API unavailable after ${MAX_RETRIES} retries (HTTP ${response.status})`);
        }
        continue;
      }

      if (!response.ok) {
        const text = await response.text();
        throw new Error(`OpenAI Embeddings HTTP ${response.status}: ${text.slice(0, 500)}`);
      }

      const json: OpenAiEmbeddingResponse = await response.json();

      // Sort by index to restore original order (OpenAI may reorder)
      const sorted = [...json.data].sort((a, b) => a.index - b.index);
      return sorted.map((obj) => obj.embedding);
    }

    throw new Error("OpenAI Embeddings API unavailable after retries");
  }
}
