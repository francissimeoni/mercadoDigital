export type RmaAdjuntoResponse = {
  id: string
  nombreArchivo: string
  contentType: string
  tamanoBytes: number
  dataUrl: string
  createdAt: string
}

export type RmaMensajeResponse = {
  id: string
  autorTipo: string
  autorNombre: string | null
  mensaje: string
  interno: boolean
  adjuntos: RmaAdjuntoResponse[]
  createdAt: string
}

export type ReposicionBitacoraPasoResponse = {
  id: string
  titulo: string
  fecha: string
  observacion: string | null
  tracking: string | null
  transportistaNombre: string | null
}

export type RmaResponse = {
  id: string
  pedidoItemId: string
  motivo: string
  descripcion: string | null
  estadoRma: string
  resolucionFinal: string | null
  observacionesInspeccion: string | null
  responsableProbable: string | null
  montoCompensacion: number | null
  referenciaCompensacion: string | null
  pedidoId: string | null
  trackingToken: string | null
  estadoPedido: string | null
  estadoEnvio: string | null
  trackingLogistica: string | null
  transportistaNombre: string | null
  clienteNombre: string | null
  clienteEmail: string | null
  revendedorNombre: string | null
  storefrontNombre: string | null
  storefrontDominio: string | null
  proveedorNombre: string | null
  proveedorEmail: string | null
  productoNombre: string | null
  cantidad: number | null
  precioItemFinal: number | null
  precioCosto: number | null
  tieneSeguro: boolean
  montoSeguro: number | null
  totalEnvioPedido: number | null
  direccionEnvio: string | null
  rotuloDevolucionDescargado: boolean
  rotuloReposicionGenerado: boolean
  permiteSolicitarEnvio: boolean
  permiteDescargarRotuloDevolucion: boolean
  permiteDescargarRotuloReposicion: boolean
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
  permiteSolicitarReposicionProveedor: boolean
  permiteRegistrarRecepcionReposicionMd: boolean
  permiteReenviarReposicionCliente: boolean
  permiteEntregarReposicionCliente: boolean
  instruccionesSiguientePaso: string | null
  reposicionBitacora: ReposicionBitacoraPasoResponse[]
  mensajes: RmaMensajeResponse[]
  createdAt: string
  updatedAt: string
}

export type SolicitarRmaRequest = {
  pedidoItemId: string
  motivo: string
  descripcion?: string
}

export type ResolverRmaRequest = {
  resolucionFinal: string
  observacionesInspeccion?: string
  responsableProbable?: string
  montoCompensacion?: number
  referenciaCompensacion?: string
  archivos?: File[]
}

export type ActualizarReposicionManualRequest = {
  transportistaId?: string
  transportistaNombre?: string
  tracking?: string
  observacion?: string
}

export type AgregarRmaMensajeRequest = {
  mensaje: string
  interno?: boolean
  archivos?: File[]
}
