import { useMemo, useState } from "react"
import { Link, Navigate, useLocation, useParams } from "react-router-dom"
import ProductoDetalleSkeleton from "../../components/catalogo/ProductoDetalleSkeleton"
import { useTenant } from "../../tenant/useTenant"
import StorefrontErrorState from "../components/StorefrontErrorState"
import StorefrontProductImage from "../components/StorefrontProductImage"
import { useStorefrontProductoDetalle } from "../hooks/useStorefrontCatalogo"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

export default function StorefrontCompraResumenPage() {
  const { productoCanonicoId } = useParams()
  const location = useLocation()
  const tenant = useTenant()
  const accentColor = tenant.branding?.colorPrimario || "#0f172a"
  const catalogoTarget =
    tenant.tipoStorefront === "PROVEEDOR" && tenant.proveedorId
      ? ({ tipo: "PROVEEDOR", proveedorId: tenant.proveedorId, host: tenant.host } as const)
      : tenant.revendedorId
        ? ({ tipo: "REVENDEDOR", revendedorId: tenant.revendedorId, host: tenant.host } as const)
        : null
  const detalle = useStorefrontProductoDetalle(productoCanonicoId, catalogoTarget)
  const [seguroTransporte, setSeguroTransporte] = useState(false)
  const [showSeguroInfo, setShowSeguroInfo] = useState(false)
  const campaignSearch = useMemo(() => buildCampaignSearch(location.search), [location.search])

  if (!productoCanonicoId) {
    return <StorefrontErrorState title="Compra no disponible" message="No se encontro un identificador de producto valido." />
  }

  if (detalle.isLoading) {
    return <ProductoDetalleSkeleton />
  }

  if (detalle.isError || !detalle.data) {
    return (
      <StorefrontErrorState
        title="No se pudo preparar la compra"
        message="Reintenta desde el detalle del producto."
        actionLabel="Reintentar"
        onAction={() => void detalle.refetch()}
      />
    )
  }

  const detalleData = detalle.data
  if (detalleData.modoCatalogo) return <Navigate replace to={`/producto/${productoCanonicoId}${campaignSearch}`} />
  if (detalleData.seguroTransporteEstimado === null || detalleData.totalConSeguroEstimado === null) return <StorefrontErrorState title="Compra no disponible" message="No se pudieron obtener los importes de la compra." />
  const seguroEstimado = detalleData.seguroTransporteEstimado
  const totalSinSeguro = detalleData.mejorPrecioFinalEstimado
  const totalConSeguro = detalleData.totalConSeguroEstimado
  const totalActual = seguroTransporte ? totalConSeguro : totalSinSeguro
  const checkoutSearch = buildCheckoutSearch(campaignSearch, seguroTransporte)

  return (
    <section className="space-y-6">
      <div className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="mb-5">
          <Link to={`/producto/${productoCanonicoId}${campaignSearch}`} className="text-sm font-semibold" style={{ color: accentColor }}>
            {"<-"} Volver al detalle
          </Link>
        </div>

        <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
          <article className="rounded-[1.8rem] border border-slate-200 bg-white p-5">
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Resumen de la compra</p>
            <div className="mt-4 flex items-start gap-4">
              <div className="h-24 w-24 overflow-hidden rounded-[1.2rem] border border-slate-200 bg-slate-50">
                <StorefrontProductImage src={detalleData.imagenPrincipal} alt={detalleData.nombrePublico} className="h-full w-full object-contain p-1.5" />
              </div>
              <div className="space-y-1">
                <h1 className="text-xl font-semibold text-slate-950">{detalleData.nombrePublico}</h1>
                <p className="text-sm text-slate-600">Entrega estimada: {detalleData.demoraEstimadaDias} dias</p>
              </div>
            </div>

            <div className="mt-5 grid gap-3">
              <SummaryRow label="Envio" value="Incluido" />
              <SummaryRow label="Precio final" value={currencyFormatter.format(totalSinSeguro)} />
              <SummaryRow label="Seguro de envio" value={seguroTransporte ? currencyFormatter.format(seguroEstimado) : "No agregado"} />
              <SummaryRow label="Total a pagar" value={currencyFormatter.format(totalActual)} highlight />
            </div>
          </article>

          <aside className="rounded-[1.8rem] border border-slate-200 bg-slate-50 p-5">
            <h2 className="text-lg font-semibold text-slate-950">Seguro de envio</h2>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              Puedes agregar cobertura ante perdida o rotura durante el transporte.
            </p>

            <label className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-slate-700">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300"
                checked={seguroTransporte}
                onChange={(event) => setSeguroTransporte(event.target.checked)}
              />
              Agregar seguro de envio
            </label>

            <p className="mt-3 rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm font-semibold text-amber-700">
              {`Asegura tu envio por tan solo ${currencyFormatter.format(seguroEstimado)}`}
            </p>

            <p className="mt-2 text-sm font-semibold text-slate-900">
              {seguroTransporte ? `Con seguro pagas ${currencyFormatter.format(totalConSeguro)}` : `Sin seguro pagas ${currencyFormatter.format(totalSinSeguro)}`}
            </p>

            <button
              type="button"
              className="mt-3 text-sm font-semibold"
              style={{ color: accentColor }}
              onClick={() => setShowSeguroInfo((current) => !current)}
            >
              {showSeguroInfo ? "Ocultar info" : "Ver en que consiste el seguro"}
            </button>

            {showSeguroInfo ? (
              <div className="mt-2 rounded-[1rem] border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs leading-5 text-amber-900">
                El seguro cubre perdida o rotura del producto durante el envio y prioriza la reposicion del pedido.
              </div>
            ) : null}

            <Link
              to={`/producto/${productoCanonicoId}/checkout${checkoutSearch}`}
              className="mt-5 inline-flex min-h-[44px] w-full items-center justify-center rounded-full px-5 py-3 text-sm font-semibold text-white shadow-sm"
              style={{ backgroundColor: accentColor }}
            >
              Comprar
            </Link>
          </aside>
        </div>
      </div>
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

function buildCheckoutSearch(campaignSearch: string, seguroTransporte: boolean) {
  const base = new URLSearchParams(campaignSearch.startsWith("?") ? campaignSearch.slice(1) : campaignSearch)
  if (seguroTransporte) {
    base.set("seguro", "1")
  } else {
    base.delete("seguro")
  }
  const search = base.toString()
  return search ? `?${search}` : ""
}

function SummaryRow({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`flex items-center justify-between gap-3 rounded-2xl px-4 py-3 ${highlight ? "bg-slate-900 text-white" : "bg-slate-50"}`}>
      <span className={`text-sm ${highlight ? "text-slate-200" : "text-slate-600"}`}>{label}</span>
      <span className="text-sm font-semibold text-right">{value}</span>
    </div>
  )
}
