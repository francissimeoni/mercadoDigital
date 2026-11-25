import storefrontOwnerClient from "./storefrontOwnerClient"

export type CategoriaProducto = { id: string; nombre: string }
export type ProductoMedia = { id: string; tipoMedia: "IMAGEN" | "VIDEO"; url: string; orden: number; esPrincipal: boolean }
export type ProductoPropio = {
  precioPublicoArs?: string
  id: string
  sku: string
  nombre: string
  descripcion: string | null
  precioVenta: number | null
  moneda: string
  stock: number | null
  categoriaPaquete: string | null
  categoriaProductoId: string | null
  categoriaProductoNombre: string | null
  activo: boolean
  promocion: boolean
  novedad: boolean
  media: ProductoMedia[]
  imagenPrincipalUrl: string | null
}
export type ProductoAnterior = ProductoPropio & { precioVenta: number; categoriaPaquete: string }
export const esProductoAnterior = (p: ProductoPropio): p is ProductoAnterior => p.precioVenta !== null && p.categoriaPaquete !== null
export type ProductoPropioInput = Omit<ProductoAnterior, "id" | "moneda" | "categoriaProductoNombre" | "media" | "imagenPrincipalUrl">
export type AdminImportImageMode = "CONSERVAR" | "AGREGAR" | "REEMPLAZAR_PORTADA"
export type AdminProductoImportItem = ProductoPropioInput & {
  fila: number
  incluir: boolean
  imagenes: string[]
  modoImagenes: AdminImportImageMode
}
export type AdminProductoImportRequest = {
  items: AdminProductoImportItem[]
  actualizarNombre: boolean
  actualizarDescripcion: boolean
  actualizarPrecio: boolean
  actualizarStock: boolean
  actualizarCategoria: boolean
  actualizarCategoriaPaquete: boolean
  actualizarEstado: boolean
  actualizarDestacados: boolean
  actualizarImagenes: boolean
}
export type AdminProductoImportResult = {
  filasRecibidas: number
  creados: number
  actualizados: number
  omitidos: number
  rechazados: number
  errores: { fila: number; sku: string | null; mensaje: string }[]
}
export type PreguntaProducto = {
  id: string; productoNombre: string; nombreContacto: string | null; emailContacto: string | null
  telefonoContacto: string | null; pregunta: string; respuesta: string | null; estado: string
}
export type SolicitudDisponibilidad = {
  id: string; productoNombre: string; nombreContacto: string | null; emailContacto: string | null
  telefonoContacto: string | null; localidad: string; observaciones: string | null
  estado: string; respuestaProveedor: string | null
}
export type Asesor = { id: string; nombre: string; email: string | null; fotoUrl: string | null; activo: boolean; whatsappComercial: string | null; provincia?: string | null; localidad?: string | null; zonaAtencion?: string | null }
export type AsesorInput = { nombre: string; activo: boolean; whatsappComercial?: string; paisTelefono?: string; telefonoMovil?: boolean; provincia?: string; localidad?: string; zonaAtencion?: string; quitarFoto?: boolean; email?: string; password?: string }
export type MercadoPagoEstado = {
  estado: string; readyForCheckout: boolean; usaCredencialesGlobalesFallback: boolean
  mercadoPagoUserId?: string | null; payoutEmail?: string | null; linkedAt?: string | null
}
export type MercadoPagoLink = { authorizationUrl: string; redirectUri: string; expiresAt: string }
export type MetricaRanking = { productoId: string | null; nombre: string; total: number; imagenPrincipalUrl?: string | null }
export type StorefrontMetrics = {
  desde: string; hasta: string; visitantesUnicos: number; visitas: number; vistasProductos: number
  consultas: number; clicksWhatsapp: number; rubrosMasVistos: MetricaRanking[]; productosMasVistos: MetricaRanking[]
}
export type StorefrontGalleryItem = { id: string | null; imagenUrl: string; titulo: string | null; comentario: string | null }
export type StorefrontHeroSlide = { id: string | null; imagenUrl: string; titulo: string | null; subtitulo: string | null }
export type StorefrontContent = {
  nombreComercial: string; logoUrl: string | null; faviconUrl: string | null
  colorPrimario: string | null; colorSecundario: string | null
  heroTitulo: string | null; heroSubtitulo: string | null; heroImagenUrl: string | null; portadas: StorefrontHeroSlide[]; footerTexto: string | null
  quienesTitulo: string | null; quienesTexto: string | null; quienesGaleria: StorefrontGalleryItem[]
  emailComercial: string | null; telefonoComercial: string | null; direccionComercial: string | null
  instagramUrl: string | null; facebookUrl: string | null; tiktokUrl: string | null; youtubeUrl: string | null
  transferenciaHabilitada: boolean; transferenciaAlias: string | null; transferenciaTitular: string | null
  smtpHost: string | null; smtpPort: number | null; smtpUsername: string | null; smtpPassword?: string
  smtpConfigured: boolean; smtpAuth: boolean; smtpStartTls: boolean
}
export type StorefrontAsset = { url: string; filename: string; contentType: string; size: number }
export type OwnerOrderItem = {
  imagenPrincipal?: string | null
  itemId: string; productoId: string | null; revendedorProductoId: string | null
  sku: string | null; nombre: string; cantidad: number; precioUnitario: number; subtotal: number
}
export type OwnerOrder = {
  totalPsp?: number; montoPago?: number | null; referenciaPago?: string | null; fechaActualizacionPago?: string | null
  pedidoId: string; estadoPedido: string; estadoEnvio: string | null; direccionEnvio: string | null
  categoriaPaquete: string | null; transportistaNombre: string | null; tracking: string | null
  totalFinal: number; totalBruto: number; totalEnvio: number; totalSeguro: number; totalDescuento: number
  fechaCreacionPedido: string; fechaActualizacionPedido: string | null
  clienteNombre: string | null; clienteEmail: string | null; clienteTelefono: string | null
  metodoPago: string | null; estadoPago: string | null; comprobanteCargado: boolean
  comprobanteCargadoAt: string | null; items: OwnerOrderItem[]
}

