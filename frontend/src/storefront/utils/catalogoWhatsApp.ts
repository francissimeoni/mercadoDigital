import type { CatalogoContacto, CatalogoContactoProducto } from "../api/storefrontCatalogoApi"
import { formatPrecioArs } from "./catalogoPublico"

export function buildCatalogoWhatsAppUrl(producto: CatalogoContactoProducto, contacto: CatalogoContacto) {
  if (!/^\+[1-9]\d{7,14}$/.test(contacto.whatsapp)) throw new Error("El contacto no tiene un número válido.")
  if (!producto.contactos.some(c => c.id === contacto.id && c.whatsapp === contacto.whatsapp)) throw new Error("El contacto ya no está disponible.")
  const share = new URL(`/api/public/share/producto/${encodeURIComponent(producto.productoId)}`, window.location.origin)
  share.searchParams.set("storefrontHost", producto.storefrontHost)
  share.searchParams.set("v", `og2-${producto.precioPublicoArs.replace(".", "-")}`)
  // La campaña del enlace de origen no decide quién recibe la consulta.
  if (!contacto.responsable) {
    share.searchParams.set("asesorId", contacto.id)
    share.searchParams.set("asesor", contacto.nombre)
  }
  const message = `Hola! Quiero comprar ${producto.nombre}${producto.sku ? ` (SKU ${producto.sku})` : ""}, publicado a ${formatPrecioArs(producto.precioPublicoArs)} ARS. ¿Podés darme más información para concretar la compra, conocer los medios de pago y coordinar la entrega? ${share.toString()}`
  return `https://wa.me/${contacto.whatsapp.slice(1)}?text=${encodeURIComponent(message)}`
}

/** La misma pestaña permite abrir la app sin depender de ventanas emergentes. */
export function abrirCatalogoWhatsApp(url: string) {
  window.location.assign(url)
}
