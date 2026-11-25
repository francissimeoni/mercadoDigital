import axiosClient from "./axiosClient"
import type {
  AdminCategoriaProductoRequest,
  AdminCategoriaProductoResponse,
  AdminCanonizacionAsignadoResponse,
  AdminCanonizacionCandidatoResponse,
  AdminCanonizacionCrearCanonicoRequest,
  AdminCanonizacionPendienteResponse,
  AdminCanonizacionResultadoResponse,
  AdminCorreoMdConfigRequest,
  AdminCorreoMdConfigResponse,
  AdminCorreoMdTestRequest,
  AdminCostoMdConfigRequest,
  AdminCostoMdConfigResponse,
  AdminPspConfigRequest,
  AdminPspConfigResponse,
  AdminRevendedorMercadoPagoOAuthLinkResponse,
  AdminRevendedorMercadoPagoRequest,
  AdminRevendedorMercadoPagoResponse,
  AdminRevendedorRequest,
  AdminRevendedorResponse,
  AdminStorefrontAssetResponse,
  AdminStorefrontResponse,
  AdminUsuarioUpdateRequest,
  AdminProveedorDespachoResponse,
  AdminProveedorDespachoRequest,
  AdminUsuarioCreateRequest,
  AdminUsuarioResponse,
  AdminStorefrontRequest,
} from "../types/admin"

export async function getAdminRevendedores(): Promise<AdminRevendedorResponse[]> {
  const response = await axiosClient.get<AdminRevendedorResponse[]>("/admin/revendedores")
  return response.data
}

export async function getAdminCostoMdConfig(): Promise<AdminCostoMdConfigResponse> {
  const response = await axiosClient.get<AdminCostoMdConfigResponse>("/admin/configuracion/costo-md")
  return response.data
}

export async function updateAdminCostoMdConfig(payload: AdminCostoMdConfigRequest): Promise<AdminCostoMdConfigResponse> {
  const response = await axiosClient.put<AdminCostoMdConfigResponse>("/admin/configuracion/costo-md", payload)
  return response.data
}

export async function getAdminPspConfig(): Promise<AdminPspConfigResponse> {
  const response = await axiosClient.get<AdminPspConfigResponse>("/admin/configuracion/psp")
  return response.data
}

export async function updateAdminPspConfig(payload: AdminPspConfigRequest): Promise<AdminPspConfigResponse> {
  const response = await axiosClient.put<AdminPspConfigResponse>("/admin/configuracion/psp", payload)
  return response.data
}

export async function getAdminCorreoMdConfig(): Promise<AdminCorreoMdConfigResponse> {
  const response = await axiosClient.get<AdminCorreoMdConfigResponse>("/admin/configuracion/correo-md")
  return response.data
}

export async function updateAdminCorreoMdConfig(payload: AdminCorreoMdConfigRequest): Promise<AdminCorreoMdConfigResponse> {
  const response = await axiosClient.put<AdminCorreoMdConfigResponse>("/admin/configuracion/correo-md", payload)
  return response.data
}

export async function testAdminCorreoMdConfig(payload: AdminCorreoMdTestRequest): Promise<void> {
  await axiosClient.post("/admin/configuracion/correo-md/test", payload)
}

export async function getAdminMercadoPagoMdConfig(): Promise<AdminRevendedorMercadoPagoResponse> {
  const response = await axiosClient.get<AdminRevendedorMercadoPagoResponse>("/admin/configuracion/mercadopago-md")
  return response.data
}

export async function updateAdminMercadoPagoMdConfig(
  payload: AdminRevendedorMercadoPagoRequest,
): Promise<AdminRevendedorMercadoPagoResponse> {
  const response = await axiosClient.put<AdminRevendedorMercadoPagoResponse>("/admin/configuracion/mercadopago-md", payload)
  return response.data
}

export async function getAdminMercadoPagoMdOAuthLink(): Promise<AdminRevendedorMercadoPagoOAuthLinkResponse> {
  const response = await axiosClient.get<AdminRevendedorMercadoPagoOAuthLinkResponse>(
    "/admin/configuracion/mercadopago-md/oauth-link",
  )
  return response.data
}

export async function getAdminCategoriasProducto(): Promise<AdminCategoriaProductoResponse[]> {
  const response = await axiosClient.get<AdminCategoriaProductoResponse[]>("/admin/configuracion/categorias-producto")
  return response.data
}

export async function createAdminCategoriaProducto(
  payload: AdminCategoriaProductoRequest,
): Promise<AdminCategoriaProductoResponse> {
  const response = await axiosClient.post<AdminCategoriaProductoResponse>("/admin/configuracion/categorias-producto", payload)
  return response.data
}

export async function updateAdminCategoriaProducto(
  id: string,
  payload: AdminCategoriaProductoRequest,
): Promise<AdminCategoriaProductoResponse> {
  const response = await axiosClient.put<AdminCategoriaProductoResponse>(`/admin/configuracion/categorias-producto/${id}`, payload)
  return response.data
}

export async function createAdminRevendedor(payload: AdminRevendedorRequest): Promise<AdminRevendedorResponse> {
  const response = await axiosClient.post<AdminRevendedorResponse>("/admin/revendedores", payload)
  return response.data
}

export async function updateAdminRevendedor(id: string, payload: AdminRevendedorRequest): Promise<AdminRevendedorResponse> {
  const response = await axiosClient.put<AdminRevendedorResponse>(`/admin/revendedores/${id}`, payload)
  return response.data
}

