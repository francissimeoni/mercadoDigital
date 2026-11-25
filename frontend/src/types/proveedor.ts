export type PedidoProveedorResponse = {
  pedidoId: string
  itemId: string
  productoId: string
  nombreProducto: string
  cantidad: number
  direccionEnvio: string | null
  estadoEnvio: string | null
  fechaCreacionPedido: string
}

export type EnvioResponse = {
  pedidoId: string
  estadoEnvio: string
  tracking: string | null
  transportistaNombre: string | null
  costoEnvio: number | null
}

export type ProveedorProductoResponse = {
  id: string
  proveedorId: string
  productoCanonicoId: string | null
  productoCanonicoNombre: string | null
  categoriaProductoId: string | null
  categoriaProductoNombre: string | null
  estadoNormalizacion: string
  sku: string
  nombre: string
  descripcion: string | null
  precioCosto: number
  moneda: string
  categoriaPaquete: string
  stockDisponible: number
  activo: boolean
  cantidadMedia: number
  imagenPrincipalUrl: string | null
  tieneVideo: boolean
}

export type ProveedorProductoUpsertRequest = {
  sku: string
  nombre: string
  descripcion: string
  precioCosto: number
  stock: number
  categoriaPaquete: string
  categoriaProductoId?: string | null
  imagenes?: string[]
  activo: boolean
}

export type ProveedorCategoriaProductoResponse = {
  id: string
  nombre: string
}

export type ProveedorCategoriaProductoSugerenciaRequest = {
  nombre: string
}

export type ProveedorCategoriaProductoSugerenciaResponse = {
  categoriaId: string
  nombre: string
  activa: boolean
  estado: "CREADA_PENDIENTE" | "YA_EXISTE_PENDIENTE" | "YA_EXISTE_ACTIVA"
  mensaje: string
}

export type ProveedorProductoMediaResponse = {
  id: string
  productoId: string
  tipoMedia: string
  url: string
  orden: number
  esPrincipal: boolean
}

export type ProveedorProductoMediaRequest = {
  tipoMedia: "IMAGEN" | "VIDEO"
  url: string
  orden?: number | null
  esPrincipal?: boolean
}

export type ProveedorProductoImportacionError = {
  fila: number | null
  sku: string | null
  mensaje: string
}

export type ProveedorProductoImportacionResponse = {
  filasLeidas: number
  creados: number
  actualizados: number
  rechazados: number
  errores: ProveedorProductoImportacionError[]
}
