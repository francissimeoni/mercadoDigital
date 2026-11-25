import { Navigate, Outlet, useLocation } from "react-router-dom"
import { canAccessPath, getDefaultPathForRoles } from "../auth/accessControl"
import { useAuth } from "../auth/useAuth"
import { resolveStorefrontLoginPath } from "../tenant/storefrontHost"

export default function ProtectedRoute() {
  const { isAuthenticated, session } = useAuth()
  const location = useLocation()
  const loginPath = resolveStorefrontLoginPath(window.location.hostname)

  if (!isAuthenticated || !session) {
    return <Navigate to={loginPath} replace />
  }

  if (!canAccessPath(location.pathname, session.roles)) {
    return <Navigate to={getDefaultPathForRoles(session.roles)} replace />
  }

  return <Outlet />
}
