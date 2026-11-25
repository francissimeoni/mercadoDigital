import { MERCADO_PAGO_DISPONIBLE } from "../config/mercadoPago"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  associateAdminCanonizacionProducto,
  createAdminCategoriaProducto,
  createAdminCanonizacionCanonico,
  createAdminUsuario,
  createAdminRevendedor,
  desassociateAdminCanonizacionProducto,
  getAdminCanonizacionAsignados,
  getAdminCanonizacionCandidatos,
  getAdminCanonizacionPendientes,
  getAdminCategoriasProducto,
  getAdminProveedorMercadoPagoOAuthLink,
  getAdminProveedorStorefront,
  getAdminProveedorDespachoConfig,
  getAdminUsuarios,
  getAdminCostoMdConfig,
  getAdminCorreoMdConfig,
  getAdminPspConfig,
  getAdminMercadoPagoMdConfig,
  getAdminMercadoPagoMdOAuthLink,
  testAdminCorreoMdConfig,
  getAdminRevendedorMercadoPagoOAuthLink,
  getAdminRevendedores,
  updateAdminCostoMdConfig,
  updateAdminCorreoMdConfig,
  updateAdminPspConfig,
  updateAdminMercadoPagoMdConfig,
  updateAdminCategoriaProducto,
  updateAdminProveedorDespachoConfig,
  updateAdminUsuario,
  updateAdminRevendedor,
  uploadAdminStorefrontAsset,
  upsertAdminRevendedorMercadoPago,
  upsertAdminStorefront,
  upsertAdminProveedorStorefront,
} from "../api/adminApi"
import type {
  AdminCanonizacionCrearCanonicoRequest,
  AdminCategoriaProductoRequest,
  AdminCostoMdConfigRequest,
  AdminCorreoMdConfigRequest,
  AdminCorreoMdTestRequest,
  AdminPspConfigRequest,
  AdminUsuarioCreateRequest,
  AdminUsuarioUpdateRequest,
  AdminRevendedorMercadoPagoRequest,
  AdminRevendedorRequest,
  AdminStorefrontRequest,
} from "../types/admin"

export function useAdminCostoMdConfig() {
  return useQuery({
    queryKey: ["admin-configuracion", "costo-md"],
    queryFn: getAdminCostoMdConfig,
  })
}

export function useUpdateAdminCostoMdConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminCostoMdConfigRequest) => updateAdminCostoMdConfig(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "costo-md"] })
      await queryClient.invalidateQueries({ queryKey: ["revendedor-pricing-preview"] })
    },
  })
}

export function useAdminPspConfig() {
  return useQuery({
    queryKey: ["admin-configuracion", "psp"],
    queryFn: getAdminPspConfig,
  })
}

export function useAdminCorreoMdConfig() {
  return useQuery({
    queryKey: ["admin-configuracion", "correo-md"],
    queryFn: getAdminCorreoMdConfig,
  })
}

export function useUpdateAdminCorreoMdConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminCorreoMdConfigRequest) => updateAdminCorreoMdConfig(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "correo-md"] })
    },
  })
}

export function useTestAdminCorreoMdConfig() {
  return useMutation({
    mutationFn: (payload: AdminCorreoMdTestRequest) => testAdminCorreoMdConfig(payload),
  })
}

export function useUpdateAdminPspConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminPspConfigRequest) => updateAdminPspConfig(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "psp"] })
      await queryClient.invalidateQueries({ queryKey: ["revendedor-pricing-preview"] })
    },
  })
}

export function useAdminMercadoPagoMdConfig() {
  return useQuery({
    queryKey: ["admin-configuracion", "mercadopago-md"],
    queryFn: getAdminMercadoPagoMdConfig,
    enabled: MERCADO_PAGO_DISPONIBLE,
  })
}

export function useUpdateAdminMercadoPagoMdConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminRevendedorMercadoPagoRequest) => updateAdminMercadoPagoMdConfig(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "mercadopago-md"] })
    },
  })
}

export function useAdminMercadoPagoMdOAuthLink() {
  return useMutation({
    mutationFn: () => getAdminMercadoPagoMdOAuthLink(),
  })
}

export function useAdminCategoriasProducto() {
  return useQuery({
    queryKey: ["admin-configuracion", "categorias-producto"],
    queryFn: getAdminCategoriasProducto,
  })
}

export function useCreateAdminCategoriaProducto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminCategoriaProductoRequest) => createAdminCategoriaProducto(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "categorias-producto"] })
    },
  })
}

export function useUpdateAdminCategoriaProducto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AdminCategoriaProductoRequest }) =>
      updateAdminCategoriaProducto(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-configuracion", "categorias-producto"] })
    },
  })
}

export function useAdminRevendedores() {
  return useQuery({
    queryKey: ["admin-revendedores"],
    queryFn: getAdminRevendedores,
  })
}

