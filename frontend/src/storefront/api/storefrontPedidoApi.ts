import axiosClient from "../../api/axiosClient"
import type {
  StorefrontPedidoDirectoRequest,
  StorefrontPedidoDirectoResponse,
} from "../types/storefrontPedido"

export async function crearStorefrontPedidoDirecto(
  payload: StorefrontPedidoDirectoRequest,
): Promise<StorefrontPedidoDirectoResponse> {
  const response = await axiosClient.post<StorefrontPedidoDirectoResponse>("/public/pedido/directo", payload)
  return response.data
}

export type TransferReceiptResponse = { estado: string; mensaje: string; trackingToken: string; tiendaNombre: string; telefono: string | null; email: string | null }

export async function uploadTransferReceipt(token: string, file: File) {
  const form = new FormData()
  form.append("archivo", file)
  return (await axiosClient.post<TransferReceiptResponse>(`/public/pedido/transferencia/${token}/comprobante`, form)).data
}
