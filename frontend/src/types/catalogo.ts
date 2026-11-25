export type CatalogoItemResponse = {
  productoCanonicoId: string
  nombrePublico: string
  mejorCostoOperativoEstimado: number
  demoraEstimadaDias: number
  stockDisponible: number
  cantidadOfertas: number
  imagenPrincipal: string | null
}

export type ProductoDetalleResponse = {
  productoCanonicoId: string
  nombrePublico: string
  descripcion: string | null
  mejorCostoOperativoEstimado: number
  demoraEstimadaDias: number
  stockDisponible: number
  imagenPrincipal: string | null
}
