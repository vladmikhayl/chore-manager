import { apiClient } from "./apiClient";
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
} from "../types/auth";

export async function register(request: RegisterRequest): Promise<void> {
  await apiClient.post("/auth/register", request);
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const response = await apiClient.post<LoginResponse>("/auth/login", request);
  return response.data;
}
