import axiosClient from "./axiosClient"
import type {
  CrearPedidoCanonicoRequest,
  PedidoResponse,
  PedidoRevendedorResponse,
} from "../types/pedido"

export async function crearPedidoCanonico(payload: CrearPedidoCanonicoRequest): Promise<PedidoResponse> {
  const response = await axiosClient.post<PedidoResponse>("/pedido/canonico", payload)
  return response.data
}

export async function getPedidosRevendedor(revendedorId: string): Promise<PedidoRevendedorResponse[]> {
  const response = await axiosClient.get<PedidoRevendedorResponse[]>(`/revendedores/${revendedorId}/pedidos`)
  return response.data
}

export async function getPedidoDetalle(pedidoId: string): Promise<PedidoResponse> {
  const response = await axiosClient.get<PedidoResponse>(`/pedido/${pedidoId}`)
  return response.data
}
