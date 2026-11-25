import axiosClient from "../../api/axiosClient"

export type StorefrontEventType = "VISITA" | "VISTA_PRODUCTO" | "CLICK_WHATSAPP"

const VISITOR_KEY = "md:storefront:visitor"

function visitorId() {
  try {
    const current = window.localStorage.getItem(VISITOR_KEY)
    if (current) return current
    const created = typeof crypto.randomUUID === "function"
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
    window.localStorage.setItem(VISITOR_KEY, created)
    return created
  } catch {
    return `session-${Date.now()}-${Math.random().toString(36).slice(2)}`
  }
}

export function trackStorefrontEvent(tipo: StorefrontEventType, options?: { host?: string | null; productoId?: string }) {
  if (typeof window === "undefined") return
  const host = options?.host?.trim() || window.location.hostname
  if (!host) return

  void axiosClient.post("/public/storefront/metricas/eventos", {
    host,
    tipo,
    visitanteId: visitorId(),
    productoId: options?.productoId,
  }).catch(() => undefined)
}

export function trackStorefrontVisit(host?: string | null) {
  if (typeof window === "undefined") return
  const normalizedHost = host?.trim() || window.location.hostname
  const key = `md:storefront:visit:${normalizedHost}`
  try {
    if (window.sessionStorage.getItem(key)) return
    window.sessionStorage.setItem(key, "1")
  } catch {
    // La medicion sigue funcionando aunque el navegador bloquee sessionStorage.
  }
  trackStorefrontEvent("VISITA", { host: normalizedHost })
}

export function trackProductView(productoId: string, host?: string | null) {
  if (typeof window === "undefined" || !productoId) return
  const normalizedHost = host?.trim() || window.location.hostname
  const key = `md:storefront:product:${normalizedHost}:${productoId}`
  const now = Date.now()
  try {
    const previous = Number(window.sessionStorage.getItem(key) || 0)
    if (now - previous < 30_000) return
    window.sessionStorage.setItem(key, String(now))
  } catch {
    // Evita que un bloqueo de storage afecte la navegacion publica.
  }
  trackStorefrontEvent("VISTA_PRODUCTO", { host: normalizedHost, productoId })
}
