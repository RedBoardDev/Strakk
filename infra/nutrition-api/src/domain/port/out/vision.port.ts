import type { Prediction } from "../../model/prediction.ts";

export interface VisionPort {
  identify(images: string[], hint?: string): Promise<Prediction[]>;
}
