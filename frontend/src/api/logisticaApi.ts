import axiosClient from "./axiosClient"
import type {
  CrearTransportistaRequest,
  DespacharEnvioRequest,
  EnvioResponse,
  PedidoLogisticaResponse,
  TransportistaResponse,
} from "../types/logistica"

export async function getRetirosPendientes(): Promise<PedidoLogisticaResponse[]> {
  const response = await axiosClient.get<PedidoLogisticaResponse[]>("/logistica/retiros-pendientes")
  return response.data
}

export async function getDespachosPendientes(): Promise<PedidoLogisticaResponse[]> {
  const response = await axiosClient.get<PedidoLogisticaResponse[]>("/logistica/despachos-pendientes")
  return response.data
}

export async function getTransportistas(): Promise<TransportistaResponse[]> {
  const response = await axiosClient.get<TransportistaResponse[]>("/logistica/transportistas")
  return response.data
}

export async function crearTransportista(
  payload: CrearTransportistaRequest,
): Promise<TransportistaResponse> {
  const response = await axiosClient.post<TransportistaResponse>("/logistica/transportistas", payload)
  return response.data
}

export async function marcarRetirado(pedidoId: string): Promise<EnvioResponse> {
  const response = await axiosClient.post<EnvioResponse>(`/envios/${pedidoId}/retirado`)
  return response.data
}

export async function despacharEnvio(
  pedidoId: string,
  payload: DespacharEnvioRequest,
): Promise<EnvioResponse> {
  const response = await axiosClient.post<EnvioResponse>(`/envios/${pedidoId}/despachar`, payload)
  return response.data
}

export async function marcarEntregado(pedidoId: string): Promise<EnvioResponse> {
  const response = await axiosClient.post<EnvioResponse>(`/envios/${pedidoId}/entregar`)
  return response.data
}

export async function getEnvioByPedido(pedidoId: string): Promise<EnvioResponse> {
  const response = await axiosClient.get<EnvioResponse>(`/envios/${pedidoId}`)
  return response.data
}

export async function descargarRotulo(pedidoId: string): Promise<Blob> {
  const response = await axiosClient.get(`/envios/${pedidoId}/rotulo`, { responseType: "blob" })
  return response.data as Blob
}
