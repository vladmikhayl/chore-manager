import type {
  ConfirmAliceLinkRequest,
  ConfirmAliceLinkResponse,
} from "../types/alice";
import { apiClient } from "./apiClient";

export async function confirmAliceLink(
  request: ConfirmAliceLinkRequest,
): Promise<string> {
  const response = await apiClient.post<ConfirmAliceLinkResponse>(
    "/alice/oauth/authorize/confirm",
    request,
  );

  return response.data.redirectUrl;
}
