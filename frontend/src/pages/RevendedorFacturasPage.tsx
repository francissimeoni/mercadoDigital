import { useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { usePedidos } from "../hooks/usePedidos"
import type { PedidoRevendedorResponse } from "../types/pedido"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

type EstadoFiltro = "TODOS" | "ABIERTOS" | "CERRADOS"

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

function normalizeText(value: string | null | undefined) {
  return (value ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim()
}

function isPedidoCerrado(pedido: PedidoRevendedorResponse) {
  const estado = normalizeText(pedido.estadoPedido)
  const envio = normalizeText(pedido.estadoEnvio)
  return (
    estado.includes("entregado") ||
    estado.includes("cancelado") ||
    estado.includes("cerrado") ||
    envio.includes("entregado")
  )
}

function getFacturaId(pedidoId: string) {
  return `FAC-${pedidoId.slice(0, 8).toUpperCase()}`
}

export default function RevendedorFacturasPage() {
  const [searchParams] = useSearchParams()
  const revendedorId =
    searchParams.get("revendedorId") || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const { data, isLoading, isError, refetch } = usePedidos(revendedorId)
  const [search, setSearch] = useState("")
  const [filtro, setFiltro] = useState<EstadoFiltro>("TODOS")

  const pedidos = data ?? []
  const filtered = useMemo(() => {
    const normalizedSearch = normalizeText(search)
    return pedidos.filter((pedido) => {
      const statusMatches =
        filtro === "TODOS" ? true : filtro === "CERRADOS" ? isPedidoCerrado(pedido) : !isPedidoCerrado(pedido)
      const haystack = normalizeText(`${pedido.pedidoId} ${getFacturaId(pedido.pedidoId)} ${pedido.estadoPedido}`)
      const queryMatches = !normalizedSearch || haystack.includes(normalizedSearch)
      return statusMatches && queryMatches
    })
  }, [filtro, pedidos, search])

  const resumen = useMemo(() => {
    const totalMovimientos = pedidos.length
    const totalFacturado = pedidos.reduce((sum, pedido) => sum + pedido.totalFinal, 0)
    const abiertos = pedidos.filter((pedido) => !isPedidoCerrado(pedido)).length
    const cerrados = Math.max(totalMovimientos - abiertos, 0)

    return {
      totalMovimientos,
      totalFacturado,
      abiertos,
      cerrados,
    }
  }, [pedidos])

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Revendedor</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Facturas y movimientos</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Resumen comercial construido sobre pedidos reales del revendedor para seguimiento de ventas, estado
              operativo y volumen facturado.
            </p>
          </div>
          <span className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-emerald-700">
            conectado
          </span>
        </div>
      </header>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="Movimientos" value={String(resumen.totalMovimientos)} detail="Pedidos registrados." />
        <SummaryCard label="Facturado total" value={currencyFormatter.format(resumen.totalFacturado)} detail="Suma de total final." />
        <SummaryCard label="Abiertos" value={String(resumen.abiertos)} detail="Pendientes de cierre operativo." />
        <SummaryCard label="Cerrados" value={String(resumen.cerrados)} detail="Entregados o cerrados." />
      </div>

      {!revendedorId ? (
        <FeedbackAlert variant="warning" title="Falta configuracion del revendedor">
          Esta vista necesita `revendedorId` para consultar la base de movimientos.
        </FeedbackAlert>
      ) : null}

      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <label className="block w-full lg:max-w-md">
            <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Buscar</span>
            <input
              type="search"
              className="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
              placeholder="Pedido o factura"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
              }}
            />
          </label>
          <div className="flex flex-wrap gap-2">
            {(["TODOS", "ABIERTOS", "CERRADOS"] as const).map((option) => (
              <button
                key={option}
                type="button"
                className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition ${
                  filtro === option
                    ? "border-slate-900 bg-slate-900 text-white"
                    : "border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:text-slate-950"
                }`}
                onClick={() => {
                  setFiltro(option)
                }}
              >
                {option}
              </button>
            ))}
          </div>
        </div>

        {revendedorId && isLoading ? (
          <div className="mt-6 space-y-3">
            {Array.from({ length: 5 }).map((_, index) => (
              <div key={index} className="h-16 animate-pulse rounded-2xl bg-slate-100" />
            ))}
          </div>
        ) : null}

        {revendedorId && isError ? (
          <div className="mt-6">
            <FeedbackAlert variant="error" title="No se pudo cargar la tabla de movimientos">
              <button
                type="button"
                className="mt-3 rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
                onClick={() => {
                  void refetch()
                }}
              >
                Reintentar
              </button>
            </FeedbackAlert>
          </div>
        ) : null}

        {revendedorId && !isLoading && !isError ? (
          filtered.length > 0 ? (
            <div className="mt-6 overflow-x-auto">
              <table className="min-w-full border-separate border-spacing-y-2">
                <thead>
                  <tr className="text-left text-xs uppercase tracking-[0.2em] text-slate-500">
                    <th className="px-3 py-2">Factura</th>
                    <th className="px-3 py-2">Pedido</th>
                    <th className="px-3 py-2">Fecha</th>
                    <th className="px-3 py-2">Estado</th>
                    <th className="px-3 py-2">Total</th>
                    <th className="px-3 py-2">Accion</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((pedido) => (
                    <tr key={pedido.pedidoId} className="rounded-2xl border border-slate-200 bg-slate-50 text-sm text-slate-800">
                      <td className="rounded-l-2xl px-3 py-3 font-semibold text-slate-900">{getFacturaId(pedido.pedidoId)}</td>
                      <td className="px-3 py-3">{pedido.pedidoId}</td>
                      <td className="px-3 py-3">{formatFecha(pedido.fechaCreacionPedido)}</td>
                      <td className="px-3 py-3">{pedido.estadoPedido}</td>
                      <td className="px-3 py-3 font-semibold text-slate-900">{currencyFormatter.format(pedido.totalFinal)}</td>
                      <td className="rounded-r-2xl px-3 py-3">
                        <Link
                          to={`/pedidos/${pedido.pedidoId}`}
                          className="inline-block rounded-md bg-slate-900 px-3 py-2 text-xs font-semibold text-white hover:bg-slate-700"
                        >
                          Ver detalle
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm text-slate-600">
              No hay movimientos para los filtros actuales.
            </div>
          )
        ) : null}
      </section>
    </section>
  )
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-4 text-2xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}
