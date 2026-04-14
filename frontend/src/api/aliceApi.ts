import type { ConfirmAliceLinkRequest } from "../types/alice";
import { apiClient } from "./apiClient";

export async function confirmAliceLink(
  request: ConfirmAliceLinkRequest,
): Promise<void> {
  await apiClient.post("/alice/oauth/authorize/confirm", request);
}