export async function upsertAdminRevendedorMercadoPago(
  id: string,
  payload: AdminRevendedorMercadoPagoRequest,
): Promise<AdminRevendedorMercadoPagoResponse> {
  const response = await axiosClient.put<AdminRevendedorMercadoPagoResponse>(`/admin/revendedores/${id}/mercadopago`, payload)
  return response.data
}

export async function getAdminRevendedorMercadoPagoOAuthLink(
  id: string,
): Promise<AdminRevendedorMercadoPagoOAuthLinkResponse> {
  const response = await axiosClient.get<AdminRevendedorMercadoPagoOAuthLinkResponse>(`/admin/revendedores/${id}/mercadopago/oauth-link`)
  return response.data
}

export async function upsertAdminStorefront(id: string, payload: AdminStorefrontRequest): Promise<AdminRevendedorResponse> {
  const response = await axiosClient.put<AdminRevendedorResponse>(`/admin/revendedores/${id}/storefront`, payload)
  return response.data
}

export async function getAdminProveedorStorefront(proveedorId: string): Promise<AdminStorefrontResponse> {
  const response = await axiosClient.get<AdminStorefrontResponse>(`/admin/proveedores/${proveedorId}/storefront`)
  return response.data
}

export async function upsertAdminProveedorStorefront(
  proveedorId: string,
  payload: AdminStorefrontRequest,
): Promise<AdminStorefrontResponse> {
  const response = await axiosClient.put<AdminStorefrontResponse>(`/admin/proveedores/${proveedorId}/storefront`, payload)
  return response.data
}

export async function uploadAdminStorefrontAsset(
  archivo: File,
  tipo: "logo" | "hero" | "favicon",
): Promise<AdminStorefrontAssetResponse> {
  const formData = new FormData()
  formData.append("archivo", archivo)
  formData.append("tipo", tipo)

  const response = await axiosClient.post<AdminStorefrontAssetResponse>("/admin/revendedores/storefront-assets", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  })
  return response.data
}

export async function getAdminUsuarios(): Promise<AdminUsuarioResponse[]> {
  const response = await axiosClient.get<AdminUsuarioResponse[]>("/admin/usuarios")
  return response.data
}

export async function createAdminUsuario(payload: AdminUsuarioCreateRequest): Promise<AdminUsuarioResponse> {
  const response = await axiosClient.post<AdminUsuarioResponse>("/admin/usuarios", payload)
  return response.data
}

export async function updateAdminUsuario(id: string, payload: AdminUsuarioUpdateRequest): Promise<AdminUsuarioResponse> {
  const response = await axiosClient.put<AdminUsuarioResponse>(`/admin/usuarios/${id}`, payload)
  return response.data
}

export async function getAdminProveedorMercadoPagoOAuthLink(
  proveedorId: string,
): Promise<AdminRevendedorMercadoPagoOAuthLinkResponse> {
  const response = await axiosClient.get<AdminRevendedorMercadoPagoOAuthLinkResponse>(
    `/admin/proveedores/${proveedorId}/mercadopago/oauth-link`,
  )
  return response.data
}

export async function getAdminProveedorDespachoConfig(proveedorId: string): Promise<AdminProveedorDespachoResponse> {
  const response = await axiosClient.get<AdminProveedorDespachoResponse>(`/admin/proveedores/${proveedorId}/despacho-config`)
  return response.data
}

export async function updateAdminProveedorDespachoConfig(
  proveedorId: string,
  payload: AdminProveedorDespachoRequest,
): Promise<AdminProveedorDespachoResponse> {
  const response = await axiosClient.put<AdminProveedorDespachoResponse>(
    `/admin/proveedores/${proveedorId}/despacho-config`,
    payload,
  )
  return response.data
}

export async function getAdminCanonizacionPendientes(): Promise<AdminCanonizacionPendienteResponse[]> {
  const response = await axiosClient.get<AdminCanonizacionPendienteResponse[]>("/admin/canonizacion/pendientes")
  return response.data
}

export async function getAdminCanonizacionAsignados(params?: {
  q?: string
  limit?: number
}): Promise<AdminCanonizacionAsignadoResponse[]> {
  const response = await axiosClient.get<AdminCanonizacionAsignadoResponse[]>("/admin/canonizacion/asignados", {
    params,
  })
  return response.data
}

export async function getAdminCanonizacionCandidatos(params?: {
  q?: string
  limit?: number
}): Promise<AdminCanonizacionCandidatoResponse[]> {
  const response = await axiosClient.get<AdminCanonizacionCandidatoResponse[]>("/admin/canonizacion/candidatos", {
    params,
  })
  return response.data
}

export async function associateAdminCanonizacionProducto(
  productoId: string,
  productoCanonicoId: string,
): Promise<AdminCanonizacionResultadoResponse> {
  const response = await axiosClient.put<AdminCanonizacionResultadoResponse>(
    `/admin/canonizacion/productos/${productoId}/asociar/${productoCanonicoId}`,
  )
  return response.data
}

export async function createAdminCanonizacionCanonico(
  productoId: string,
  payload: AdminCanonizacionCrearCanonicoRequest,
): Promise<AdminCanonizacionResultadoResponse> {
  const response = await axiosClient.post<AdminCanonizacionResultadoResponse>(
    `/admin/canonizacion/productos/${productoId}/crear-canonico`,
    payload,
  )
  return response.data
}

export async function desassociateAdminCanonizacionProducto(
  productoId: string,
): Promise<AdminCanonizacionResultadoResponse> {
  const response = await axiosClient.delete<AdminCanonizacionResultadoResponse>(`/admin/canonizacion/productos/${productoId}/canonico`)
  return response.data
}
