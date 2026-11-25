import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  actualizarProveedorProducto,
  crearProveedorProducto,
  crearProveedorProductoMedia,
  desactivarProveedorProducto,
  eliminarProveedorProductoMedia,
  getProveedorCategoriasProducto,
  getProveedorProductoMedia,
  getProveedorProductos,
  importarProveedorProductos,
  sugerirProveedorCategoriaProducto,
} from "../api/proveedorApi"
import type {
  ProveedorCategoriaProductoSugerenciaRequest,
  ProveedorCategoriaProductoSugerenciaResponse,
  ProveedorProductoMediaRequest,
  ProveedorProductoImportacionResponse,
  ProveedorProductoUpsertRequest,
} from "../types/proveedor"

export function useProveedorProductos(proveedorId: string | null) {
  return useQuery({
    queryKey: ["proveedor-productos", proveedorId],
    queryFn: () => getProveedorProductos(proveedorId as string),
    enabled: Boolean(proveedorId),
  })
}

export function useProveedorCategoriasProducto(proveedorId: string | null) {
  return useQuery({
    queryKey: ["proveedor-categorias-producto", proveedorId],
    queryFn: () => getProveedorCategoriasProducto(proveedorId as string),
    enabled: Boolean(proveedorId),
  })
}

export function useSugerirProveedorCategoriaProducto(proveedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ProveedorCategoriaProductoSugerenciaRequest): Promise<ProveedorCategoriaProductoSugerenciaResponse> =>
      sugerirProveedorCategoriaProducto(proveedorId as string, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-categorias-producto", proveedorId] })
    },
  })
}

export function useCrearProveedorProducto(proveedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ProveedorProductoUpsertRequest) => crearProveedorProducto(proveedorId as string, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
    },
  })
}

export function useActualizarProveedorProducto(proveedorId: string | null, productoId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ProveedorProductoUpsertRequest) =>
      actualizarProveedorProducto(proveedorId as string, productoId as string, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
      if (productoId) {
        await queryClient.invalidateQueries({ queryKey: ["proveedor-producto-media", proveedorId, productoId] })
      }
    },
  })
}

export function useDesactivarProveedorProducto(proveedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (productoId: string) => desactivarProveedorProducto(proveedorId as string, productoId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
    },
  })
}

export function useProveedorProductoMedia(proveedorId: string | null, productoId: string | null) {
  return useQuery({
    queryKey: ["proveedor-producto-media", proveedorId, productoId],
    queryFn: () => getProveedorProductoMedia(proveedorId as string, productoId as string),
    enabled: Boolean(proveedorId && productoId),
  })
}

export function useCrearProveedorProductoMedia(proveedorId: string | null, productoId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: ProveedorProductoMediaRequest) =>
      crearProveedorProductoMedia(proveedorId as string, productoId as string, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
      await queryClient.invalidateQueries({ queryKey: ["proveedor-producto-media", proveedorId, productoId] })
    },
  })
}

export function useEliminarProveedorProductoMedia(proveedorId: string | null, productoId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (mediaId: string) => eliminarProveedorProductoMedia(proveedorId as string, productoId as string, mediaId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
      await queryClient.invalidateQueries({ queryKey: ["proveedor-producto-media", proveedorId, productoId] })
    },
  })
}

export function useImportarProveedorProductos(proveedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (archivo: File): Promise<ProveedorProductoImportacionResponse> =>
      importarProveedorProductos(proveedorId as string, archivo),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-productos", proveedorId] })
    },
  })
}
