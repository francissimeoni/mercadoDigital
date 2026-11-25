export type CrearPedidoCanonicoItemRequest = {
  productoCanonicoId: string
  cantidad: number
  tieneSeguro: boolean
}

export type CrearPedidoCanonicoRequest = {
  clienteId: string
  revendedorId: string
  items: CrearPedidoCanonicoItemRequest[]
}

export type PedidoResponse = {
  id: string
  revendedorId: string
  clienteId: string
  estadoPedido: string
  totalBruto: number
  totalPsp: number
  totalEnvio: number
  totalSeguro: number
  totalFinal: number
  items: PedidoItemResponse[]
  estadoPago: string
  estadoEnvio: string
  createdAt: string
  updatedAt: string
}

export type PedidoItemResponse = {
  id: string
  productoId: string
  productoNombre: string
  cantidad: number
  precioCosto: number
  markupAplicado: number
  precioItemFinal: number
  tieneSeguro: boolean
  montoSeguro: number
}

export type PedidoRevendedorResponse = {
  pedidoId: string
  estadoPedido: string
  estadoEnvio: string | null
  totalFinal: number
  tracking: string | null
  fechaCreacionPedido: string
}
