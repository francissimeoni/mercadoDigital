export type StorefrontPedidoDirectoRequest = {
  revendedorId?: string
  proveedorId?: string
  nombreApellido: string
  email: string
  telefonoWhatsapp: string
  provincia: string
  localidad: string
  codigoPostal: string
  calle: string
  numero: string
  pisoDepto?: string
  observaciones?: string
  productoCanonicoId?: string
  productoId?: string
  revendedorProductoId?: string
  cantidad: number
  seguroTransporte: boolean
  codigoDescuento?: string
  asesorVentaId?: string
  aceptacionTerminos: boolean
  metodoPago: "PSP" | "TRANSFERENCIA"
}

export type StorefrontPedidoDirectoResponse = {
  pedidoId: string
  estadoPedido: string
  estadoPago: string
  metodoPago: string
  totalProducto: number
  totalEnvio: number
  totalSeguro: number
  totalDescuento: number
  totalFinal: number
  demoraEstimadaDias: number
  trackingToken: string
  transferenciaToken?: string | null
  codigoDescuentoAplicado?: string | null
}

export type StorefrontPrepararPagoPspResponse = {
  pedidoId: string
  estadoPedido: string
  estadoPago: string
  metodoPago: string
  monto: number
  referenciaPedido: string
  referenciaPago: string
  checkoutUrl?: string | null
}

export type StorefrontCheckoutFormState = {
  nombreApellido: string
  email: string
  telefonoWhatsapp: string
  provincia: string
  localidad: string
  codigoPostal: string
  calle: string
  numero: string
  pisoDepto: string
  observaciones: string
  cantidad: number
  seguroTransporte: boolean
  codigoDescuento: string
  aceptacionTerminos: boolean
}

export type StorefrontPedidoTrackingItem = {
  pedidoItemId: string
  productoPublicacionId?: string | null
  nombreProducto: string
  cantidad: number
  imagenPrincipal: string | null
}

export type StorefrontPedidoTrackingEnvio = {
  estadoEnvio: string | null
  trackingLogistica: string | null
  transportistaNombre?: string | null
}

export type StorefrontPedidoTrackingResponse = {
  transferenciaToken?: string | null
  comprobanteCargado?: boolean
  pedidoId: string
  estadoPedido: string
  estadoPago: string
  metodoPago: string
  fechaCreacion: string
  demoraEstimadaDias: number
  direccionEnvio: string
  totalProducto: number
  totalEnvio: number
  totalSeguro: number
  totalDescuento: number
  totalFinal: number
  codigoDescuentoAplicado?: string | null
  items: StorefrontPedidoTrackingItem[]
  envio: StorefrontPedidoTrackingEnvio
}

export type StorefrontPublicRmaMessage = {
  id: string
  autorTipo: string
  autorNombre: string | null
  mensaje: string
  interno: boolean
  adjuntos: {
    id: string
    nombreArchivo: string
    contentType: string
    tamanoBytes: number
    dataUrl: string
    createdAt: string
  }[]
  createdAt: string
}

export type StorefrontReposicionBitacoraPaso = {
  id: string
  titulo: string
  fecha: string
  observacion: string | null
  tracking: string | null
  transportistaNombre: string | null
}

export type StorefrontPublicRmaResponse = {
  id: string
  pedidoId: string
  pedidoItemId: string
  productoNombre: string
  motivo: string
  estadoRma: string
  estadoCliente: string
  resolucionFinal: string | null
  montoCompensacion: number | null
  referenciaCompensacion: string | null
  trackingLogistica: string | null
  rotuloDevolucionDescargado: boolean
  rotuloReposicionGenerado: boolean
  permiteDescargarRotuloDevolucion: boolean
  fechaAprobacion: string | null
  fechaEnvioSolicitado: string | null
  fechaRotuloDevolucionDescargado: string | null
  fechaRecibidoDeposito: string | null
  fechaCierre: string | null
  fechaRotuloReposicionGenerado: string | null
  fechaReposicionSolicitadaProveedor: string | null
  fechaReposicionRecibidaMd: string | null
  fechaReposicionReenviadaCliente: string | null
  fechaReposicionEntregadaCliente: string | null
  estadoReposicion: string
  resumenReposicion: string
  reposicionTracking: string | null
  reposicionTransportistaNombre: string | null
  instruccionesSiguientePaso: string
  reposicionBitacora: StorefrontReposicionBitacoraPaso[]
  createdAt: string
  updatedAt: string
  mensajes: StorefrontPublicRmaMessage[]
}

export type StorefrontPublicSolicitarRmaRequest = {
  pedidoItemId: string
  motivo: string
  mensajeInicial: string
  archivos?: File[]
}

export type StorefrontPublicAgregarRmaMensajeRequest = {
  mensaje: string
  archivos?: File[]
}
