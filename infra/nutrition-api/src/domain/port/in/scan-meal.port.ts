import type { Prediction } from "../../model/prediction.ts";
import type { GroundedItem } from "../../model/grounded-item.ts";

export interface ScanRequest {
  images: string[];
  hint?: string;
  textOnly?: boolean;
}

export interface ScanResponse {
  predictions: Prediction[];
  items: GroundedItem[];
}

export interface ScanMealPort {
  scan(request: ScanRequest): Promise<ScanResponse>;
}
