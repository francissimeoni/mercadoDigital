import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import StorefrontImageDialog from "../components/StorefrontImageDialog"
import { catalogReturn } from "../utils/catalogNavigation"
import CatalogoContactButton from "../components/CatalogoContactButton"
import { precioCatalogo } from "../utils/catalogoPublico"
import { useEffect, useMemo, useRef, useState } from "react"
import { Link, useLocation, useParams } from "react-router-dom"
import ProductoDetalleSkeleton from "../../components/catalogo/ProductoDetalleSkeleton"
import { useTenant } from "../../tenant/useTenant"
import StorefrontErrorState from "../components/StorefrontErrorState"
import StorefrontProductImage from "../components/StorefrontProductImage"
import { WhatsAppIcon } from "../components/StorefrontWhatsAppFab"
import { useStorefrontCatalogo, useStorefrontProductoDetalle } from "../hooks/useStorefrontCatalogo"
import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"
import { normalizeWhatsAppPhone } from "../utils/storefrontContact"
import { trackProductView, trackStorefrontEvent } from "../api/storefrontMetricsApi"


export default function StorefrontProductDetailPage() {
  const { productoCanonicoId } = useParams()
  return <ProductDetail key={productoCanonicoId} />
}
function ProductDetail() {
  const { productoCanonicoId } = useParams()
  const location = useLocation()
  const back = catalogReturn(location)
  const tenant = useTenant()
  const accentColor = tenant.branding?.colorPrimario || "#0f172a"
  const catalogoTarget =
    tenant.tipoStorefront === "PROVEEDOR" && tenant.proveedorId
      ? ({ tipo: "PROVEEDOR", proveedorId: tenant.proveedorId, host: tenant.host } as const)
      : tenant.revendedorId
        ? ({ tipo: "REVENDEDOR", revendedorId: tenant.revendedorId, host: tenant.host } as const)
        : null
  const detalle = useStorefrontProductoDetalle(productoCanonicoId, catalogoTarget)
  const catalogo = useStorefrontCatalogo(catalogoTarget)
  const [selectedImageIndex, setSelectedImageIndex] = useState(0)
  const [fullscreenImage, setFullscreenImage] = useState<string | null>(null)
  const campaignSearch = useMemo(() => buildCampaignSearch(location.search), [location.search])
  const detalleData = detalle.data ?? null
  const relatedProducts = useMemo(() => {
    const items = catalogo.data ?? []
    const current = items.find((item) => getProductReference(item) === productoCanonicoId)
    return items
      .filter((item) => getProductReference(item) !== productoCanonicoId)
      .sort((first, second) => {
        const firstMatchesCategory = Boolean(current?.categoriaProducto && first.categoriaProducto === current.categoriaProducto)
        const secondMatchesCategory = Boolean(current?.categoriaProducto && second.categoriaProducto === current.categoriaProducto)
        return Number(secondMatchesCategory) - Number(firstMatchesCategory)
          || (first.modoCatalogo || second.modoCatalogo ? 0 : Number((second.stockDisponible ?? 0) > 0) - Number((first.stockDisponible ?? 0) > 0))
          || Number(Boolean(second.imagenPrincipal)) - Number(Boolean(first.imagenPrincipal))
      })
      .slice(0, 10)
  }, [catalogo.data, productoCanonicoId])

  const images = useMemo(() => {
    const fromApi = (detalleData?.imagenes ?? []).filter(Boolean)
    if (fromApi.length > 0) {
      return fromApi
    }
    return detalleData?.imagenPrincipal ? [detalleData.imagenPrincipal] : []
  }, [detalleData])

  const activeImage = images[selectedImageIndex] || detalleData?.imagenPrincipal || null

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [productoCanonicoId])

  useEffect(() => {
    if (tenant.tipoStorefront === "REVENDEDOR" && detalleData?.productoId) {
      trackProductView(detalleData.productoId, tenant.host)
    }
  }, [detalleData?.productoId, tenant.host, tenant.tipoStorefront])



  if (!productoCanonicoId) {
    return <StorefrontErrorState title="Producto no disponible" message="No se encontro un identificador de producto valido." />
  }

  if (detalle.isLoading) {
    return <ProductoDetalleSkeleton />
  }

  if (detalle.isError || !detalle.data) {
    return (
      <StorefrontErrorState
        title="No se pudo cargar el producto"
        message="Reintenta desde el catalogo de la tienda."
        actionLabel="Reintentar"
        onAction={() => void detalle.refetch()}
      />
    )
  }

  const whatsappPhone = tenant.branding?.telefonoComercial
    ? normalizeWhatsAppPhone(tenant.branding.telefonoComercial)
    : null
  const whatsappUrl = whatsappPhone
    ? buildProductWhatsAppUrl(whatsappPhone, productoCanonicoId, tenant.host, location.search)
    : null

  return (
    <section className="sf-product-detail">
      {location.state?.compraCatalogo && <div role="status" className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-950">Ahora esta tienda recibe consultas por WhatsApp. Elegí un contacto para coordinar tu compra.{location.state?.pedidoSinEnlace && <> Si ya tenés un pedido, consultalo desde <Link to="/mi-cuenta" className="font-semibold underline">tu cuenta</Link> o usá el enlace de seguimiento que recibiste.</>}</div>}
      <div className="sf-detail-panel">
        <div className="mb-5">
          <Link to={back.path} state={{ catalogRestore: back }} className="text-sm font-semibold" style={{ color: accentColor }}>
            {"<-"} Volver al catalogo
          </Link>
        </div>

        <div className="sf-detail-grid">
          <div className={`sf-gallery ${images.length > 1 ? "sf-gallery-multiple" : ""}`}>
            <div className="sf-gallery-stage">
              <button
                type="button"
                className="h-full w-full cursor-zoom-in" aria-label="Ampliar imagen del producto" disabled={!activeImage}
                onClick={() => {
                  if (activeImage) {
                    setFullscreenImage(activeImage)
                  }
                }}
              >
                <StorefrontProductImage src={activeImage} alt={detalle.data.nombrePublico} className="h-full w-full object-contain p-3" priority />
              </button>

            </div>
            {images.length > 1 ? (
              <div className="sf-gallery-thumbnails">
                {images.map((image, index) => (
                  <button
                    key={`${image}-${index}`}
                    aria-label={`Ver imagen ${index + 1}`} aria-pressed={index === selectedImageIndex}
                    type="button"
                    className={`aspect-square w-20 shrink-0 snap-start overflow-hidden rounded-xl border bg-slate-50 p-1.5 transition sm:w-24 ${
                      index === selectedImageIndex
                        ? "border-slate-900 ring-1 ring-slate-300"
                        : "border-slate-200 hover:border-slate-300"
                    }`}
                    onClick={() => {
                      setSelectedImageIndex(index)
                    }}
                  >
                    <StorefrontProductImage src={image} alt={`${detalle.data.nombrePublico} ${index + 1}`} className="h-full w-full object-contain" />
                  </button>
                ))}
              </div>
            ) : null}
          </div>

          <div className="sf-detail-summary">
            <div className="sf-product-heading">
              {catalogo.data?.find(p => getProductReference(p) === productoCanonicoId)?.categoriaProducto && <p className="sf-eyebrow">{catalogo.data.find(p => getProductReference(p) === productoCanonicoId)?.categoriaProducto}</p>}
              
              <h1 className="sf-product-title">{detalle.data.nombrePublico}</h1>

            </div>

            <div className="sf-detail-price">
              <p className="sf-muted">{detalle.data.modoCatalogo ? "Precio en pesos argentinos" : "Precio de contado o transferencia"}</p>
              <p className="sf-price">
                {precioCatalogo(detalle.data)}
              </p>
              {MERCADO_PAGO_DISPONIBLE && !detalle.data.modoCatalogo && tenant.storefront?.mercadoPagoHabilitado && <div className="mt-3 rounded-2xl border border-sky-100 bg-sky-50 px-4 py-3"><p>Pagá con tarjeta de crédito</p><strong>{new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(detalle.data.mejorPrecioFinalEstimado * (1 + (tenant.storefront.mercadoPagoRecargoPorcentaje ?? 0) / 100))}</strong></div>}
            </div>

            {!detalle.data.modoCatalogo && <div className="grid gap-3">
              <DetailMetric label="Disponibilidad" value={(detalle.data.stockDisponible ?? 0) > 0 ? "En stock" : "Sin stock"} />
            </div>}
            {detalle.data.modoCatalogo && detalle.data.condicionesEntrega && <details className="sf-delivery"><summary><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true"><path d="M3 5h11v12H3zM14 9h4l3 4v4h-7"/><circle cx="7" cy="18" r="2"/><circle cx="18" cy="18" r="2"/></svg><span>Condiciones de entrega</span><span className="sf-delivery-more" aria-hidden="true">+</span></summary><p>{detalle.data.condicionesEntrega}</p></details>}

            {detalle.data.modoCatalogo ? <div className="sf-detail-contact"><span className="sf-mobile-price">{precioCatalogo(detalle.data)}</span><CatalogoContactButton key={`${tenant.host}-${productoCanonicoId}`} productoId={productoCanonicoId} host={tenant.host ?? ""} precioMostrado={detalle.data.precioPublicoArs ?? ""} /></div> : <div className={`grid gap-3 ${whatsappUrl ? "sm:grid-cols-2" : ""}`}>
              <Link
                to={`/producto/${productoCanonicoId}/resumen-compra${campaignSearch}`}
                className="inline-flex min-h-[48px] items-center justify-center rounded-full px-5 py-3 text-sm font-semibold text-white shadow-sm"
                style={{ backgroundColor: accentColor }}
              >
                Comprar ahora
              </Link>
              {whatsappUrl ? (
                <a
                  href={whatsappUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={() => trackStorefrontEvent("CLICK_WHATSAPP", { host: tenant.host, productoId: detalle.data.productoId })}
                  className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-full bg-[#25D366] px-5 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-[#20bd5a]"
                >
                  <WhatsAppIcon className="h-5 w-5" />
                  Consultar por WhatsApp
                </a>
              ) : null}
            </div>}
            
          </div>
        </div>

        {detalle.data.descripcion && <section className="sf-description" aria-labelledby="description-title"><h2 id="description-title">Descripción y características</h2><p>{detalle.data.descripcion}</p></section>}
        {relatedProducts.length > 0 ? (
          <RelatedProductsCarousel key={productoCanonicoId} items={relatedProducts} accentColor={accentColor} campaignSearch={campaignSearch} returnState={back} />
        ) : null}
      </div>

      {fullscreenImage && <StorefrontImageDialog src={fullscreenImage} alt={detalle.data.nombrePublico} close={() => setFullscreenImage(null)} />}
    </section>
  )
}

function buildCampaignSearch(currentSearch: string) {
  const source = new URLSearchParams(currentSearch)
  const allowedKeys = ["lista", "asesor", "asesorId", "storefrontHost"]
  const next = new URLSearchParams()

  allowedKeys.forEach((key) => {
    const value = source.get(key)
    if (value && value.trim()) {
      next.set(key, value.trim())
    }
  })

  const search = next.toString()
  return search ? `?${search}` : ""
}

function buildProductWhatsAppUrl(phone: string, productId: string, tenantHost: string | null, currentSearch: string) {
  const host = tenantHost || window.location.hostname
  const shareUrl = new URL(`/api/public/share/producto/${productId}`, window.location.origin)
  shareUrl.searchParams.set("storefrontHost", host)
  shareUrl.searchParams.set("v", "og2")
  const campaign = new URLSearchParams(currentSearch)
  const advisorId = campaign.get("asesorId")
  const advisor = campaign.get("asesor")
  if (advisorId?.trim()) shareUrl.searchParams.set("asesorId", advisorId.trim())
  if (advisor?.trim()) shareUrl.searchParams.set("asesor", advisor.trim())
  const message = `Hola! Quiero saber más sobre este producto que vi en ${shareUrl.toString()}`
  return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`
}

function DetailMetric({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{label}</p>
      <p className="mt-2 text-base font-semibold text-slate-900">{value}</p>
    </article>
  )
}

function RelatedProductsCarousel({ items, accentColor, campaignSearch, returnState }: { items: StorefrontCatalogoItem[]; accentColor: string; campaignSearch: string; returnState: ReturnType<typeof catalogReturn> }) {
  const [activeIndex, setActiveIndex] = useState(0)
  const scrollerRef = useRef<HTMLDivElement | null>(null)




  useEffect(() => {
    const scroller = scrollerRef.current
    const target = scroller?.children.item(activeIndex) as HTMLElement | null
    if (!scroller || !target) return
    scroller.scrollTo({ left: target.offsetLeft - scroller.offsetLeft, behavior: window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth" })
  }, [activeIndex])

  const move = (direction: -1 | 1) => {
    setActiveIndex((current) => (current + direction + items.length) % items.length)
  }

  return (
    <section className="mt-8 border-t border-slate-200 pt-6">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          
          <h2 className="mt-1 text-xl font-semibold text-slate-950">Productos relacionados</h2>
        </div>
        {items.length > 1 ? (
          <div className="flex gap-2">
            <CarouselButton label="Anterior" icon="‹" onClick={() => move(-1)} />
            <CarouselButton label="Siguiente" icon="›" onClick={() => move(1)} />
          </div>
        ) : null}
      </div>

      <div ref={scrollerRef} className="flex snap-x gap-3 overflow-x-auto pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {items.map((item) => {
          const reference = getProductReference(item)
          return (
            <Link
              key={reference}
              to={`/producto/${reference}${campaignSearch}`}
              state={{ catalogReturn: returnState }}
              className="group flex min-w-[78%] snap-start items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-3 transition hover:-translate-y-0.5 hover:border-slate-300 hover:bg-white hover:shadow-md sm:min-w-[calc(50%-0.375rem)] lg:min-w-[calc(33.333%-0.5rem)] xl:min-w-[calc(25%-0.6rem)]"
            >
              <div className="h-20 w-20 shrink-0 overflow-hidden rounded-xl border border-slate-200 bg-white">
                <StorefrontProductImage src={item.imagenPrincipal} alt={item.nombrePublico} className="h-full w-full object-contain p-1.5" />
              </div>
              <div className="min-w-0">
                <h3 className="line-clamp-2 text-sm font-semibold leading-5 text-slate-900 group-hover:text-slate-950">{item.nombrePublico}</h3>
                <p className="mt-2 break-words text-sm font-bold" style={{ color: accentColor }}>{precioCatalogo(item)}</p>
              </div>
            </Link>
          )
        })}
      </div>
    </section>
  )
}

function CarouselButton({ label, icon, onClick }: { label: string; icon: string; onClick: () => void }) {
  return <button type="button" aria-label={label} onClick={onClick} className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 bg-white text-xl text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50">{icon}</button>
}

function getProductReference(item: StorefrontCatalogoItem) {
  return item.productoCanonicoId || item.productoId
}