export function useCreateAdminRevendedor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminRevendedorRequest) => createAdminRevendedor(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-revendedores"] })
    },
  })
}

export function useUpdateAdminRevendedor() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AdminRevendedorRequest }) => updateAdminRevendedor(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-revendedores"] })
    },
  })
}

export function useUpsertAdminStorefront() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AdminStorefrontRequest }) => upsertAdminStorefront(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-revendedores"] })
    },
  })
}

export function useAdminProveedorStorefront(proveedorId?: string | null) {
  return useQuery({
    queryKey: ["admin-proveedor-storefront", proveedorId],
    queryFn: () => getAdminProveedorStorefront(proveedorId as string),
    enabled: Boolean(proveedorId),
  })
}

export function useUpsertAdminProveedorStorefront() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ proveedorId, payload }: { proveedorId: string; payload: AdminStorefrontRequest }) =>
      upsertAdminProveedorStorefront(proveedorId, payload),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ["admin-proveedor-storefront", variables.proveedorId] })
      await queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] })
    },
  })
}

export function useUpsertAdminRevendedorMercadoPago() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AdminRevendedorMercadoPagoRequest }) =>
      upsertAdminRevendedorMercadoPago(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-revendedores"] })
    },
  })
}

export function useAdminRevendedorMercadoPagoOAuthLink() {
  return useMutation({
    mutationFn: (id: string) => getAdminRevendedorMercadoPagoOAuthLink(id),
  })
}

export function useUploadAdminStorefrontAsset() {
  return useMutation({
    mutationFn: ({ archivo, tipo }: { archivo: File; tipo: "logo" | "hero" | "favicon" }) =>
      uploadAdminStorefrontAsset(archivo, tipo),
  })
}

export function useAdminUsuarios() {
  return useQuery({
    queryKey: ["admin-usuarios"],
    queryFn: getAdminUsuarios,
  })
}

export function useCreateAdminUsuario() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AdminUsuarioCreateRequest) => createAdminUsuario(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] })
    },
  })
}

export function useUpdateAdminUsuario() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AdminUsuarioUpdateRequest }) => updateAdminUsuario(id, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] })
    },
  })
}

export function useAdminProveedorMercadoPagoOAuthLink() {
  return useMutation({
    mutationFn: (proveedorId: string) => getAdminProveedorMercadoPagoOAuthLink(proveedorId),
  })
}

export function useAdminProveedorDespachoConfig(proveedorId?: string | null) {
  return useQuery({
    queryKey: ["admin-proveedor-despacho", proveedorId],
    queryFn: () => getAdminProveedorDespachoConfig(proveedorId as string),
    enabled: Boolean(proveedorId),
  })
}

export function useUpdateAdminProveedorDespachoConfig() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ proveedorId, despachaMercaderia }: { proveedorId: string; despachaMercaderia: boolean }) =>
      updateAdminProveedorDespachoConfig(proveedorId, { despachaMercaderia }),
    onSuccess: async (_, variables) => {
      await queryClient.invalidateQueries({ queryKey: ["admin-proveedor-despacho", variables.proveedorId] })
      await queryClient.invalidateQueries({ queryKey: ["admin-usuarios"] })
    },
  })
}

export function useAdminCanonizacionPendientes() {
  return useQuery({
    queryKey: ["admin-canonizacion", "pendientes"],
    queryFn: getAdminCanonizacionPendientes,
  })
}

export function useAdminCanonizacionAsignados(search?: string) {
  return useQuery({
    queryKey: ["admin-canonizacion", "asignados", search ?? ""],
    queryFn: () => getAdminCanonizacionAsignados({ q: search?.trim() || undefined, limit: 40 }),
  })
}

export function useAdminCanonizacionCandidatos(search?: string) {
  return useQuery({
    queryKey: ["admin-canonizacion", "candidatos", search ?? ""],
    queryFn: () => getAdminCanonizacionCandidatos({ q: search?.trim() || undefined, limit: 40 }),
  })
}

export function useAssociateAdminCanonizacionProducto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ productoId, productoCanonicoId }: { productoId: string; productoCanonicoId: string }) =>
      associateAdminCanonizacionProducto(productoId, productoCanonicoId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "pendientes"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "candidatos"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "asignados"] })
    },
  })
}

export function useCreateAdminCanonizacionCanonico() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ productoId, payload }: { productoId: string; payload: AdminCanonizacionCrearCanonicoRequest }) =>
      createAdminCanonizacionCanonico(productoId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "pendientes"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "candidatos"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "asignados"] })
    },
  })
}

export function useDesassociateAdminCanonizacionProducto() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (productoId: string) => desassociateAdminCanonizacionProducto(productoId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "pendientes"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "candidatos"] })
      await queryClient.invalidateQueries({ queryKey: ["admin-canonizacion", "asignados"] })
    },
  })
}
