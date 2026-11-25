import axiosClient from "./axiosClient"
import type { CatalogoItemResponse, ProductoDetalleResponse } from "../types/catalogo"

export async function getCatalogo(revendedorId: string): Promise<CatalogoItemResponse[]> {
  const response = await axiosClient.get<CatalogoItemResponse[]>("/catalogo", {
    params: { revendedorId },
  })

  return response.data
}

export async function getProductoDetalle(productoId: string): Promise<ProductoDetalleResponse> {
  const revendedorId = localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const response = await axiosClient.get<ProductoDetalleResponse>(`/catalogo/${productoId}`, {
    params: revendedorId ? { revendedorId } : undefined,
  })
  return response.data
}
