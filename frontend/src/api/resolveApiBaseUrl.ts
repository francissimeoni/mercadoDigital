import { isStorefrontBrowserHost } from "../tenant/storefrontHost"

export function resolveApiBaseUrl(): string {
  if (typeof window !== "undefined" && isStorefrontBrowserHost(window.location.hostname)) {
    return "/api"
  }

  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  const fallbackBaseUrl = "http://localhost:8080"
  const baseUrl = configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : fallbackBaseUrl

  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl
}
