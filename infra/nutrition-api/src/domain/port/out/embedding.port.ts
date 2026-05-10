export interface EmbeddingPort {
  embed(texts: string[]): Promise<number[][]>;
}
