import axios from "axios"
import { useEffect, useMemo, useRef } from "react"
import { Link, Navigate } from "react-router-dom"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { useTenant } from "../../tenant/useTenant"
import StorefrontErrorState from "../components/StorefrontErrorState"
import { usePedidoTracking } from "../hooks/usePedidoTracking"
import { findLatestCheckoutStateWithTracking, getStorefrontCheckoutKey } from "../utils/storefrontCheckoutState"

const AUTO_REFRESH_DELAY_MS = 2500

export default function StorefrontPagoRetornoPage() {
  const tenant = useTenant()
  const accentColor = tenant.branding?.colorPrimario || "#0f172a"
  const hasScheduledRefresh = useRef(false)
  const refreshTimeoutRef = useRef<number | null>(null)
  const storefrontKey = tenant.status === "resolved" ? getStorefrontCheckoutKey(tenant.tipoStorefront, tenant.revendedorId, tenant.proveedorId) : null

  const checkoutState = useMemo(() => findLatestCheckoutStateWithTracking(storefrontKey), [storefrontKey])
  const trackingToken = checkoutState?.pedidoCreado?.trackingToken || null
  const tracking = usePedidoTracking(trackingToken || undefined, tenant.host)

  useEffect(() => {
    if (!trackingToken || !tracking.data || hasScheduledRefresh.current) {
      return
    }

    if (isPagoPendiente(tracking.data.estadoPago)) {
      hasScheduledRefresh.current = true
      refreshTimeoutRef.current = window.setTimeout(() => {
        void tracking.refetch()
      }, AUTO_REFRESH_DELAY_MS)
    }
  }, [tracking, tracking.data, trackingToken])

  useEffect(() => {
    return () => {
      if (refreshTimeoutRef.current !== null) {
        window.clearTimeout(refreshTimeoutRef.current)
      }
    }
  }, [])

  if (!trackingToken) {
    return (
      <StorefrontErrorState
        title="No pudimos reanudar tu pedido"
        message="Volviste desde el pago, pero no encontramos tu pedido en este navegador. Si tienes tu enlace de seguimiento, usalo directamente."
      />
    )
  }

  if (!tenant.host) {
    return (
      <StorefrontErrorState
        title="Tienda no resuelta"
        message="No pudimos determinar la tienda actual para validar el seguimiento del pedido."
      />
    )
  }

  if (tracking.isLoading) {
    return <PagoRetornoSkeleton />
  }

  if (tracking.isError || !tracking.data) {
    const errorState = getTrackingReturnErrorState(tracking.error)
    return (
      <StorefrontErrorState
        title={errorState.title}
        message={errorState.message}
        actionLabel={errorState.allowRetry ? "Reintentar verificacion" : undefined}
        onAction={errorState.allowRetry ? () => void tracking.refetch() : undefined}
      />
    )
  }

  const trackingData = tracking.data
  if (tenant.storefront?.modoCatalogo) return <Navigate replace to={`/pedido/${encodeURIComponent(trackingToken)}`} />
  const estadoPago = trackingData.estadoPago?.trim().toUpperCase() || "DESCONOCIDO"
  const pagoConfirmado = estadoPago === "CONFIRMADO" &&
    ["PAGADO", "ENVIADO", "ENTREGADO"].includes(trackingData.estadoPedido?.trim().toUpperCase() || "")
  const pagoFallido = estadoPago === "RECHAZADO" || estadoPago === "CANCELADO"
  const pagoReembolsado = estadoPago === "REEMBOLSADO"
  const tone = pagoConfirmado ? "success" : pagoFallido || pagoReembolsado ? "failure" : "pending"
  const totalFormateado = new Intl.NumberFormat("es-AR", {
    style: "currency",
    currency: "ARS",
    maximumFractionDigits: 2,
  }).format(trackingData.totalFinal)

  return (
    <section className="space-y-6">
      <header
        className={`rounded-3xl border bg-white p-6 shadow-sm sm:p-8 ${
          tone === "success" ? "border-emerald-200" : tone === "failure" ? "border-rose-200" : "border-slate-200"
        }`}
        style={{
          backgroundImage:
            tone === "success"
              ? "linear-gradient(145deg, #dcfce7 0%, #ffffff 45%, #f0fdf4 100%)"
              : tone === "failure"
                ? "linear-gradient(145deg, #ffe4e6 0%, #ffffff 45%, #fff1f2 100%)"
                : `linear-gradient(145deg, ${accentColor}10 0%, #ffffff 45%, #f8fafc 100%)`,
        }}
      >
        <p className="text-sm font-semibold uppercase tracking-[0.24em] text-slate-500">Compra</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-950">
          {pagoConfirmado ? "Pago confirmado" : pagoReembolsado ? "Pago reembolsado" : pagoFallido ? "Pago no completado" : "Estamos verificando tu pago"}
        </h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
          {pagoConfirmado
            ? "Tu compra ya quedo registrada y ya puedes seguir el pedido."
            : pagoReembolsado
              ? "La tienda registra un reembolso para este pedido. Consulta el seguimiento para ver su estado actual."
              : pagoFallido
                ? "El pago no se completó. Consulta el estado actual antes de intentar otra acción."
                : "Todavía no hay una confirmación final del pago. El estado puede actualizarse con demora."}
        </p>
      </header>

      <FeedbackAlert
        variant={pagoConfirmado ? "success" : pagoFallido || pagoReembolsado ? "error" : "info"}
        title={pagoConfirmado ? "Pago confirmado" : pagoReembolsado ? "Pago reembolsado" : pagoFallido ? "Pago no completado" : "Verificación en curso"}
      >
        <p>
          Pedido <span className="font-semibold">{trackingData.pedidoId}</span> con pago en{" "}
          <span className="font-semibold">{trackingData.estadoPago}</span>.
        </p>
        <p className="mt-2">
          {pagoConfirmado
            ? "La compra quedo cerrada correctamente."
            : pagoReembolsado
              ? "El seguimiento conserva la referencia de este pedido."
              : pagoFallido
                ? "Conservamos el enlace para consultar el pedido."
                : "Mercado Pago puede demorar unos instantes en acreditar el pago."}
        </p>
      </FeedbackAlert>

      <div className="space-y-4">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          {pagoConfirmado ? (
            <div className="space-y-5">
              <div className="grid gap-4 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-4 sm:grid-cols-[96px_1fr]">
                <div className="h-24 w-24 overflow-hidden rounded-[1.25rem] border border-slate-200 bg-white">
                  {trackingData.items[0]?.imagenPrincipal ? (
                    <img
                      src={trackingData.items[0].imagenPrincipal}
                      alt={trackingData.items[0].nombreProducto}
                      className="h-full w-full object-cover"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center px-3 text-center text-xs text-slate-500">
                      Sin imagen
                    </div>
                  )}
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Producto</p>
                  <h2 className="mt-1 text-xl font-semibold text-slate-950">
                    {trackingData.items[0]?.nombreProducto || "Tu compra"}
                  </h2>
                  <p className="mt-2 text-sm text-slate-600">
                    Pedido <span className="font-semibold">{trackingData.pedidoId}</span>
                  </p>
                  <p className="mt-1 text-sm text-slate-600">
                    Total <span className="font-semibold">{totalFormateado}</span>
                  </p>
                </div>
              </div>

              <div className="rounded-[1.5rem] border border-emerald-200 bg-emerald-50 p-4 text-sm leading-6 text-emerald-900">
                A partir de ahora vas a poder rastrear tu pedido desde el link que te mandamos al correo, o tambien desde{" "}
                <Link to={`/pedido/${trackingToken}`} className="font-semibold underline">
                  aqui
                </Link>
                .
              </div>
            </div>
          ) : pagoFallido || pagoReembolsado ? (
            <div className="space-y-4">
              <div className="rounded-[1.5rem] border border-rose-200 bg-rose-50 p-4 text-sm leading-6 text-rose-900">
                {pagoReembolsado ? "El pago figura como reembolsado." : "El pago no se completó. Tu pedido sigue disponible para consultar su estado."}
              </div>
              <div className="grid gap-3 text-sm text-slate-700">
                <ResumenRow label="Pedido" value={trackingData.pedidoId} />
                <ResumenRow label="Estado pedido" value={trackingData.estadoPedido} />
                <ResumenRow label="Estado pago" value={trackingData.estadoPago} />
                <ResumenRow label="Metodo" value={humanizePaymentMethod(trackingData.metodoPago)} />
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <h2 className="text-xl font-semibold text-slate-950">Estado actual del pedido</h2>
              <div className="grid gap-3 text-sm text-slate-700">
                <ResumenRow label="Pedido" value={trackingData.pedidoId} />
                <ResumenRow label="Estado pedido" value={trackingData.estadoPedido} />
                <ResumenRow label="Estado pago" value={trackingData.estadoPago} />
                <ResumenRow label="Metodo" value={humanizePaymentMethod(trackingData.metodoPago)} />
                <ResumenRow label="Codigo de seguimiento" value={trackingToken} />
              </div>
            </div>
          )}
        </section>

        <div className="space-y-4">
          {!pagoConfirmado && !pagoFallido && !pagoReembolsado ? (
            <div className="rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
              Aún no hay una confirmación de pago. Consultá el seguimiento para ver novedades.
            </div>
          ) : null}

          <div className="flex flex-wrap gap-3">
            <Link
              to={`/pedido/${trackingToken}`}
              className="inline-flex rounded-full px-4 py-2 text-sm font-semibold text-white"
              style={{ backgroundColor: accentColor }}
            >
              Ver seguimiento del pedido
            </Link>
            <Link
              to="/catalogo"
              className="inline-flex rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700"
            >
              Volver a productos
            </Link>
          </div>
        </div>
      </div>
    </section>
  )
}

