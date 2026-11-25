import { Navigate } from "react-router-dom"
import { useAuth } from "../auth/useAuth"
import { getDefaultPathForRoles } from "../auth/accessControl"

export default function RoleHomeRedirect() {
  const { session } = useAuth()
  return <Navigate to={getDefaultPathForRoles(session?.roles ?? [])} replace />
}
