import { Link } from "react-router-dom"
import ProveedorMetricCard from "../../components/proveedor/ProveedorMetricCard"
import ProveedorModuleHeader from "../../components/proveedor/ProveedorModuleHeader"
import ProveedorPedidoItem from "../../components/proveedor/ProveedorPedidoItem"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { useProveedorContext } from "../../hooks/useProveedorContext"
import { usePedidosProveedor } from "../../hooks/useProveedorPedidos"
import { useProveedorProductos } from "../../hooks/useProveedorProductos"
import type { PedidoProveedorResponse, ProveedorProductoResponse } from "../../types/proveedor"

type AlertItem = {
  id: string
  tone: "red" | "amber" | "blue"
  title: string
  description: string
  ctaLabel: string
  to: string
}

function formatFechaCorta(value: string | null) {
  if (!value) return "Sin registros"

  return new Date(value).toLocaleString("es-AR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  })
}

function formatRelativeAge(value: string) {
  const diffMs = Date.now() - new Date(value).getTime()
  const diffHours = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)))

  if (diffHours < 24) return `${diffHours} h`

  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays} d`
}

export default function ProveedorDashboardPage() {
  const { proveedorId, email } = useProveedorContext()
  const pedidosQuery = usePedidosProveedor(proveedorId)
  const productosQuery = useProveedorProductos(proveedorId)

  const pedidos = pedidosQuery.data ?? []
  const productos = productosQuery.data ?? []

  const totalPedidos = pedidos.length
  const totalUnidades = pedidos.reduce((acum, item) => acum + item.cantidad, 0)
  const pedidosConDireccion = pedidos.filter((item) => Boolean(item.direccionEnvio)).length
  const pedidoMasAntiguo = sortPedidosByOldest(pedidos)[0] ?? null

  const productosActivos = productos.filter((item) => item.activo).length
  const productosSinStock = productos.filter((item) => item.stockDisponible <= 0).length
  const productosPendientes = productos.filter((item) => item.estadoNormalizacion === "PENDIENTE").length
  const productosSinMedia = productos.filter((item) => item.cantidadMedia <= 0).length

  const previewPedidos = sortPedidosByPriority(pedidos).slice(0, 4)
  const productosCriticos = productos.filter((item) => item.stockDisponible <= 0).slice(0, 4)
  const productosSinNormalizar = productos.filter((item) => item.estadoNormalizacion === "PENDIENTE").slice(0, 4)
  const productosSinMediaPreview = productos.filter((item) => item.cantidadMedia <= 0).slice(0, 4)

  const alertas = buildAlertas({ pedidos, productos })
  const loadingDashboard = proveedorId && pedidosQuery.isLoading
  const hasPedidosError = proveedorId && pedidosQuery.isError
  const hasProductosError = proveedorId && productosQuery.isError

  return (
    <section className="space-y-6">
      <ProveedorModuleHeader
        eyebrow="Proveedor"
        title="Panel de Control"
        description="Supervisión global de pedidos, catálogo de productos y logística de entrada."
        proveedorId={proveedorId}
        aside={
          <div className="flex items-center gap-4">
             <div className="hidden shrink-0 lg:block">
              <p className="text-right text-[10px] font-bold uppercase tracking-wider text-slate-400">Usuario activo</p>
              <p className="text-right text-sm font-semibold text-slate-700">{email || "Proveedor"}</p>
            </div>
            <Link
              to="/proveedor/productos"
              className="rounded-full bg-slate-950 px-6 py-2.5 text-sm font-bold text-white shadow-sm transition hover:bg-slate-800"
            >
              Catálogo
            </Link>
          </div>
        }
      />

      {!proveedorId ? (
        <FeedbackAlert variant="warning" title="Proveedor no resuelto">
          Falta `proveedorId` para cargar el dashboard operativo del proveedor.
        </FeedbackAlert>
      ) : null}

      {loadingDashboard ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-36 animate-pulse rounded-3xl border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {hasPedidosError ? (
        <FeedbackAlert variant="error" title="No se pudo cargar la operacion de pedidos">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <p>Ocurrio un problema consultando la bandeja operativa del proveedor.</p>
            <button
              type="button"
              className="rounded-full bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
              onClick={() => void pedidosQuery.refetch()}
            >
              Reintentar
            </button>
          </div>
        </FeedbackAlert>
      ) : null}

      {hasProductosError ? (
        <FeedbackAlert variant="warning" title="Productos no disponibles temporalmente">
          El dashboard sigue mostrando pedidos reales, pero no pudo recuperar el resumen del catalogo del proveedor.
        </FeedbackAlert>
      ) : null}

      {proveedorId && !pedidosQuery.isLoading && !pedidosQuery.isError ? (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <ProveedorMetricCard
              label="Pendientes preparación"
              value={String(totalPedidos)}
              helper="Órdenes esperando acción."
              to="/proveedor/pedidos"
            />
            <ProveedorMetricCard
              label="Unidades a preparar"
              value={String(totalUnidades)}
              helper="Volumen total de artículos."
              tone="violet"
              to="/proveedor/pedidos"
            />
            <ProveedorMetricCard
              label="Con direccion"
              value={String(pedidosConDireccion)}
              helper="Logística de última milla lista."
              tone="emerald"
              to="/proveedor/pedidos?filtro=con-direccion"
            />
            <ProveedorMetricCard
              label="Pedido mas antiguo"
              value={formatFechaCorta(pedidoMasAntiguo?.fechaCreacionPedido ?? null)}
              helper="Referencia de tiempo crítico."
              tone="amber"
              to="/proveedor/pedidos?filtro=pendientes"
            />
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            <ProveedorMetricCard
              label="Oferta activa"
              value={String(productosActivos)}
              helper="Artículos visibles en tienda."
              tone="blue"
              to="/proveedor/productos?filtro=activos"
            />
            <ProveedorMetricCard
              label="Stock agotado"
              value={String(productosSinStock)}
              helper="Requieren actualización."
              tone="amber"
              to="/proveedor/productos?filtro=sin-stock"
            />
            <ProveedorMetricCard
              label="Pendientes normalización"
              value={String(productosPendientes)}
              helper="En proceso de revisión interna."
              tone="amber"
              to="/proveedor/productos?filtro=pendientes"
            />
            <ProveedorMetricCard
              label="Sin media"
              value={String(productosSinMedia)}
              helper="Contenido visual faltante."
              tone="violet"
              to="/proveedor/productos?filtro=sin-media"
            />
          </div>

          <section className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
            <section className="flex flex-col rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/30 p-5 px-6">
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Atención</p>
                  <h3 className="mt-1 text-lg font-bold text-slate-950">Prioridades</h3>
                </div>
                <Link to="/proveedor/pedidos" className="text-[10px] font-bold uppercase tracking-wider text-slate-500 transition hover:text-slate-950">
                  Gestionar
                </Link>
              </div>

              <div className="flex-1 space-y-2.5 p-5">
                {alertas.length > 0 ? (
                  alertas.map((alerta) => <AlertCard key={alerta.id} alert={alerta} />)
                ) : (
                  <EmptyPanel>No se registran prioridades críticas en este momento.</EmptyPanel>
                )}
              </div>
            </section>

            <section className="flex flex-col rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/30 p-5 px-6">
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">Operaciones</p>
                  <h3 className="mt-1 text-lg font-bold text-slate-950">Órdenes recientes</h3>
                </div>
                <Link
                  to="/proveedor/pedidos"
                  className="rounded-full border border-slate-200 px-3.5 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-500 transition hover:border-slate-300 hover:bg-slate-50"
                >
                  Ver todas
                </Link>
              </div>

              <div className="flex-1 space-y-2.5 p-5">
                {previewPedidos.length > 0 ? (
                  previewPedidos.map((item) => (
                    <ProveedorPedidoItem
                      key={item.itemId}
                      item={item}
                      onMarcarListo={() => undefined}
                      isMutating={false}
                      variant="compact"
                    />
                  ))
                ) : (
                  <EmptyPanel>No hay nuevas órdenes pendientes.</EmptyPanel>
                )}
              </div>
            </section>
          </section>

          {(productosCriticos.length > 0 || productosSinNormalizar.length > 0 || productosSinMediaPreview.length > 0) && productos.length > 0 ? (
            <section className="rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
              <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/30 p-5 px-6">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Productos</p>
                  <h3 className="mt-1 text-lg font-bold text-slate-950">Revisiones de catálogo</h3>
                </div>
                <Link to="/proveedor/productos" className="text-[10px] font-bold uppercase tracking-wider text-slate-500 transition hover:text-slate-950">
                  Ver catálogo
                </Link>
              </div>

              <div className="grid gap-4 xl:grid-cols-3 p-5">
                <ProductListCard
                  title="Sin stock"
                  items={productosCriticos}
                  emptyMessage="No hay productos sin stock."
                  tone="amber"
                  to="/proveedor/productos?filtro=sin-stock"
                />
                <ProductListCard
                  title="Pendientes MD"
                  items={productosSinNormalizar}
                  emptyMessage="No hay productos pendientes."
                  tone="blue"
                  to="/proveedor/productos?filtro=pendientes"
                />
                <ProductListCard
                  title="Sin media"
                  items={productosSinMediaPreview}
                  emptyMessage="No hay productos sin media."
                  tone="violet"
                  to="/proveedor/productos?filtro=sin-media"
                />
              </div>
            </section>
          ) : null}
        </>
      ) : null}
    </section>
  )
}

function buildAlertas({
  pedidos,
  productos,
}: {
  pedidos: PedidoProveedorResponse[]
  productos: ProveedorProductoResponse[]
}): AlertItem[] {
  const alertas: AlertItem[] = []
  const oldest = sortPedidosByOldest(pedidos)[0]
  const sinStock = productos.filter((item) => item.stockDisponible <= 0)
  const pendientes = productos.filter((item) => item.estadoNormalizacion === "PENDIENTE")
  const sinMedia = productos.filter((item) => item.cantidadMedia <= 0)

  if (oldest) {
    alertas.push({
      id: "pedidos-antiguos",
      tone: resolvePedidoPriority(oldest) === "red" ? "red" : "amber",
      title: "Pedido pendiente con mayor antiguedad",
      description: `${oldest.nombreProducto} lleva ${formatRelativeAge(oldest.fechaCreacionPedido)} pendiente.`,
      ctaLabel: "Ir a pedidos",
      to: "/proveedor/pedidos?filtro=pendientes",
    })
  }

  if (sinStock.length > 0) {
    alertas.push({
      id: "productos-sin-stock",
      tone: "amber",
      title: "Productos sin stock",
      description: `${sinStock.length} producto${sinStock.length === 1 ? "" : "s"} requieren revision.`,
      ctaLabel: "Revisar",
      to: "/proveedor/productos?filtro=sin-stock",
    })
  }

  if (pendientes.length > 0) {
    alertas.push({
      id: "productos-pendientes",
      tone: "blue",
      title: "Pendientes MD",
      description: `${pendientes.length} producto${pendientes.length === 1 ? "" : "s"} siguen sin asociacion canonica.`,
      ctaLabel: "Ver productos",
      to: "/proveedor/productos?filtro=pendientes",
    })
  }

  if (sinMedia.length > 0) {
    alertas.push({
      id: "productos-sin-media",
      tone: "blue",
      title: "Productos sin media",
      description: `${sinMedia.length} producto${sinMedia.length === 1 ? "" : "s"} pueden completarse con imagen o video.`,
      ctaLabel: "Gestionar",
      to: "/proveedor/productos?filtro=sin-media",
    })
  }

  return alertas.slice(0, 4)
}

function sortPedidosByOldest(pedidos: PedidoProveedorResponse[]) {
  return pedidos.slice().sort((a, b) => new Date(a.fechaCreacionPedido).getTime() - new Date(b.fechaCreacionPedido).getTime())
}

function sortPedidosByPriority(pedidos: PedidoProveedorResponse[]) {
  return pedidos
    .slice()
    .sort((a, b) => {
      const scoreDiff = getPedidoPriorityScore(b) - getPedidoPriorityScore(a)
      if (scoreDiff !== 0) return scoreDiff
      return new Date(a.fechaCreacionPedido).getTime() - new Date(b.fechaCreacionPedido).getTime()
    })
}

function getPedidoPriorityScore(item: PedidoProveedorResponse) {
  const ageHours = Math.max(0, Math.floor((Date.now() - new Date(item.fechaCreacionPedido).getTime()) / (1000 * 60 * 60)))
  const lacksAddress = item.direccionEnvio ? 0 : 2
  const oldEnough = ageHours >= 48 ? 3 : ageHours >= 24 ? 2 : 1
  return oldEnough + lacksAddress
}

function resolvePedidoPriority(item: PedidoProveedorResponse): "red" | "amber" | "blue" {
  const score = getPedidoPriorityScore(item)
  if (score >= 5) return "red"
  if (score >= 3) return "amber"
  return "blue"
}

function AlertCard({ alert }: { alert: AlertItem }) {
  const toneClassName =
    alert.tone === "red"
      ? "border-red-200 bg-red-50"
      : alert.tone === "amber"
        ? "border-amber-200 bg-amber-50"
        : "border-blue-200 bg-blue-50"

  const badgeClassName =
    alert.tone === "red"
      ? "bg-red-100 text-red-700"
      : alert.tone === "amber"
        ? "bg-amber-100 text-amber-700"
        : "bg-blue-100 text-blue-700"

  return (
    <article className={`flex items-start justify-between gap-4 rounded-xl border p-3.5 shadow-sm ${toneClassName}`}>
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <span className={`rounded px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider ${badgeClassName}`}>Requerido</span>
        </div>
        <h4 className="mt-2 text-sm font-bold leading-tight text-slate-950">{alert.title}</h4>
        <p className="mt-1 text-xs font-medium text-slate-600">{alert.description}</p>
      </div>
      <Link
        to={alert.to}
        className="shrink-0 rounded-lg bg-white px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-700 ring-1 ring-slate-200 transition hover:bg-slate-50"
      >
        {alert.ctaLabel}
      </Link>
    </article>
  )
}

function ProductListCard({
  title,
  items,
  emptyMessage,
  tone,
  to,
}: {
  title: string
  items: ProveedorProductoResponse[]
  emptyMessage: string
  tone: "amber" | "blue" | "violet"
  to: string
}) {
  return (
    <section className="flex flex-col rounded-3xl border border-slate-200 bg-white shadow-sm overflow-hidden">
      <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/30 p-4 px-5">
        <p className="text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{title}</p>
        <Link to={to} className="text-[10px] font-bold uppercase tracking-wider text-slate-500 transition hover:text-slate-950">
          Ir
        </Link>
      </div>
      <div className="flex-1 space-y-2.5 p-4">
        {items.length > 0 ? (
          items.map((item) => (
            <div key={item.id} className="rounded-xl border border-slate-100 bg-slate-50/50 p-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-bold text-slate-950">{item.nombre}</p>
                  <p className="mt-0.5 text-[10px] font-bold uppercase tracking-wider text-slate-400">SKU {item.sku}</p>
                </div>
                <PriorityBadge tone={tone}>
                  {tone === "amber" ? `${item.stockDisponible} un.` : tone === "blue" ? item.estadoNormalizacion : `${item.cantidadMedia} med.`}
                </PriorityBadge>
              </div>
            </div>
          ))
        ) : (
          <EmptyPanel>{emptyMessage}</EmptyPanel>
        )}
      </div>
    </section>
  )
}

function PriorityBadge({
  children,
  tone,
}: {
  children: string
  tone: "red" | "amber" | "blue" | "violet"
}) {
  const className =
    tone === "red"
      ? "bg-red-100 text-red-700"
      : tone === "amber"
        ? "bg-amber-100 text-amber-700"
        : tone === "violet"
          ? "bg-violet-100 text-violet-700"
          : "bg-blue-100 text-blue-700"

  return <span className={`rounded-lg px-2 py-0.5 text-[10px] font-bold uppercase tracking-tight ${className}`}>{children}</span>
}

function EmptyPanel({ children }: { children: string }) {
  return <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-5 text-sm leading-6 text-slate-600">{children}</div>
}
