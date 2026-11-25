export type PedidoLogisticaResponse = {
  pedidoId: string
  productoId: string
  nombreProducto: string
  proveedorId: string
  nombreProveedor: string
  cantidad: number
  estadoEnvio: string | null
  tracking: string | null
  fechaCreacionPedido: string
}

export type EnvioResponse = {
  pedidoId: string
  estadoEnvio: string
  tracking: string | null
  transportistaNombre: string | null
  costoEnvio: number | null
}

export type TransportistaResponse = {
  id: string
  nombre: string
}

export type CrearTransportistaRequest = {
  nombre: string
}

export type DespacharEnvioRequest = {
  transportistaId?: string
  transportistaNombre?: string
  tracking: string
}
