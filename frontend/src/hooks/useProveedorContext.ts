import { useSearchParams } from "react-router-dom"
import { useAuth } from "../auth/useAuth"

export function useProveedorContext() {
  const { session } = useAuth()
  const [searchParams] = useSearchParams()

  const proveedorId =
    searchParams.get("proveedorId") ||
    session?.proveedorId ||
    localStorage.getItem("proveedorId") ||
    import.meta.env.VITE_PROVEEDOR_ID ||
    null

  return {
    proveedorId,
    email: session?.email || null,
    roles: session?.roles ?? [],
  }
}
