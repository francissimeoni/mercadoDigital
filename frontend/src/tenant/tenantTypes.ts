export type TenantStorefront = {
  modoCatalogo?: boolean
  mercadoPagoHabilitado?: boolean
  mercadoPagoRecargoPorcentaje?: number
  tipoStorefront: "REVENDEDOR" | "PROVEEDOR"
  revendedorId: string | null
  proveedorId: string | null
  dominio: string
  nombreComercial: string
  logoUrl: string | null
  faviconUrl: string | null
  colorPrimario: string | null
  colorSecundario: string | null
  heroTitulo: string | null
  heroSubtitulo: string | null
  heroImagenUrl: string | null
  portadas: StorefrontHeroSlide[]
  emailComercial: string | null
  telefonoComercial: string | null
  direccionComercial: string | null
  footerTexto: string | null
  quienesTitulo: string | null
  quienesTexto: string | null
  quienesGaleria: StorefrontGalleryItem[]
  instagramUrl: string | null
  facebookUrl: string | null
  tiktokUrl: string | null
  youtubeUrl: string | null
  transferenciaHabilitada: boolean
  transferenciaAlias: string | null
  transferenciaTitular: string | null
  publicada: boolean
}

export type StorefrontGalleryItem = {
  id: string | null
  imagenUrl: string
  titulo: string | null
  comentario: string | null
}

export type StorefrontHeroSlide = {
  id: string | null
  imagenUrl: string
  titulo: string | null
  subtitulo: string | null
}

export type TenantBranding = {
  nombreComercial: string
  logoUrl: string | null
  faviconUrl: string | null
  colorPrimario: string | null
  colorSecundario: string | null
  heroTitulo: string | null
  heroSubtitulo: string | null
  heroImagenUrl: string | null
  portadas: StorefrontHeroSlide[]
  emailComercial: string | null
  telefonoComercial: string | null
  direccionComercial: string | null
  footerTexto: string | null
  quienesTitulo: string | null
  quienesTexto: string | null
  quienesGaleria: StorefrontGalleryItem[]
  instagramUrl: string | null
  facebookUrl: string | null
  tiktokUrl: string | null
  youtubeUrl: string | null
  transferenciaHabilitada: boolean
  transferenciaAlias: string | null
  transferenciaTitular: string | null
}

export type TenantErrorKind =
  | "not_available"
  | "network"
  | "invalid_host"
  | "local_unresolved"

export type TenantError = {
  kind: TenantErrorKind
  message: string
}

export type TenantStatus =
  | "idle"
  | "loading"
  | "resolved"
  | "error"
  | "local_unresolved"

export type TenantSource = "browser" | "query-override" | "session-override" | "dev-override" | null

export type TenantContextValue = {
  status: TenantStatus
  isLoading: boolean
  error: TenantError | null
  storefront: TenantStorefront | null
  revendedorId: string | null
  proveedorId: string | null
  tipoStorefront: "REVENDEDOR" | "PROVEEDOR" | null
  branding: TenantBranding | null
  host: string | null
  source: TenantSource
  isAvailable: boolean
  refresh: () => Promise<void>
}
