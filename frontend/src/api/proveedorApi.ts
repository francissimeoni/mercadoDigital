import axiosClient from "./axiosClient"
import type {
  EnvioResponse,
  PedidoProveedorResponse,
  ProveedorCategoriaProductoResponse,
  ProveedorCategoriaProductoSugerenciaRequest,
  ProveedorCategoriaProductoSugerenciaResponse,
  ProveedorProductoMediaRequest,
  ProveedorProductoMediaResponse,
  ProveedorProductoImportacionResponse,
  ProveedorProductoResponse,
  ProveedorProductoUpsertRequest,
} from "../types/proveedor"

export async function getPedidosProveedorPendientes(proveedorId: string): Promise<PedidoProveedorResponse[]> {
  const response = await axiosClient.get<PedidoProveedorResponse[]>(
    `/proveedores/${proveedorId}/pedidos-pendientes`,
  )
  return response.data
}

export async function marcarPedidoListoParaRetiro(pedidoId: string): Promise<EnvioResponse> {
  const response = await axiosClient.post<EnvioResponse>(`/envios/${pedidoId}/listo-para-retiro`)
  return response.data
}

export async function getProveedorProductos(proveedorId: string): Promise<ProveedorProductoResponse[]> {
  const response = await axiosClient.get<ProveedorProductoResponse[]>(`/proveedores/${proveedorId}/productos`)
  return response.data
}

export async function getProveedorCategoriasProducto(proveedorId: string): Promise<ProveedorCategoriaProductoResponse[]> {
  const response = await axiosClient.get<ProveedorCategoriaProductoResponse[]>(
    `/proveedores/${proveedorId}/categorias-producto`,
  )
  return response.data
}

export async function sugerirProveedorCategoriaProducto(
  proveedorId: string,
  payload: ProveedorCategoriaProductoSugerenciaRequest,
): Promise<ProveedorCategoriaProductoSugerenciaResponse> {
  const response = await axiosClient.post<ProveedorCategoriaProductoSugerenciaResponse>(
    `/proveedores/${proveedorId}/categorias-producto/sugerencias`,
    payload,
  )
  return response.data
}

export async function crearProveedorProducto(
  proveedorId: string,
  payload: ProveedorProductoUpsertRequest,
): Promise<ProveedorProductoResponse> {
  const response = await axiosClient.post<ProveedorProductoResponse>(`/proveedores/${proveedorId}/productos`, payload)
  return response.data
}

export async function actualizarProveedorProducto(
  proveedorId: string,
  productoId: string,
  payload: ProveedorProductoUpsertRequest,
): Promise<ProveedorProductoResponse> {
  const response = await axiosClient.put<ProveedorProductoResponse>(
    `/proveedores/${proveedorId}/productos/${productoId}`,
    payload,
  )
  return response.data
}

export async function desactivarProveedorProducto(
  proveedorId: string,
  productoId: string,
): Promise<ProveedorProductoResponse> {
  const response = await axiosClient.delete<ProveedorProductoResponse>(
    `/proveedores/${proveedorId}/productos/${productoId}`,
  )
  return response.data
}

export async function getProveedorProductoMedia(
  proveedorId: string,
  productoId: string,
): Promise<ProveedorProductoMediaResponse[]> {
  const response = await axiosClient.get<ProveedorProductoMediaResponse[]>(
    `/proveedores/${proveedorId}/productos/${productoId}/media`,
  )
  return response.data
}

export async function crearProveedorProductoMedia(
  proveedorId: string,
  productoId: string,
  payload: ProveedorProductoMediaRequest,
): Promise<ProveedorProductoMediaResponse> {
  const response = await axiosClient.post<ProveedorProductoMediaResponse>(
    `/proveedores/${proveedorId}/productos/${productoId}/media`,
    payload,
  )
  return response.data
}

export async function eliminarProveedorProductoMedia(
  proveedorId: string,
  productoId: string,
  mediaId: string,
): Promise<void> {
  await axiosClient.delete(`/proveedores/${proveedorId}/productos/${productoId}/media/${mediaId}`)
}

export async function importarProveedorProductos(
  proveedorId: string,
  archivo: File,
): Promise<ProveedorProductoImportacionResponse> {
  const formData = new FormData()
  formData.append("archivo", archivo)

  const response = await axiosClient.post<ProveedorProductoImportacionResponse>(
    `/proveedores/${proveedorId}/productos/importacion`,
    formData,
    {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    },
  )

  return response.data
}
