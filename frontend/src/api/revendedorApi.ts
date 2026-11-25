import axiosClient from "./axiosClient"
import type { RevendedorAsesorRequest, RevendedorAsesorResponse, RevendedorStorefrontHostResponse } from "../types/revendedor"

export async function getRevendedorAsesores(revendedorId: string): Promise<RevendedorAsesorResponse[]> {
  const response = await axiosClient.get<RevendedorAsesorResponse[]>(`/revendedores/${revendedorId}/asesores`)
  return response.data
}

export async function createRevendedorAsesor(
  revendedorId: string,
  payload: RevendedorAsesorRequest,
): Promise<RevendedorAsesorResponse> {
  const response = await axiosClient.post<RevendedorAsesorResponse>(`/revendedores/${revendedorId}/asesores`, payload)
  return response.data
}

export async function updateRevendedorAsesor(
  revendedorId: string,
  asesorId: string,
  payload: RevendedorAsesorRequest,
): Promise<RevendedorAsesorResponse> {
  const response = await axiosClient.put<RevendedorAsesorResponse>(
    `/revendedores/${revendedorId}/asesores/${asesorId}`,
    payload,
  )
  return response.data
}

export async function getRevendedorStorefrontHost(revendedorId: string): Promise<RevendedorStorefrontHostResponse> {
  const response = await axiosClient.get<RevendedorStorefrontHostResponse>(`/revendedores/${revendedorId}/storefront-host`)
  return response.data
}
