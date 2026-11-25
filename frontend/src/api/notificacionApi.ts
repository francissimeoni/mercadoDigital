import axiosClient from "./axiosClient"
import type { NotificacionResponse } from "../types/notificacion"

export async function getNotificaciones(usuarioId: string): Promise<NotificacionResponse[]> {
  const response = await axiosClient.get<NotificacionResponse[]>(`/notificaciones/${usuarioId}`)
  return response.data
}

export async function marcarNotificacionComoLeida(id: string): Promise<NotificacionResponse> {
  const response = await axiosClient.post<NotificacionResponse>(`/notificaciones/${id}/leer`)
  return response.data
}
