import type { TenantBranding } from "../../tenant/tenantTypes"

export const PLATFORM_DOCUMENT_TITLE = "Mercado Digital"
export const STOREFRONT_DOCUMENT_TITLE_FALLBACK = "Tienda online"
export const DEFAULT_FAVICON_HREF = "/vite.svg"
export const DEFAULT_THEME_COLOR = "#0f172a"

function resolveApiBaseUrl(): string {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  const fallbackBaseUrl = "http://localhost:8080"
  const baseUrl = configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : fallbackBaseUrl

  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl
}

export function resolveStorefrontAssetUrl(url: string | null | undefined): string | null {
  const trimmed = url?.trim()
  if (!trimmed) {
    return null
  }

  if (/^(https?:|data:|blob:)/i.test(trimmed)) {
    return trimmed
  }

  const normalizedPath = trimmed.startsWith("/") ? trimmed : `/${trimmed}`
  return `${resolveApiBaseUrl()}${normalizedPath}`
}

export function resolveStorefrontDisplayName(branding: TenantBranding | null | undefined): string | null {
  const nombreComercial = branding?.nombreComercial?.trim()
  return nombreComercial || null
}

export function resolveStorefrontFaviconHref(branding: TenantBranding | null | undefined): string {
  return (
    resolveStorefrontAssetUrl(branding?.faviconUrl) ||
    resolveStorefrontAssetUrl(branding?.logoUrl) ||
    DEFAULT_FAVICON_HREF
  )
}

export function resolveStorefrontDocumentTitle(branding: TenantBranding | null | undefined): string {
  return resolveStorefrontDisplayName(branding) || STOREFRONT_DOCUMENT_TITLE_FALLBACK
}

export function ensureFaviconLink(): HTMLLinkElement | null {
  if (typeof document === "undefined") {
    return null
  }

  let favicon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (!favicon) {
    favicon = document.createElement("link")
    favicon.setAttribute("rel", "icon")
    document.head.appendChild(favicon)
  }

  return favicon
}

export function ensureThemeColorMeta(): HTMLMetaElement | null {
  if (typeof document === "undefined") {
    return null
  }

  let meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
  if (!meta) {
    meta = document.createElement("meta")
    meta.setAttribute("name", "theme-color")
    document.head.appendChild(meta)
  }

  return meta
}

export function applyPlatformDocumentBranding(): void {
  if (typeof document === "undefined") {
    return
  }

  document.title = PLATFORM_DOCUMENT_TITLE

  const faviconLink = ensureFaviconLink()
  if (faviconLink) {
    faviconLink.removeAttribute("type")
    faviconLink.setAttribute("href", DEFAULT_FAVICON_HREF)
  }

  const themeColorMeta = ensureThemeColorMeta()
  if (themeColorMeta) {
    themeColorMeta.setAttribute("content", DEFAULT_THEME_COLOR)
  }
}

export function applyStorefrontDocumentBranding(branding: TenantBranding | null | undefined): void {
  if (typeof document === "undefined") {
    return
  }

  const nextTitle = resolveStorefrontDocumentTitle(branding)
  const nextFaviconHref = resolveStorefrontFaviconHref(branding)
  const nextThemeColor = branding?.colorPrimario?.trim() || DEFAULT_THEME_COLOR

  document.title = nextTitle

  const faviconLink = ensureFaviconLink()
  if (faviconLink && faviconLink.getAttribute("href") !== nextFaviconHref) {
    // El favicon configurado puede ser PNG, JPG, WEBP o SVG. Un type estatico
    // hace que algunos navegadores descarten el archivo aunque la URL sea valida.
    faviconLink.removeAttribute("type")
    faviconLink.setAttribute("href", nextFaviconHref)
  }

  const themeColorMeta = ensureThemeColorMeta()
  if (themeColorMeta && themeColorMeta.getAttribute("content") !== nextThemeColor) {
    themeColorMeta.setAttribute("content", nextThemeColor)
  }
}
