import { Navigate } from "react-router-dom"
import { useAuth } from "../auth/useAuth"
import { getDefaultPathForRoles } from "../auth/accessControl"

export default function AppFallbackRoute() {
  const { isAuthenticated, session } = useAuth()

  if (isAuthenticated && session) {
    return <Navigate to={getDefaultPathForRoles(session.roles)} replace />
  }

  return <Navigate to="/" replace />
}
