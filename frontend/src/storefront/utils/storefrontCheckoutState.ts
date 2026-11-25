import type {
  StorefrontPedidoDirectoResponse,
  StorefrontPrepararPagoPspResponse,
} from "../types/storefrontPedido"

export const STOREFRONT_CHECKOUT_STORAGE_PREFIX = "storefront-checkout:"

export type PersistedCheckoutState = {
  pedidoCreado: StorefrontPedidoDirectoResponse | null
  pagoPreparado: StorefrontPrepararPagoPspResponse | null
  updatedAt: number
}

export function getCheckoutStorageKey(storefrontKey: string, productoId: string) {
  return `${STOREFRONT_CHECKOUT_STORAGE_PREFIX}${storefrontKey}:${productoId}`
}

export function getStorefrontCheckoutKey(tipo: string | null, revendedorId: string | null, proveedorId: string | null) {
  if (tipo === "REVENDEDOR" && revendedorId) return `REVENDEDOR:${revendedorId}`
  if (tipo === "PROVEEDOR" && proveedorId) return `PROVEEDOR:${proveedorId}`
  return null
}

export function readCheckoutState(storageKey: string): PersistedCheckoutState | null {
  try {
    const rawValue = window.sessionStorage.getItem(storageKey)
    if (!rawValue) return null
    const parsed = JSON.parse(rawValue) as Partial<PersistedCheckoutState>
    if (!parsed || typeof parsed !== "object") return null
    return {
      pedidoCreado: parsed.pedidoCreado || null,
      pagoPreparado: parsed.pagoPreparado || null,
      updatedAt: typeof parsed.updatedAt === "number" ? parsed.updatedAt : 0,
    }
  } catch {
    // Un estado ilegible o un navegador sin almacenamiento no debe borrar otras referencias.
    return null
  }
}

export function writeCheckoutState(storageKey: string, value: Omit<PersistedCheckoutState, "updatedAt">) {
  if (!value.pedidoCreado && !value.pagoPreparado) {
    window.sessionStorage.removeItem(storageKey)
    return
  }

  const persistedState: PersistedCheckoutState = {
    ...value,
    updatedAt: Date.now(),
  }

  window.sessionStorage.setItem(storageKey, JSON.stringify(persistedState))
}

export function clearCheckoutState(storageKey: string) {
  window.sessionStorage.removeItem(storageKey)
}

export function clearCheckoutStateByTrackingToken(storefrontKey: string | null | undefined, trackingToken: string) {
  if (!storefrontKey || !trackingToken) {
    return
  }

  const storefrontPrefix = `${STOREFRONT_CHECKOUT_STORAGE_PREFIX}${storefrontKey}:`

  for (let index = window.sessionStorage.length - 1; index >= 0; index -= 1) {
    const key = window.sessionStorage.key(index)

    if (!key || !key.startsWith(storefrontPrefix)) {
      continue
    }

    const parsed = readCheckoutState(key)
    if (parsed?.pedidoCreado?.trackingToken === trackingToken) {
      window.sessionStorage.removeItem(key)
    }
  }
}

export function findLatestCheckoutStateWithTracking(storefrontKey?: string | null) {
  if (!storefrontKey) return null
  let latestMatch: PersistedCheckoutState | null = null
  const storefrontPrefix = `${STOREFRONT_CHECKOUT_STORAGE_PREFIX}${storefrontKey}:`

  try {
  for (let index = 0; index < window.sessionStorage.length; index += 1) {
    const key = window.sessionStorage.key(index)

    if (!key || !key.startsWith(storefrontPrefix)) {
      continue
    }

    const parsed = readCheckoutState(key)
    if (typeof parsed?.pedidoCreado?.trackingToken !== "string" || !parsed.pedidoCreado.trackingToken.trim()) {
      continue
    }

    if (!latestMatch || parsed.updatedAt > latestMatch.updatedAt) {
      latestMatch = parsed
    }
  }
  } catch { return null }
  return latestMatch
}
