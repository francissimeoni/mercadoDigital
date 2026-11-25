import axiosClient from "../../api/axiosClient"
import type {
  StorefrontPublicAgregarRmaMensajeRequest,
  StorefrontPublicRmaResponse,
  StorefrontPublicSolicitarRmaRequest,
} from "../types/storefrontPedido"

export async function getStorefrontRmas(trackingToken: string, storefrontHost: string): Promise<StorefrontPublicRmaResponse[]> {
  const response = await axiosClient.get<StorefrontPublicRmaResponse[]>(`/public/pedido/${trackingToken}/rma`, {
    params: { storefrontHost },
  })
  return response.data
}

export async function solicitarStorefrontRma(
  trackingToken: string,
  storefrontHost: string,
  payload: StorefrontPublicSolicitarRmaRequest,
): Promise<StorefrontPublicRmaResponse> {
  const body = new FormData()
  body.append("pedidoItemId", payload.pedidoItemId)
  body.append("motivo", payload.motivo)
  body.append("mensajeInicial", payload.mensajeInicial)
  payload.archivos?.forEach((archivo) => body.append("archivos", archivo))

  const response = await axiosClient.post<StorefrontPublicRmaResponse>(`/public/pedido/${trackingToken}/rma`, body, {
    params: { storefrontHost },
    headers: { "Content-Type": "multipart/form-data" },
  })
  return response.data
}

export async function agregarMensajeStorefrontRma(
  trackingToken: string,
  storefrontHost: string,
  rmaId: string,
  payload: StorefrontPublicAgregarRmaMensajeRequest,
): Promise<StorefrontPublicRmaResponse> {
  const body = new FormData()
  body.append("mensaje", payload.mensaje)
  payload.archivos?.forEach((archivo) => body.append("archivos", archivo))

  const response = await axiosClient.post<StorefrontPublicRmaResponse>(
    `/public/pedido/${trackingToken}/rma/${rmaId}/mensajes`,
    body,
    {
      params: { storefrontHost },
      headers: { "Content-Type": "multipart/form-data" },
    },
  )
  return response.data
}

export async function descargarRotuloDevolucionStorefrontRma(
  trackingToken: string,
  storefrontHost: string,
  rmaId: string,
): Promise<Blob> {
  const response = await axiosClient.get(`/public/pedido/${trackingToken}/rma/${rmaId}/rotulo-devolucion`, {
    params: { storefrontHost },
    responseType: "blob",
  })
  return response.data as Blob
}
