import axios from "axios"
import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import {
  applyPlatformDocumentBranding,
  applyStorefrontDocumentBranding,
} from "../storefront/utils/storefrontBranding"
import { resolveStorefront } from "./tenantApi"
import { normalizeTenantHost } from "./tenantHost"
import navarroVentasLogo from "../assets/NavarroVentasJpeg.jpeg"
import type {
  TenantBranding,
  TenantContextValue,
  TenantError,
  TenantSource,
  TenantStatus,
  TenantStorefront,
} from "./tenantTypes"

const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]"])
const DEMO_NAVARRO_HOSTS = new Set(["demo.revendedor.local", "navarroventas.local", "www.navarroventas.com"])
export const LOCAL_STOREFRONT_OVERRIDE_KEY = "md:storefront-host-override"

type TenantResolution = {
  host: string | null
  source: TenantSource
  localFallbackMessage: string | null
}

const initialValue: TenantContextValue = {
  status: "idle",
  isLoading: false,
  error: null,
  storefront: null,
  revendedorId: null,
  proveedorId: null,
  tipoStorefront: null,
  branding: null,
  host: null,
  source: null,
  isAvailable: false,
  refresh: async () => {},
}

export const TenantContext = createContext<TenantContextValue>(initialValue)

function normalizeHost(host: string): string {
  return normalizeTenantHost(host)
}

function resolveTenantHost(): TenantResolution {
  if (typeof window === "undefined") {
    return {
      host: null,
      source: null,
      localFallbackMessage: "Tenant no disponible fuera del browser.",
    }
  }

  const browserHost = normalizeHost(window.location.hostname)
  if (!browserHost) {
    return {
      host: null,
      source: null,
      localFallbackMessage: "No se pudo determinar el host actual del browser.",
    }
  }

  if (!LOCAL_HOSTS.has(browserHost)) {
    return {
      host: browserHost,
      source: "browser",
      localFallbackMessage: null,
    }
  }

  const queryParams = new URLSearchParams(window.location.search)
  const queryOverride = normalizeHost(queryParams.get("storefrontHost") ?? "")
  if (queryOverride) {
    window.sessionStorage.setItem(LOCAL_STOREFRONT_OVERRIDE_KEY, queryOverride)
    return {
      host: queryOverride,
      source: "query-override",
      localFallbackMessage: null,
    }
  }

  const sessionOverride = normalizeHost(window.sessionStorage.getItem(LOCAL_STOREFRONT_OVERRIDE_KEY) ?? "")
  if (sessionOverride) {
    return {
      host: sessionOverride,
      source: "session-override",
      localFallbackMessage: null,
    }
  }

  const devOverride = normalizeHost(import.meta.env.VITE_STOREFRONT_HOST ?? "")
  if (devOverride) {
    return {
      host: devOverride,
      source: "dev-override",
      localFallbackMessage: null,
    }
  }

  return {
    host: null,
    source: null,
    localFallbackMessage:
      "Host local detectado. Define VITE_STOREFRONT_HOST para resolver un tenant en entorno local.",
  }
}

function getLocalBrandingOverride(host: string | null): TenantBranding | null {
  if (!host || !DEMO_NAVARRO_HOSTS.has(host)) {
    return null
  }

  return {
    nombreComercial: "Navarro Ventas",
    logoUrl: navarroVentasLogo,
    faviconUrl: navarroVentasLogo,
    colorPrimario: null,
    colorSecundario: null,
    heroTitulo: null,
    heroSubtitulo: null,
    heroImagenUrl: navarroVentasLogo,
    portadas: [],
    emailComercial: null,
    telefonoComercial: null,
    direccionComercial: null,
    footerTexto: null,
    quienesTitulo: null,
    quienesTexto: null,
    quienesGaleria: [],
    instagramUrl: null,
    facebookUrl: null,
    tiktokUrl: null,
    youtubeUrl: null,
    transferenciaHabilitada: false,
    transferenciaAlias: null,
    transferenciaTitular: null,
  }
}