function PagoRetornoSkeleton() {
  return (
    <section className="space-y-6">
      <div className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="h-4 w-36 animate-pulse rounded bg-slate-200" />
        <div className="mt-4 h-10 w-2/3 animate-pulse rounded bg-slate-200" />
        <div className="mt-3 h-5 w-full animate-pulse rounded bg-slate-200" />
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="h-72 animate-pulse rounded-3xl bg-slate-200" />
        <div className="h-72 animate-pulse rounded-3xl bg-slate-200" />
      </div>
    </section>
  )
}

function ResumenRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl bg-slate-50 px-4 py-3">
      <span className="font-medium text-slate-600">{label}</span>
      <span className="break-all text-right font-semibold">{value}</span>
    </div>
  )
}

function isPagoPendiente(estadoPago: string) {
  return estadoPago.trim().toUpperCase() === "PENDIENTE"
}

function humanizePaymentMethod(value: string) {
  return value.trim().toUpperCase() === "PSP" ? "Mercado Pago" : value
}

function getTrackingReturnErrorState(error: unknown) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 404) {
      return {
        title: "Pedido no encontrado",
        message: "No encontramos el pedido para el enlace guardado en este navegador. Si recibiste otro enlace de seguimiento, usalo directamente.",
        allowRetry: false,
      }
    }

    return {
      title: "No se pudo verificar el pedido",
      message: "Ocurrio un problema temporal consultando el seguimiento. Puedes reintentar en unos segundos.",
      allowRetry: true,
    }
  }

  return {
    title: "No se pudo verificar el pedido",
    message: "Ocurrio un error inesperado al consultar el estado real del pedido.",
    allowRetry: true,
  }
}
