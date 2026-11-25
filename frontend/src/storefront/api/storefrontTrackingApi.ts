import axiosClient from "../../api/axiosClient"
import type { StorefrontPedidoTrackingResponse } from "../types/storefrontPedido"
import type { StorefrontPrepararPagoPspResponse } from "../types/storefrontPedido"

export async function getStorefrontPedidoTracking(
  trackingToken: string,
  storefrontHost: string,
): Promise<StorefrontPedidoTrackingResponse> {
  const response = await axiosClient.get<StorefrontPedidoTrackingResponse>(`/public/pedido/${trackingToken}`, {
    params: { storefrontHost },
  })
  return response.data
}

export async function confirmarStorefrontEntrega(trackingToken: string, storefrontHost: string) {
  return (await axiosClient.post(`/public/pedido/${trackingToken}/confirmar-entrega`, null, { params: { storefrontHost } })).data
}

export async function reanudarStorefrontPago(trackingToken: string, storefrontHost: string) {
  return (await axiosClient.post<StorefrontPrepararPagoPspResponse>(`/public/pedido/${encodeURIComponent(trackingToken)}/reanudar-pago`, null, { params: { storefrontHost } })).data
}