function toBranding(storefront: TenantStorefront | null, host: string | null): TenantBranding | null {
  const localOverride = getLocalBrandingOverride(host)
  if (!storefront) {
    return localOverride
  }

  return {
    nombreComercial: storefront.nombreComercial || localOverride?.nombreComercial || "Tienda online",
    logoUrl: storefront.logoUrl || localOverride?.logoUrl || null,
    faviconUrl: storefront.faviconUrl || localOverride?.faviconUrl || null,
    colorPrimario: storefront.colorPrimario || localOverride?.colorPrimario || null,
    colorSecundario: storefront.colorSecundario || localOverride?.colorSecundario || null,
    heroTitulo: storefront.heroTitulo || localOverride?.heroTitulo || null,
    heroSubtitulo: storefront.heroSubtitulo || localOverride?.heroSubtitulo || null,
    heroImagenUrl: storefront.heroImagenUrl || localOverride?.heroImagenUrl || null,
    portadas: storefront.portadas?.length ? storefront.portadas : localOverride?.portadas ?? [],
    emailComercial: storefront.emailComercial,
    telefonoComercial: storefront.telefonoComercial,
    direccionComercial: storefront.direccionComercial,
    footerTexto: storefront.footerTexto || localOverride?.footerTexto || null,
    quienesTitulo: storefront.quienesTitulo,
    quienesTexto: storefront.quienesTexto,
    quienesGaleria: storefront.quienesGaleria ?? [],
    instagramUrl: storefront.instagramUrl,
    facebookUrl: storefront.facebookUrl,
    tiktokUrl: storefront.tiktokUrl,
    youtubeUrl: storefront.youtubeUrl,
    transferenciaHabilitada: storefront.transferenciaHabilitada ?? false,
    transferenciaAlias: storefront.transferenciaAlias,
    transferenciaTitular: storefront.transferenciaTitular,
  }
}

function isPublicStorePath(pathname: string): boolean {
  return (
    pathname === "/" ||
    pathname === "/catalogo" ||
    pathname === "/quienes-somos" ||
    pathname === "/login" ||
    pathname === "/ingresar" ||
    pathname === "/ingresar/google" ||
    pathname === "/mi-cuenta" ||
    pathname === "/mi-tienda" ||
    pathname.startsWith("/producto/") ||
    pathname === "/pago/retorno" ||
    pathname.startsWith("/pedido/")
    || pathname.startsWith("/transferencia/")
  )
}

export function TenantProvider({ children }: { children: ReactNode }) {
  const [{ host, source, localFallbackMessage }] = useState(resolveTenantHost)
  const [status, setStatus] = useState<TenantStatus>(
    localFallbackMessage ? "local_unresolved" : host ? "loading" : "idle",
  )
  const [error, setError] = useState<TenantError | null>(
    localFallbackMessage
      ? {
          kind: "local_unresolved",
          message: localFallbackMessage,
        }
      : null,
  )
  const [storefront, setStorefront] = useState<TenantStorefront | null>(null)

  const refresh = useCallback(async () => {
    if (!host) {
      return
    }

    setStatus("loading")
    setError(null)

    try {
      const resolved = await resolveStorefront(host)
      setStorefront(resolved)
      setStatus("resolved")
    } catch (caughtError) {
      setStorefront(null)

      if (axios.isAxiosError(caughtError)) {
        const statusCode = caughtError.response?.status
        const message =
          typeof caughtError.response?.data?.message === "string"
            ? caughtError.response.data.message
            : "No se pudo resolver la tienda actual."

        if (statusCode === 404) {
          setError({ kind: "not_available", message })
          setStatus("error")
          return
        }

        if (statusCode === 400) {
          setError({ kind: "invalid_host", message })
          setStatus("error")
          return
        }

        setError({
          kind: "network",
          message: "No se pudo contactar al backend para resolver la tienda actual.",
        })
        setStatus("error")
        return
      }

      setError({
        kind: "network",
        message: "Ocurrio un error inesperado al resolver la tienda actual.",
      })
      setStatus("error")
    }
  }, [host])

  useEffect(() => {
    if (!host || localFallbackMessage) {
      return
    }

    void refresh()
  }, [host, localFallbackMessage, refresh])

  const resolvedBranding = storefront ? toBranding(storefront, host) : null

  const value = useMemo<TenantContextValue>(
    () => ({
      status,
      isLoading: status === "loading" && !storefront,
      error,
      storefront,
      revendedorId: storefront?.revendedorId ?? null,
      proveedorId: storefront?.proveedorId ?? null,
      tipoStorefront: storefront?.tipoStorefront ?? null,
      branding: resolvedBranding,
      host,
      source,
      isAvailable: Boolean(storefront?.publicada),
      refresh,
    }),
    [status, error, storefront, resolvedBranding, host, source, refresh],
  )

  useEffect(() => {
    if (typeof document === "undefined") {
      return
    }

    const syncDocumentBranding = () => {
      const shouldApplyTenantBranding = isPublicStorePath(window.location.pathname)
      if (shouldApplyTenantBranding) {
        if (value.branding) {
          applyStorefrontDocumentBranding(value.branding)
        }
        return
      }

      applyPlatformDocumentBranding()
    }

    syncDocumentBranding()
    window.addEventListener("popstate", syncDocumentBranding)

    return () => {
      window.removeEventListener("popstate", syncDocumentBranding)
    }
  }, [value.branding])

  return <TenantContext.Provider value={value}>{children}</TenantContext.Provider>
}