const base = (revendedorId: string) => `/revendedores/${revendedorId}`

export async function listarCategorias(id: string) {
  return (await storefrontOwnerClient.get<CategoriaProducto[]>(`${base(id)}/categorias-producto`)).data
}
export async function listarProductos(id: string) {
  return (await storefrontOwnerClient.get<ProductoPropio[]>(`${base(id)}/productos-propios`)).data
}
export async function crearProducto(id: string, input: ProductoPropioInput) {
  return (await storefrontOwnerClient.post<ProductoPropio>(`${base(id)}/productos-propios`, input)).data
}
export async function actualizarProducto(id: string, productId: string, input: ProductoPropioInput) {
  return (await storefrontOwnerClient.put<ProductoPropio>(`${base(id)}/productos-propios/${productId}`, input)).data
}
export async function agregarImagen(id: string, productId: string, url: string, orden: number, esPrincipal: boolean) {
  return (await storefrontOwnerClient.post<ProductoMedia>(`${base(id)}/productos-propios/${productId}/media`, { tipoMedia: "IMAGEN", url, orden, esPrincipal })).data
}
export async function actualizarImagen(id: string, productId: string, media: ProductoMedia, esPrincipal: boolean) {
  return (await storefrontOwnerClient.put<ProductoMedia>(`${base(id)}/productos-propios/${productId}/media/${media.id}`, { ...media, esPrincipal })).data
}
export async function eliminarImagen(id: string, productId: string, mediaId: string) {
  await storefrontOwnerClient.delete(`${base(id)}/productos-propios/${productId}/media/${mediaId}`)
}
export async function importarProductosComoAdmin(id: string, input: AdminProductoImportRequest) {
  return (await storefrontOwnerClient.post<AdminProductoImportResult>(`/admin/revendedores/${id}/productos-propios/importar`, input)).data
}
export async function listarPreguntas(id: string) {
  return (await storefrontOwnerClient.get<PreguntaProducto[]>(`${base(id)}/productos-propios/consultas`)).data
}
export async function responderPregunta(id: string, questionId: string, respuesta: string) {
  return (await storefrontOwnerClient.put(`${base(id)}/productos-propios/consultas/${questionId}/respuesta`, { respuesta })).data
}
export async function listarDisponibilidad(id: string) {
  return (await storefrontOwnerClient.get<SolicitudDisponibilidad[]>(`${base(id)}/productos-propios/disponibilidad`)).data
}
export async function responderDisponibilidad(id: string, requestId: string, disponible: boolean, respuestaProveedor: string) {
  return (await storefrontOwnerClient.put(`${base(id)}/productos-propios/disponibilidad/${requestId}/respuesta`, { disponible, respuestaProveedor })).data
}
export async function listarAsesores(id: string) {
  return (await storefrontOwnerClient.get<Asesor[]>(`${base(id)}/asesores`)).data
}
export async function guardarAsesor(id: string, asesorId: string | null, input: AsesorInput, foto?: File | null) {
  const url = `${base(id)}/asesores${asesorId ? `/${asesorId}` : ""}`
  let data: AsesorInput | FormData = input
  if (foto) {
    data = new FormData()
    data.append("datos", new Blob([JSON.stringify(input)], { type: "application/json" }))
    data.append("foto", foto)
  }
  return (await storefrontOwnerClient.request<Asesor>({ method: asesorId ? "PUT" : "POST", url, data })).data
}
export async function crearAsesor(id: string, input: { nombre: string; email?: string; password?: string; activo: boolean; whatsappComercial?: string }) {
  return (await storefrontOwnerClient.post<Asesor>(`${base(id)}/asesores`, input)).data
}
export async function actualizarAsesor(id: string, advisorId: string, input: { nombre: string; email?: string; fotoUrl?: string | null; activo: boolean; whatsappComercial?: string }) {
  return (await storefrontOwnerClient.put<Asesor>(`${base(id)}/asesores/${advisorId}`, input)).data
}
export async function getMercadoPago(id: string) {
  return (await storefrontOwnerClient.get<MercadoPagoEstado>(`${base(id)}/mercadopago`)).data
}
export async function getMercadoPagoLink(id: string) {
  return (await storefrontOwnerClient.get<MercadoPagoLink>(`${base(id)}/mercadopago/oauth-link`)).data
}
export async function unlinkMercadoPago(id: string) {
  return (await storefrontOwnerClient.delete<MercadoPagoEstado>(`${base(id)}/mercadopago`)).data
}
export async function getStorefrontMetrics(id: string, desde: string, hasta: string) {
  return (await storefrontOwnerClient.get<StorefrontMetrics>(`${base(id)}/metricas`, { params: { desde, hasta } })).data
}
export async function getStorefrontContent(id: string) {
  return (await storefrontOwnerClient.get<StorefrontContent>(`${base(id)}/storefront/contenido`)).data
}
export async function updateStorefrontContent(id: string, input: StorefrontContent) {
  const { smtpConfigured, ...request } = input
  void smtpConfigured
  return (await storefrontOwnerClient.put<StorefrontContent>(`${base(id)}/storefront/contenido`, request)).data
}
export async function uploadStorefrontImage(id: string, file: File, tipo = "galeria") {
  const form = new FormData()
  form.append("archivo", file)
  form.append("tipo", tipo)
  return (await storefrontOwnerClient.post<StorefrontAsset>(`${base(id)}/storefront/assets`, form)).data
}
export async function probarCorreoStorefront(id: string) {
  await storefrontOwnerClient.post(`${base(id)}/storefront/email/prueba`)
}
export async function listarPedidos(id: string) {
  return (await storefrontOwnerClient.get<OwnerOrder[]>(`${base(id)}/pedidos`)).data
}
export async function confirmarTransferencia(id: string, pedidoId: string) {
  await storefrontOwnerClient.put(`${base(id)}/pedidos/${pedidoId}/confirmar-transferencia`)
}
export async function abrirComprobante(id: string, pedidoId: string) {
  const response = await storefrontOwnerClient.get<Blob>(`${base(id)}/pedidos/${pedidoId}/comprobante`, { responseType: "blob" })
  const url = URL.createObjectURL(response.data)
  window.open(url, "_blank", "noopener,noreferrer")
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
}
export async function abrirRotuloPedido(id: string, pedidoId: string) {
  const target = window.open("", "_blank")
  try {
    const response = await storefrontOwnerClient.get<Blob>(`${base(id)}/pedidos/${pedidoId}/rotulo`, { responseType: "blob" })
    const url = URL.createObjectURL(response.data)
    if (target) target.location.href = url
    else window.open(url, "_blank", "noopener,noreferrer")
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
  } catch (error) {
    target?.close()
    throw error
  }
}
export async function despacharPedido(id: string, pedidoId: string, input: { transportistaNombre: string; tracking: string }) {
  await storefrontOwnerClient.post(`${base(id)}/pedidos/${pedidoId}/despachar`, input)
}
export async function entregarPedido(id: string, pedidoId: string) {
  await storefrontOwnerClient.post(`${base(id)}/pedidos/${pedidoId}/entregar`)
}

export async function cancelarPedidoImpago(id: string, pedidoId: string) {
  await storefrontOwnerClient.put(`${base(id)}/pedidos/${pedidoId}/cancelar-impago`)
}

export async function ajustarPreciosMasivamente(id: string, items: { productoId: string; precioFinal: number }[]) {
  return (await storefrontOwnerClient.put<ProductoPropio[]>(`${base(id)}/productos-propios/precios/ajuste-masivo`, { items })).data
}
