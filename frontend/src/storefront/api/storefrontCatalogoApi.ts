import axiosClient from "../../api/axiosClient"
import type { StorefrontCatalogoItem, StorefrontProductoDetalle } from "../types/storefrontCatalogo"

export type CatalogoTarget = (
  | { tipo: "REVENDEDOR"; revendedorId: string }
  | { tipo: "PROVEEDOR"; proveedorId: string }) & { host?: string | null }

export async function getStorefrontCatalogo(target: CatalogoTarget, signal?: AbortSignal): Promise<StorefrontCatalogoItem[]> {
  const response = await axiosClient.get<StorefrontCatalogoItem[]>(
    target.host ? "/public/catalogo/tienda" : target.tipo === "PROVEEDOR" ? "/public/catalogo/proveedor" : "/public/catalogo",
    {
      signal,
      params: target.host ? { host: target.host } : target.tipo === "PROVEEDOR" ? { proveedorId: target.proveedorId } : { revendedorId: target.revendedorId },
    },
  )

  return response.data
}

export async function getStorefrontProductoDetalle(
  productoId: string,
  target: CatalogoTarget,
  signal?: AbortSignal,
): Promise<StorefrontProductoDetalle> {
  const response = await axiosClient.get<StorefrontProductoDetalle>(
    target.host ? `/public/catalogo/tienda/productos/${productoId}` : target.tipo === "PROVEEDOR" ? `/public/catalogo/proveedor/${productoId}` : `/public/catalogo/${productoId}`,
    {
      signal,
      params: target.host ? { host: target.host } : target.tipo === "PROVEEDOR" ? { proveedorId: target.proveedorId } : { revendedorId: target.revendedorId },
    },
  )

  return response.data
}

export type CatalogoContacto = { id: string; nombre: string; responsable: boolean; whatsapp: string; fotoUrl?: string | null; provincia?: string | null; localidad?: string | null; zonaAtencion?: string | null }
export type CatalogoContactoProducto = {
  productoId: string; nombre: string; sku: string | null; precioPublicoArs: string
  condicionesEntrega: string | null; storefrontHost: string; contactos: CatalogoContacto[]
}

export async function getCatalogoContacto(host: string, productoId: string, signal?: AbortSignal): Promise<CatalogoContactoProducto> {
  const response = await axiosClient.get<CatalogoContactoProducto>(`/public/catalogo/tienda/productos/${productoId}/contacto`, { params: { host }, signal })
  return response.data
}
