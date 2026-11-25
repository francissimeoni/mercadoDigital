export type AdminStorefrontResponse = {
  id: string
  tipoStorefront?: "REVENDEDOR" | "PROVEEDOR" | null
  dominioPublico?: string | null
  revendedorId?: string | null
  proveedorId?: string | null
  nombreComercial: string | null
  logoUrl: string | null
  faviconUrl: string | null
  colorPrimario: string | null
  colorSecundario: string | null
  heroTitulo: string | null
  heroSubtitulo: string | null
  heroImagenUrl: string | null
  emailComercial: string | null
  telefonoComercial: string | null
  direccionComercial: string | null
  footerTexto: string | null
  smtpHost: string | null
  smtpPort: number | null
  smtpUsername: string | null
  smtpConfigured: boolean
  smtpAuth: boolean
  smtpStartTls: boolean
  publicada: boolean
  createdAt: string
  updatedAt: string
}

export type AdminRevendedorMercadoPagoResponse = {
  id: string | null
  estado: string
  readyForCheckout: boolean
  usaCredencialesGlobalesFallback: boolean
  mercadoPagoUserId: string | null
  publicKey: string | null
  accessTokenMasked: string | null
  accessTokenConfigured: boolean
  refreshTokenMasked: string | null
  refreshTokenConfigured: boolean
  tokenExpiresAt: string | null
  liveMode: boolean
  scope: string | null
  payoutEmail: string | null
  linkedAt: string | null
  lastRefreshAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AdminRevendedorMercadoPagoOAuthLinkResponse = {
  authorizationUrl: string
  redirectUri: string
  expiresAt: string
}

export type AdminRevendedorResponse = {
  id: string
  nombre: string
  dominio: string | null
  activo: boolean
  usuarioEmail: string | null
  usuarioConfigurado: boolean
  createdAt: string
  updatedAt: string
  storefront: AdminStorefrontResponse | null
  mercadoPago: AdminRevendedorMercadoPagoResponse | null
}

export type AdminRevendedorRequest = {
  nombre: string
  dominio: string
  usuarioEmail: string
  usuarioPassword: string
  activo: boolean
}

export type AdminRevendedorMercadoPagoRequest = {
  mercadoPagoUserId: string
  publicKey: string
  accessToken: string
  refreshToken: string
  tokenExpiresAt: string | null
  liveMode: boolean
  scope: string
  payoutEmail?: string | null
}

export type AdminStorefrontRequest = {
  dominioPublico?: string
  nombreComercial: string
  logoUrl: string
  faviconUrl: string
  colorPrimario: string
  colorSecundario: string
  heroTitulo: string
  heroSubtitulo: string
  heroImagenUrl: string
  emailComercial: string
  telefonoComercial: string
  direccionComercial: string
  footerTexto: string
  smtpHost: string
  smtpPort: number | null
  smtpUsername: string
  smtpPassword: string
  smtpAuth: boolean
  smtpStartTls: boolean
  publicada: boolean
}

export type AdminStorefrontAssetResponse = {
  url: string
  filename: string
  contentType: string
  size: number
}

export type AdminCostoMdCategoriaResponse = {
  id: string
  categoriaPaquete: string
  monto: number
  tipoCalculo: "MONTO_FIJO" | "PORCENTAJE"
  createdAt: string | null
  updatedAt: string | null
}

export type AdminCostoMdConfigResponse = {
  items: AdminCostoMdCategoriaResponse[]
}

export type AdminCostoMdCategoriaRequest = {
  categoriaPaquete: string
  monto: number
  tipoCalculo?: "MONTO_FIJO" | "PORCENTAJE"
}

export type AdminCostoMdConfigRequest = {
  items: AdminCostoMdCategoriaRequest[]
}

export type AdminPspConfigResponse = {
  id: string
  pspFeePorcentaje: number
  seguroTransportePorcentaje: number
  createdAt: string | null
  updatedAt: string | null
}

export type AdminPspConfigRequest = {
  pspFeePorcentaje: number
  seguroTransportePorcentaje: number
}

export type AdminCorreoMdConfigResponse = {
  id: string
  fromEmail: string
  fromName: string
  replyTo: string | null
  smtpHost: string | null
  smtpPort: number | null
  smtpUsername: string | null
  smtpConfigured: boolean
  smtpAuth: boolean
  smtpStartTls: boolean
  createdAt: string | null
  updatedAt: string | null
}

export type AdminCorreoMdConfigRequest = {
  fromEmail: string
  fromName: string
  replyTo: string | null
  smtpHost: string | null
  smtpPort: number | null
  smtpUsername: string | null
  smtpPassword: string | null
  smtpAuth: boolean
  smtpStartTls: boolean
}

export type AdminCorreoMdTestRequest = {
  toEmail: string
}

export type AdminCategoriaProductoResponse = {
  id: string
  nombre: string
  slug: string
  activa: boolean
  sugerida: boolean
  sugeridaPorProveedorId: string | null
  createdAt: string | null
  updatedAt: string | null
}

export type AdminCategoriaProductoRequest = {
  nombre: string
  activa?: boolean | null
}

export type AdminUsuarioTipo = "ADMIN_MD" | "LOGISTICA_MD" | "PROVEEDOR" | "REVENDEDOR" | "ASESOR_VENTA" | "CLIENTE"

export type AdminUsuarioResponse = {
  id: string
  nombre: string
  email: string
  tipoUsuario: AdminUsuarioTipo
  roles: string[]
  activo: boolean
  proveedorId: string | null
  proveedorDespachante?: boolean | null
  revendedorId: string | null
  mercadoPago: AdminRevendedorMercadoPagoResponse | null
  createdAt: string
  updatedAt: string
}

export type AdminUsuarioCreateRequest = {
  nombre: string
  email: string
  password: string
  tipoUsuario: AdminUsuarioTipo
  activo: boolean
  nombreEntidad: string
  dominioRevendedor: string
  proveedorDespachante?: boolean
}

export type AdminUsuarioUpdateRequest = {
  nombre: string
  email: string
  tipoUsuario: AdminUsuarioTipo
  activo: boolean
  nombreEntidad: string
  dominioRevendedor: string
  proveedorDespachante?: boolean
}

export type AdminProveedorDespachoResponse = {
  proveedorId: string
  nombre: string
  despachaMercaderia: boolean
  updatedAt: string
}

export type AdminProveedorDespachoRequest = {
  despachaMercaderia: boolean
}

export type AdminCanonizacionPendienteResponse = {
  productoId: string
  sku: string
  nombre: string
  descripcion: string | null
  precioCosto: number
  moneda: string
  categoriaPaquete: string
  stockDisponible: number
  imagenPrincipalUrl: string | null
  proveedorId: string
  proveedorNombre: string
  createdAt: string
  updatedAt: string
}

export type AdminCanonizacionCandidatoResponse = {
  productoCanonicoId: string
  marca: string
  modelo: string
  nombrePublico: string
  updatedAt: string
}

export type AdminCanonizacionAsignadoResponse = {
  productoId: string
  sku: string
  nombreProductoProveedor: string
  proveedorId: string
  proveedorNombre: string
  productoCanonicoId: string
  productoCanonicoNombre: string
  updatedAt: string
}

export type AdminCanonizacionCrearCanonicoRequest = {
  marca: string
  modelo: string
  nombrePublico: string
  atributosJson: string
}

export type AdminCanonizacionResultadoResponse = {
  productoId: string
  sku: string
  nombreProductoProveedor: string
  proveedorId: string
  proveedorNombre: string
  productoCanonicoId: string
  productoCanonicoNombre: string
  accion: "ASOCIADO_EXISTENTE" | "CREADO_Y_ASOCIADO" | "DESASOCIADO"
  updatedAt: string
}
