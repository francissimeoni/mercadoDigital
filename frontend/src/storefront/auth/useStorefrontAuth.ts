import { useContext } from "react"
import { StorefrontAuthContext } from "./storefrontAuthContextValue"

export function useStorefrontAuth() {
  const value = useContext(StorefrontAuthContext)
  if (!value) throw new Error("useStorefrontAuth debe usarse dentro de StorefrontAuthProvider")
  return value
}
