import axiosClient from "./axiosClient"
import type { AuthResponse, LoginRequest } from "../types/auth"

export async function login(payload: LoginRequest): Promise<AuthResponse> {
  const response = await axiosClient.post<AuthResponse>("/auth/login", payload)
  return response.data
}
