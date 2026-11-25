import type { AuthResponse, AuthSession } from "../types/auth"

const AUTH_SESSION_KEY = "authSession"
export const AUTH_SESSION_EVENT = "md:auth-session-event"

type JwtPayload = {
  sub?: string
  uid?: string
  roles?: string[]
  exp?: number
}

type AuthSessionEventReason = "saved" | "cleared" | "expired"

function normalizeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/")
  const padding = normalized.length % 4
  if (padding === 0) {
    return normalized
  }
  return normalized + "=".repeat(4 - padding)
}

function decodeJwtPayload(token: string): JwtPayload | null {
  const parts = token.split(".")
  if (parts.length !== 3) {
    return null
  }

  try {
    const payloadJson = atob(normalizeBase64Url(parts[1]))
    return JSON.parse(payloadJson) as JwtPayload
  } catch {
    return null
  }
}

function emitAuthSessionEvent(reason: AuthSessionEventReason) {
  window.dispatchEvent(new CustomEvent(AUTH_SESSION_EVENT, { detail: { reason } }))
}

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== "object" || value === null) {
    return false
  }

  const candidate = value as Partial<AuthSession>
  return (
    typeof candidate.token === "string" &&
    typeof candidate.email === "string" &&
    typeof candidate.issuedAt === "string" &&
    typeof candidate.userId === "string" &&
    Array.isArray(candidate.roles) &&
    candidate.roles.every((role) => typeof role === "string") &&
    (typeof candidate.exp === "number" || candidate.exp === null) &&
    (typeof candidate.revendedorId === "string" || candidate.revendedorId === null) &&
    (typeof candidate.proveedorId === "string" || candidate.proveedorId === null) &&
    (typeof candidate.clienteId === "string" || candidate.clienteId === null)
  )
}

export function buildAuthSession(response: AuthResponse): AuthSession | null {
  const payload = decodeJwtPayload(response.token)
  if (!payload || !payload.uid || !payload.sub) {
    return null
  }

  const email = response.email || payload.sub

  return {
    token: response.token,
    email,
    issuedAt: response.issuedAt,
    userId: payload.uid,
    roles: Array.isArray(payload.roles) ? payload.roles : [],
    exp: typeof payload.exp === "number" ? payload.exp : null,
    revendedorId: response.revendedorId,
    proveedorId: response.proveedorId,
    clienteId: response.clienteId,
  }
}

function setOrRemoveStorageValue(key: string, value: string | null) {
  if (value) {
    localStorage.setItem(key, value)
    return
  }
  localStorage.removeItem(key)
}

export function saveAuthSession(session: AuthSession) {
  localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
  localStorage.setItem("token", session.token)
  localStorage.setItem("usuarioId", session.userId)
  localStorage.setItem("email", session.email)
  localStorage.setItem("roles", JSON.stringify(session.roles))
  setOrRemoveStorageValue("revendedorId", session.revendedorId)
  setOrRemoveStorageValue("proveedorId", session.proveedorId)
  setOrRemoveStorageValue("clienteId", session.clienteId)
  emitAuthSessionEvent("saved")
}

export function clearAuthSession(reason: AuthSessionEventReason = "cleared") {
  localStorage.removeItem(AUTH_SESSION_KEY)
  localStorage.removeItem("token")
  localStorage.removeItem("usuarioId")
  localStorage.removeItem("email")
  localStorage.removeItem("roles")
  localStorage.removeItem("revendedorId")
  localStorage.removeItem("proveedorId")
  localStorage.removeItem("clienteId")
  emitAuthSessionEvent(reason)
}

export function getStoredAuthSession(): AuthSession | null {
  const raw = localStorage.getItem(AUTH_SESSION_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as unknown
    if (!isAuthSession(parsed)) {
      clearAuthSession()
      return null
    }
    return parsed
  } catch {
    clearAuthSession()
    return null
  }
}

export function isSessionExpired(session: AuthSession): boolean {
  if (!session.exp) {
    return false
  }
  const nowInSeconds = Math.floor(Date.now() / 1000)
  return nowInSeconds >= session.exp
}

export function getAuthToken(): string | null {
  const session = getStoredAuthSession()
  if (!session) {
    return null
  }
  if (isSessionExpired(session)) {
    clearAuthSession("expired")
    return null
  }
  return session.token
}

import { resolveStorefrontLoginPath } from "../tenant/storefrontHost"

export function redirectToLogin() {
  const path = resolveStorefrontLoginPath(window.location.hostname)
  if (window.location.pathname !== path) {
    window.location.replace(path)
  }
}
