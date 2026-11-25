import axiosClient from "../../api/axiosClient"
import type { StorefrontPrepararPagoPspResponse } from "../types/storefrontPedido"

export async function prepararStorefrontPagoPsp(pedidoId: string, trackingToken: string, storefrontHost: string): Promise<StorefrontPrepararPagoPspResponse> {
  const response = await axiosClient.post<StorefrontPrepararPagoPspResponse>(`/public/pedido/${encodeURIComponent(pedidoId)}/pago`, null, {
    params: { trackingToken, storefrontHost },
  })
  return response.data
}
