export type StorefrontSession = {
  token: string
  refreshToken?: string
  email: string
  nombre: string
  issuedAt: string
  revendedorId: string | null
  clienteId: string | null
  asesorId: string | null
  roles: string[]
}

const SESSION_KEY = "md:storefront-session"
export const STOREFRONT_SESSION_CHANGED_EVENT = "md:storefront-session-changed"

export function getStorefrontSession(): StorefrontSession | null {
  if (typeof window === "undefined") return null
  try {
    const raw = window.localStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as StorefrontSession) : null
  } catch {
    window.localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function saveStorefrontSession(session: StorefrontSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new Event(STOREFRONT_SESSION_CHANGED_EVENT))
}

export function clearStorefrontSession() {
  window.localStorage.removeItem(SESSION_KEY)
  window.dispatchEvent(new Event(STOREFRONT_SESSION_CHANGED_EVENT))
}
