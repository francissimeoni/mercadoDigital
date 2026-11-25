import axiosClient from "./axiosClient"
import type {
  ActualizarReposicionManualRequest,
  AgregarRmaMensajeRequest,
  ResolverRmaRequest,
  RmaResponse,
  SolicitarRmaRequest,
} from "../types/rma"

export async function getRmas(params?: { pedidoId?: string }): Promise<RmaResponse[]> {
  const response = await axiosClient.get<RmaResponse[]>("/rma", { params })
  return response.data
}

export async function getRmaDetalle(rmaId: string): Promise<RmaResponse> {
  const response = await axiosClient.get<RmaResponse>(`/rma/${rmaId}`)
  return response.data
}

export async function solicitarRma(payload: SolicitarRmaRequest): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>("/rma/solicitar", payload)
  return response.data
}

export async function aprobarRma(rmaId: string): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/aprobar`)
  return response.data
}

export async function solicitarEnvioRma(rmaId: string): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/solicitar-envio`)
  return response.data
}

export async function recibirRmaEnDeposito(rmaId: string): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/recibir-en-deposito`)
  return response.data
}

export async function resolverRma(rmaId: string, payload: ResolverRmaRequest): Promise<RmaResponse> {
  const body = new FormData()
  body.append("resolucionFinal", payload.resolucionFinal)
  if (payload.observacionesInspeccion) {
    body.append("observacionesInspeccion", payload.observacionesInspeccion)
  }
  if (payload.responsableProbable) {
    body.append("responsableProbable", payload.responsableProbable)
  }
  if (payload.montoCompensacion != null) {
    body.append("montoCompensacion", String(payload.montoCompensacion))
  }
  if (payload.referenciaCompensacion) {
    body.append("referenciaCompensacion", payload.referenciaCompensacion)
  }
  payload.archivos?.forEach((archivo) => body.append("archivos", archivo))

  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/resolver`, body, {
    headers: { "Content-Type": "multipart/form-data" },
  })
  return response.data
}

export async function agregarMensajeRma(rmaId: string, payload: AgregarRmaMensajeRequest): Promise<RmaResponse> {
  const body = new FormData()
  body.append("mensaje", payload.mensaje)
  body.append("interno", String(Boolean(payload.interno)))
  payload.archivos?.forEach((archivo) => body.append("archivos", archivo))

  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/mensajes`, body, {
    headers: { "Content-Type": "multipart/form-data" },
  })
  return response.data
}

export async function descargarRotuloDevolucionRma(rmaId: string): Promise<Blob> {
  const response = await axiosClient.get(`/rma/${rmaId}/rotulo-devolucion`, { responseType: "blob" })
  return response.data as Blob
}

export async function descargarRotuloReposicionRma(rmaId: string): Promise<Blob> {
  const response = await axiosClient.get(`/rma/${rmaId}/rotulo-reposicion`, { responseType: "blob" })
  return response.data as Blob
}

export async function solicitarReposicionProveedorRma(rmaId: string, payload?: ActualizarReposicionManualRequest): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/reposicion/solicitar-proveedor`, payload ?? {})
  return response.data
}

export async function recibirReposicionMdRma(rmaId: string, payload?: ActualizarReposicionManualRequest): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/reposicion/recibir-md`, payload ?? {})
  return response.data
}

export async function reenviarReposicionClienteRma(rmaId: string, payload: ActualizarReposicionManualRequest): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/reposicion/reenviar-cliente`, payload)
  return response.data
}

export async function entregarReposicionClienteRma(rmaId: string, payload?: ActualizarReposicionManualRequest): Promise<RmaResponse> {
  const response = await axiosClient.post<RmaResponse>(`/rma/${rmaId}/reposicion/entregar-cliente`, payload ?? {})
  return response.data
}
