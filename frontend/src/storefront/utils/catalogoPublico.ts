import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"

/** Formato exacto: los importes decimales públicos no pasan por Number. */
export function formatPrecioArs(value: string) {
  if (!/^\d+\.\d{2}$/.test(value)) return "Precio no disponible"
  const [entero, centavos] = value.split(".")
  return `$ ${BigInt(entero).toLocaleString("es-AR")},${centavos}`
}

export function precioCatalogo(item: Pick<StorefrontCatalogoItem, "modoCatalogo" | "precioPublicoArs" | "mejorPrecioFinalEstimado">) {
  return item.modoCatalogo ? formatPrecioArs(item.precioPublicoArs ?? "")
    : new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(item.mejorPrecioFinalEstimado)
}

export function compararPrecios(a: StorefrontCatalogoItem, b: StorefrontCatalogoItem) {
  if (a.modoCatalogo && b.modoCatalogo && a.precioPublicoArs && b.precioPublicoArs) {
    const first = BigInt(a.precioPublicoArs.replace(".", ""))
    const second = BigInt(b.precioPublicoArs.replace(".", ""))
    return first < second ? -1 : first > second ? 1 : 0
  }
  return a.mejorPrecioFinalEstimado - b.mejorPrecioFinalEstimado
}
