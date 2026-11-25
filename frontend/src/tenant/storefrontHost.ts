import type { TenantSource } from "./tenantTypes"
import { normalizeTenantHost } from "./tenantHost"

const PORTAL_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]", "mercado-digital.com.ar", "www.mercado-digital.com.ar"])

export function isPortalBrowserHost(host: string | null | undefined): boolean {
  const normalized = normalizeTenantHost(host ?? "")
  if (!normalized) {
    return true
  }

  return PORTAL_HOSTS.has(normalized)
}

export function isStorefrontBrowserHost(host: string | null | undefined): boolean {
  const normalized = normalizeTenantHost(host ?? "")
  if (!normalized) {
    return false
  }

  return !PORTAL_HOSTS.has(normalized)
}

export function shouldRenderPublicStorefront(options: {
  host: string | null
  source: TenantSource
  isStorefrontPreview: boolean
}): boolean {
  if (options.isStorefrontPreview) {
    return true
  }

  if (!options.host) {
    return false
  }

  return options.source === "browser" && isStorefrontBrowserHost(options.host)
}

export function resolveStorefrontLoginPath(host: string | null | undefined): "/login" | "/portal/login" {
  return isStorefrontBrowserHost(host) ? "/login" : "/portal/login"
}
