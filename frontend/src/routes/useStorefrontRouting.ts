import { useAuth } from "../auth/useAuth"
import { LOCAL_STOREFRONT_OVERRIDE_KEY } from "../tenant/TenantContext"
import { shouldRenderPublicStorefront } from "../tenant/storefrontHost"
import { useTenant } from "../tenant/useTenant"

function useStorefrontPreviewMode() {
  const searchParams = typeof window !== "undefined" ? new URLSearchParams(window.location.search) : null
  const hasPreviewQuery = Boolean(searchParams?.get("storefrontHost"))
  const hasPreviewSession =
    typeof window !== "undefined" && Boolean(window.sessionStorage.getItem(LOCAL_STOREFRONT_OVERRIDE_KEY))

  return hasPreviewQuery || hasPreviewSession
}

export function useIsStorefrontPublicExperience() {
  const tenant = useTenant()
  const isStorefrontPreview = useStorefrontPreviewMode()

  return shouldRenderPublicStorefront({
    host: tenant.host,
    source: tenant.source ?? "browser",
    isStorefrontPreview,
  })
}

export function useShouldUseOperationalLayout() {
  const { isAuthenticated, session } = useAuth()
  const isStorefrontPublic = useIsStorefrontPublicExperience()

  if (!isAuthenticated || !session || isStorefrontPublic) {
    return false
  }

  const roles = session.roles ?? []
  const isClienteOnly =
    roles.includes("CLIENTE") &&
    !roles.some((role) => ["REVENDEDOR", "PROVEEDOR", "LOGISTICA_MD", "ADMIN_MD"].includes(role))

  return !isClienteOnly
}
