import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import axiosClient from "../../api/axiosClient"
import {
  clearStorefrontSession,
  getStorefrontSession,
  saveStorefrontSession,
  STOREFRONT_SESSION_CHANGED_EVENT,
  type StorefrontSession,
} from "./storefrontSession"
import { StorefrontAuthContext, type StorefrontLoginInput, type StorefrontRegisterInput } from "./storefrontAuthContextValue"

export function StorefrontAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StorefrontSession | null>(getStorefrontSession)

  useEffect(() => {
    const syncSession = () => setSession(getStorefrontSession())
    window.addEventListener(STOREFRONT_SESSION_CHANGED_EVENT, syncSession)
    window.addEventListener("storage", syncSession)
    return () => {
      window.removeEventListener(STOREFRONT_SESSION_CHANGED_EVENT, syncSession)
      window.removeEventListener("storage", syncSession)
    }
  }, [])

  const accept = useCallback((next: StorefrontSession) => {
    saveStorefrontSession(next)
    setSession(next)
    return next
  }, [])

  const login = useCallback(async (input: StorefrontLoginInput) => {
    const response = await axiosClient.post<StorefrontSession>("/public/storefront/auth/login", input)
    return accept(response.data)
  }, [accept])

  const register = useCallback(async (input: StorefrontRegisterInput) => {
    const response = await axiosClient.post<StorefrontSession>("/public/storefront/auth/clientes/registro", input)
    return accept(response.data)
  }, [accept])

  const logout = useCallback(() => {
    clearStorefrontSession()
    setSession(null)
  }, [])

  const value = useMemo(() => ({ session, login, register, logout }), [session, login, register, logout])
  return <StorefrontAuthContext.Provider value={value}>{children}</StorefrontAuthContext.Provider>
}
