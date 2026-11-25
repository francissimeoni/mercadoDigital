export type StorefrontCatalogoItem = {
  modoCatalogo?: boolean
  precioPublicoArs?: string
  productoId: string
  productoCanonicoId: string | null
  nombrePublico: string
  categoriaProducto: string | null
  mejorPrecioFinalEstimado: number
  demoraEstimadaDias: number | null
  stockDisponible: number | null
  cantidadOfertas: number | null
  imagenPrincipal: string | null
  promocion: boolean
  novedad: boolean
}

export type StorefrontProductoDetalle = {
  modoCatalogo?: boolean
  precioPublicoArs?: string
  condicionesEntrega?: string | null
  sku?: string | null
  productoId: string
  productoCanonicoId: string | null
  nombrePublico: string
  descripcion: string | null
  mejorPrecioFinalEstimado: number
  seguroTransporteEstimado: number | null
  totalConSeguroEstimado: number | null
  demoraEstimadaDias: number | null
  stockDisponible: number | null
  imagenPrincipal: string | null
  imagenes: string[]
}
