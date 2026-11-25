
import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { login as loginRequest } from "../api/authApi"
import type { AuthSession, LoginRequest } from "../types/auth"
import { hasFrontendAccess } from "./accessControl"
import {
  AUTH_SESSION_EVENT,
  buildAuthSession,
  clearAuthSession,
  getStoredAuthSession,
  isSessionExpired,
  redirectToLogin,
  saveAuthSession,
} from "./authSession"

type AuthContextValue = {
  session: AuthSession | null
  isAuthenticated: boolean
  login: (payload: LoginRequest) => Promise<AuthSession>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => {
    const current = getStoredAuthSession()
    if (!current || isSessionExpired(current)) {
      clearAuthSession()
      return null
    }
    return current
  })

  const login = useCallback(async (payload: LoginRequest) => {
    const response = await loginRequest(payload)
    const nextSession = buildAuthSession(response)
    if (!nextSession) {
      throw new Error("No se pudo interpretar la sesion de autenticacion.")
    }
    if (!hasFrontendAccess(nextSession.roles)) {
      clearAuthSession()
      throw new Error("Tu usuario se autentico, pero no tiene un modulo disponible en este frontend.")
    }

    saveAuthSession(nextSession)
    setSession(nextSession)
    return nextSession
  }, [])

  const logout = useCallback(() => {
    clearAuthSession()
    setSession(null)
  }, [])

  useEffect(() => {
    const syncSession = () => {
      const current = getStoredAuthSession()
      if (!current) {
        setSession(null)
        return
      }
      if (isSessionExpired(current)) {
        clearAuthSession("expired")
        setSession(null)
        redirectToLogin()
        return
      }
      setSession(current)
    }

    window.addEventListener(AUTH_SESSION_EVENT, syncSession as EventListener)
    window.addEventListener("storage", syncSession)

    return () => {
      window.removeEventListener(AUTH_SESSION_EVENT, syncSession as EventListener)
      window.removeEventListener("storage", syncSession)
    }
  }, [])

  useEffect(() => {
    if (!session?.exp) {
      return
    }

    const remainingMs = session.exp * 1000 - Date.now()
    if (remainingMs <= 0) {
      clearAuthSession("expired")
      setSession(null)
      redirectToLogin()
      return
    }

    const timeoutId = window.setTimeout(() => {
      clearAuthSession("expired")
      setSession(null)
      redirectToLogin()
    }, remainingMs)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [session])

  const value = useMemo(
    () => ({
      session,
      isAuthenticated: Boolean(session),
      login,
      logout,
    }),
    [session, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
