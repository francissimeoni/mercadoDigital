import { Navigate } from "react-router-dom"
import { getDefaultPathForRoles } from "../auth/accessControl"
import { useAuth } from "../auth/useAuth"

export default function PortalEntryRoute() {
  const { isAuthenticated, session } = useAuth()

  if (isAuthenticated && session) {
    return <Navigate to={getDefaultPathForRoles(session.roles)} replace />
  }

  return <Navigate to="/portal/login" replace />
}
