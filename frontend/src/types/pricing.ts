export type RevendedorPricingConfigResponse = {
  modoCatalogo?: boolean
  revendedorId: string
  markupPorcentaje: number
  markupFijo: number
  comisionAsesorPorcentaje: number
  comisionAsesorFija: number
  codigoDescuento: string | null
  descuentoFijo: number
}

export type RevendedorPricingConfigRequest = {
  markupPorcentaje: number
  markupFijo: number
  comisionAsesorPorcentaje: number
  comisionAsesorFija: number
  codigoDescuento?: string | null
  descuentoFijo: number
}

export type RevendedorMarkupOverrideRequest = {
  markupPorcentaje: number
  markupFijo: number
  comisionAsesorPorcentaje: number
  comisionAsesorFija: number
  activo: boolean
}

export type RevendedorPricingPreviewItemResponse = {
  productoCanonicoId: string
  productoBaseId: string
  nombrePublico: string
  costoOperativo: number
  markupPorcentaje: number
  markupFijo: number
  comisionAsesorPorcentaje: number
  comisionAsesorFija: number
  markupBruto: number
  comisionAsesorMonto: number
  gananciaNetaRevendedor: number
  precioFinalEstimado: number
  demoraEstimadaDias: number
  stockDisponible: number
  overrideActivo: boolean
  origenConfiguracion: string
  imagenPrincipal: string | null
}
