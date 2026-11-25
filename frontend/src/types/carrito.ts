export type AddItemToCarritoPayload = {
  productoId: string
  cantidad: number
}

export type CarritoItemResponse = {
  productoId: string
  nombre: string
  precio: number
  cantidad: number
  subtotal: number
  imagenPrincipal?: string | null
}

export type CarritoResponse = {
  items: CarritoItemResponse[]
  total?: number
}
