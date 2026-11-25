import axiosClient from "./axiosClient"
import type {
  RevendedorMarkupOverrideRequest,
  RevendedorPricingConfigRequest,
  RevendedorPricingConfigResponse,
  RevendedorPricingPreviewItemResponse,
} from "../types/pricing"

export async function getRevendedorPricingConfig(revendedorId: string): Promise<RevendedorPricingConfigResponse> {
  const response = await axiosClient.get<RevendedorPricingConfigResponse>(`/revendedores/${revendedorId}/pricing-v1/config`)
  return response.data
}

export async function updateRevendedorPricingConfig(
  revendedorId: string,
  request: RevendedorPricingConfigRequest,
): Promise<RevendedorPricingConfigResponse> {
  const response = await axiosClient.put<RevendedorPricingConfigResponse>(
    `/revendedores/${revendedorId}/pricing-v1/config`,
    request,
  )
  return response.data
}

export async function getRevendedorPricingPreview(revendedorId: string): Promise<RevendedorPricingPreviewItemResponse[]> {
  const response = await axiosClient.get<RevendedorPricingPreviewItemResponse[]>(`/revendedores/${revendedorId}/pricing-v1/preview`)
  return response.data
}

export async function upsertRevendedorMarkupOverride(
  revendedorId: string,
  productoId: string,
  request: RevendedorMarkupOverrideRequest,
): Promise<RevendedorPricingPreviewItemResponse> {
  const response = await axiosClient.put<RevendedorPricingPreviewItemResponse>(
    `/revendedores/${revendedorId}/pricing-v1/productos/${productoId}`,
    request,
  )
  return response.data
}

export async function deleteRevendedorMarkupOverride(revendedorId: string, productoId: string): Promise<void> {
  await axiosClient.delete(`/revendedores/${revendedorId}/pricing-v1/productos/${productoId}`)
}
