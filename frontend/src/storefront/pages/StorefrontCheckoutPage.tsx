import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import StorefrontCheckoutResult from '../components/StorefrontCheckoutResult'
import axios from "axios"
import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from "react"
import { Link, Navigate, useParams, useSearchParams } from "react-router-dom"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import ProductoDetalleSkeleton from "../../components/catalogo/ProductoDetalleSkeleton"
import { useTenant } from "../../tenant/useTenant"
import StorefrontErrorState from "../components/StorefrontErrorState"
import { useStorefrontProductoDetalle } from "../hooks/useStorefrontCatalogo"
import { useCrearPedidoDirecto } from "../hooks/useCrearPedidoDirecto"
import { usePrepararPagoPsp } from "../hooks/usePrepararPagoPsp"
import { clearCheckoutState, getCheckoutStorageKey, readCheckoutState, writeCheckoutState } from "../utils/storefrontCheckoutState"
import type {
  StorefrontCheckoutFormState,
  StorefrontPedidoDirectoResponse,
  StorefrontPrepararPagoPspResponse,
} from "../types/storefrontPedido"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

const initialFormState: StorefrontCheckoutFormState = {
  nombreApellido: "",
  email: "",
  telefonoWhatsapp: "",
  provincia: "",
  localidad: "",
  codigoPostal: "",
  calle: "",
  numero: "",
  pisoDepto: "",
  observaciones: "",
  cantidad: 1,
  seguroTransporte: false,
  codigoDescuento: "",
  aceptacionTerminos: false,
}

export default function StorefrontCheckoutPage() {
  const { productoCanonicoId } = useParams()
  const [searchParams] = useSearchParams()
  const tenant = useTenant()
  const accentColor = tenant.branding?.colorPrimario || "#0f172a"
  const catalogoTarget =
    tenant.tipoStorefront === "PROVEEDOR" && tenant.proveedorId
      ? ({ tipo: "PROVEEDOR", proveedorId: tenant.proveedorId, host: tenant.host } as const)
      : tenant.revendedorId
        ? ({ tipo: "REVENDEDOR", revendedorId: tenant.revendedorId, host: tenant.host } as const)
        : null
  const detalle = useStorefrontProductoDetalle(productoCanonicoId, catalogoTarget)
  const compraAnteriorHabilitada = detalle.isSuccess && !detalle.data?.modoCatalogo && !tenant.storefront?.modoCatalogo
  const crearPedido = useCrearPedidoDirecto()
  const prepararPago = usePrepararPagoPsp()
  const prepararPagoMutate = prepararPago.mutate
  const seguroParam = searchParams.get("seguro") === "1"
  const [form, setForm] = useState<StorefrontCheckoutFormState>({ ...initialFormState, seguroTransporte: seguroParam })
  const [validationError, setValidationError] = useState<string | null>(null)
  const [pedidoCreado, setPedidoCreado] = useState<StorefrontPedidoDirectoResponse | null>(null)
  const [pagoPreparado, setPagoPreparado] = useState<StorefrontPrepararPagoPspResponse | null>(null)
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1)
  const [showSeguroInfo, setShowSeguroInfo] = useState(false)
  const [metodoPago, setMetodoPago] = useState<"PSP" | "TRANSFERENCIA" | null>(null)
  const mercadoPagoHabilitado = MERCADO_PAGO_DISPONIBLE && (tenant.tipoStorefront === "PROVEEDOR" || !!tenant.storefront?.mercadoPagoHabilitado)
  const transferenciaHabilitada = tenant.tipoStorefront === "REVENDEDOR" && !!tenant.branding?.transferenciaHabilitada
  const recargoPorcentaje = tenant.tipoStorefront === "REVENDEDOR" ? Number(tenant.storefront?.mercadoPagoRecargoPorcentaje || 0) : 0
  const redirectTimeoutRef = useRef<number | null>(null)
  const revendedorId = tenant.revendedorId
  const proveedorId = tenant.proveedorId
  const asesorVentaId = normalizeUuid(searchParams.get("asesorId"))
  const storefrontKey =
    tenant.tipoStorefront === "PROVEEDOR"
      ? proveedorId
        ? `PROVEEDOR:${proveedorId}`
        : null
      : revendedorId
        ? `REVENDEDOR:${revendedorId}`
        : null
  const storageKey = storefrontKey && productoCanonicoId ? getCheckoutStorageKey(storefrontKey, productoCanonicoId) : null

  useEffect(() => {
    if (!storageKey || !compraAnteriorHabilitada) {
      return
    }

    const restoredState = readCheckoutState(storageKey)

    if (!restoredState) {
      return
    }

    setPedidoCreado(restoredState.pedidoCreado)
    setPagoPreparado(restoredState.pagoPreparado)
  }, [storageKey, compraAnteriorHabilitada])

  useEffect(() => {
    if (!crearPedido.data || !compraAnteriorHabilitada) {
      return
    }

    setPedidoCreado(crearPedido.data)
    setCurrentStep(3)
    if (MERCADO_PAGO_DISPONIBLE && crearPedido.data.metodoPago !== "TRANSFERENCIA" && tenant.host && crearPedido.data.trackingToken) {
      prepararPagoMutate({ pedidoId: crearPedido.data.pedidoId, trackingToken: crearPedido.data.trackingToken, storefrontHost: tenant.host })
    }
  }, [crearPedido.data, compraAnteriorHabilitada, prepararPagoMutate, tenant.host])

  useEffect(() => {
    if (!MERCADO_PAGO_DISPONIBLE || !prepararPago.data || !compraAnteriorHabilitada) {
      return
    }

    setPagoPreparado(prepararPago.data)

    const checkoutUrl = getCheckoutUrl(prepararPago.data)
    if (checkoutUrl) {
      redirectTimeoutRef.current = window.setTimeout(() => {
        window.location.assign(checkoutUrl)
      }, 1200)
    }
  }, [prepararPago.data, compraAnteriorHabilitada])

  useEffect(() => {
    if (!prepararPago.error) {
      return
    }

    setPagoPreparado(null)
    // El pedido ya existe: conservar su referencia evita ofrecer crear otro al fallar PSP.
  }, [prepararPago.error, storageKey])

  useEffect(() => {
    return () => {
      if (redirectTimeoutRef.current !== null) {
        window.clearTimeout(redirectTimeoutRef.current)
      }
    }
  }, [])

  useEffect(() => {
    if (!storageKey || !compraAnteriorHabilitada || (!pedidoCreado && !pagoPreparado)) {
      return
    }

    writeCheckoutState(storageKey, {
      pedidoCreado,
      pagoPreparado,
    })
  }, [pagoPreparado, pedidoCreado, storageKey, compraAnteriorHabilitada])

  const handleTextChange =
    (field: keyof Omit<StorefrontCheckoutFormState, "cantidad" | "seguroTransporte" | "codigoDescuento" | "aceptacionTerminos">) =>
    (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }))
    }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setValidationError(null)

    if (!compraAnteriorHabilitada || pedidoCreado) return

    if (!productoCanonicoId) {
      setValidationError("No se pudo resolver la tienda o el producto para crear el pedido.")
      return
    }

    const nextError = validateForm(form)
    if (nextError) {
      setValidationError(nextError)
      return
    }

    if (!metodoPago) {
      setValidationError("Elegí cómo querés pagar antes de confirmar la compra.")
      return
    }

    setPedidoCreado(null)
    setPagoPreparado(null)
    if (storageKey) {
      clearCheckoutState(storageKey)
    }

    crearPedido.mutate({
      revendedorId: tenant.tipoStorefront === "REVENDEDOR" ? revendedorId || undefined : undefined,
      proveedorId: tenant.tipoStorefront === "PROVEEDOR" ? proveedorId || undefined : undefined,
      revendedorProductoId: tenant.tipoStorefront === "REVENDEDOR" ? productoCanonicoId : undefined,
      productoId: tenant.tipoStorefront === "PROVEEDOR" ? detalle.data?.productoId : undefined,
      nombreApellido: form.nombreApellido.trim(),
      email: form.email.trim(),
      telefonoWhatsapp: form.telefonoWhatsapp.trim(),
      provincia: form.provincia.trim(),
      localidad: form.localidad.trim(),
      codigoPostal: form.codigoPostal.trim(),
      calle: form.calle.trim(),
      numero: form.numero.trim(),
      pisoDepto: form.pisoDepto.trim() || undefined,
      observaciones: form.observaciones.trim() || undefined,
      cantidad: form.cantidad,
      seguroTransporte: form.seguroTransporte,
      codigoDescuento: form.codigoDescuento.trim() || undefined,
      asesorVentaId: asesorVentaId || undefined,
      aceptacionTerminos: form.aceptacionTerminos,
      metodoPago,
    })
  }

  const requestError = getRequestErrorMessage(crearPedido.error)
  const prepararPagoError = getRequestErrorMessage(prepararPago.error)
  const checkoutUrl = MERCADO_PAGO_DISPONIBLE && pagoPreparado ? getCheckoutUrl(pagoPreparado) : null

  const handleNextStep = () => {
    const nextError = validateBuyerStep(form)
    if (nextError) {
      setValidationError(nextError)
      return
    }

    setValidationError(null)
    setCurrentStep(2)
  }

  if (!productoCanonicoId) {
    return (
      <StorefrontErrorState
        title="Formas de pago no disponibles"
        message="No se encontro un identificador de producto valido para iniciar la compra."
      />
    )
  }

  if (!catalogoTarget) {
    return (
      <StorefrontErrorState
        title="Tienda no disponible"
        message="No se pudo resolver la tienda actual para crear el pedido."
      />
    )
  }

  if (detalle.isLoading) {
    return <ProductoDetalleSkeleton />
  }

  if (detalle.isError || !detalle.data) {
    return (
      <StorefrontErrorState
        title="No se pudo iniciar formas de pago"
        message="Reintenta desde el detalle del producto."
        actionLabel="Reintentar"
        onAction={() => void detalle.refetch()}
      />
    )
  }

  const detalleData = detalle.data
  if (detalleData.modoCatalogo) return <Navigate replace to={`/producto/${productoCanonicoId}?${searchParams.toString()}`} />
  if (detalleData.seguroTransporteEstimado === null || detalleData.totalConSeguroEstimado === null) return <StorefrontErrorState title="Compra no disponible" message="No se pudieron obtener los importes de la compra." />
  const seguroEstimado = detalleData.seguroTransporteEstimado * form.cantidad
  const totalBase = seguroEstimado > 0 && form.seguroTransporte
    ? detalleData.totalConSeguroEstimado * form.cantidad
    : detalleData.mejorPrecioFinalEstimado * form.cantidad
  const recargoMercadoPago = metodoPago === "PSP" && mercadoPagoHabilitado
    ? Math.round((totalBase / form.cantidad * recargoPorcentaje / 100 + Number.EPSILON) * 100) / 100 * form.cantidad : 0
  const totalEstimado = Math.round((totalBase + recargoMercadoPago + Number.EPSILON) * 100) / 100
  const seguroResumen = form.seguroTransporte ? currencyFormatter.format(seguroEstimado) : "No agregado"

  if (pedidoCreado) return <StorefrontCheckoutResult order={pedidoCreado} email={form.email} productName={detalleData.nombrePublico} image={detalleData.imagenPrincipal} quantity={form.cantidad} checkoutUrl={checkoutUrl} error={prepararPagoError} />

  if (!mercadoPagoHabilitado && !transferenciaHabilitada) return <section className="mx-auto max-w-2xl rounded-3xl border border-slate-200 bg-white p-8 text-center">
    <h1 className="text-2xl font-bold">Coordiná tu compra con la tienda</h1>
    <p className="mt-4 text-slate-600">Esta tienda no recibe pagos por la plataforma. Volvé a la ficha del producto y usá el botón de contacto para consultar cómo comprar.</p>
    <Link to={`/producto/${encodeURIComponent(productoCanonicoId)}`} className="mt-6 inline-flex rounded-full bg-slate-950 px-6 py-3 font-semibold text-white">Volver al producto</Link>
  </section>

  const subtotalEstimado = detalleData.mejorPrecioFinalEstimado * form.cantidad

  return (
    <section className="space-y-6">
      <div
        className="rounded-[1.6rem] border border-slate-200 bg-white p-4 shadow-sm sm:rounded-[2rem] sm:p-8"
        style={{ backgroundImage: `linear-gradient(145deg, ${accentColor}10 0%, #ffffff 42%, #f8fafc 100%)` }}
      >
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-semibold leading-tight text-slate-950 sm:text-3xl">Finalizar compra</h1>
          </div>
          <Link to={`/producto/${productoCanonicoId}`} className="text-sm font-semibold" style={{ color: accentColor }}>
            {"<-"} Volver al detalle
          </Link>
        </div>
      </div>

      <section className="grid gap-3 sm:grid-cols-2">
        <CheckoutSpotlightCard label="Producto" value={detalleData.nombrePublico} />
        <CheckoutSpotlightCard label="Total estimado actual" value={currencyFormatter.format(totalEstimado)} tone="highlight" />
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="space-y-4">
          {currentStep === 1 ? (
            <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
              <div>
                <h2 className="text-xl font-semibold text-slate-950">Datos de envio</h2>
              </div>
              <div className="mt-5 grid gap-4 sm:grid-cols-2">
                <label className="space-y-2 sm:col-span-2">
                  <span className="text-sm font-medium text-slate-700">Nombre y apellido</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.nombreApellido}
                    onChange={handleTextChange("nombreApellido")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Email</span>
                  <input
                    type="email"
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.email}
                    onChange={handleTextChange("email")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Telefono / WhatsApp</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.telefonoWhatsapp}
                    onChange={handleTextChange("telefonoWhatsapp")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Provincia</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.provincia}
                    onChange={handleTextChange("provincia")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Localidad</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.localidad}
                    onChange={handleTextChange("localidad")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Codigo postal</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.codigoPostal}
                    onChange={handleTextChange("codigoPostal")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Calle</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.calle}
                    onChange={handleTextChange("calle")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Numero</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.numero}
                    onChange={handleTextChange("numero")}
                  />
                </label>
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Piso / Depto</span>
                  <input
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.pisoDepto}
                    onChange={handleTextChange("pisoDepto")}
                  />
                </label>
                <label className="space-y-2 sm:col-span-2">
                  <span className="text-sm font-medium text-slate-700">Observaciones</span>
                  <textarea
                    rows={3}
                    className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                    value={form.observaciones}
                    onChange={handleTextChange("observaciones")}
                  />
                </label>
              </div>

              {validationError ? (
                <div className="mt-5">
                  <FeedbackAlert variant="warning" title="Revisa tus datos">
                    {validationError}
                  </FeedbackAlert>
                </div>
              ) : null}

              <div className="mt-5 flex">
                <button
                  type="button"
                  className="inline-flex min-h-[44px] w-full items-center justify-center rounded-full px-5 py-3 text-sm font-semibold text-white sm:w-auto"
                  style={{ backgroundColor: accentColor }}
                  onClick={handleNextStep}
                >
                  Continuar
                </button>
              </div>
            </section>
          ) : null}

          {currentStep === 2 ? (
            <form onSubmit={handleSubmit} className="space-y-5 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
              <div>
                <h2 className="text-xl font-semibold text-slate-950">Resumen de compra</h2>
              </div>

              <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50 p-4">
                <div className="grid gap-4 sm:grid-cols-[88px_1fr_auto] sm:items-start">
                  <div className="h-24 w-24 overflow-hidden rounded-[1.25rem] border border-slate-200 bg-white">
                    {detalleData.imagenPrincipal ? (
                      <img
                        src={detalleData.imagenPrincipal}
                        alt={detalleData.nombrePublico}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full items-center justify-center px-3 text-center text-xs text-slate-500">
                        Sin imagen
                      </div>
                    )}
                  </div>
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Producto</p>
                    <h3 className="mt-1 text-lg font-semibold text-slate-950">{detalleData.nombrePublico}</h3>
                    <p className="mt-2 text-sm text-slate-600">Cantidad: {form.cantidad}</p>
                    <p className="mt-1 text-sm text-slate-600">Entrega estimada: {detalleData.demoraEstimadaDias} dias</p>
                  </div>
                  <p className="text-right text-2xl font-bold" style={{ color: accentColor }}>
                    {currencyFormatter.format(totalEstimado)}
                  </p>
                </div>
              </div>

              <div className="rounded-[1.5rem] border border-slate-200 bg-white p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-sm font-semibold text-slate-950">Seguro de transporte</p>
                    <p className="mt-1 text-sm text-slate-600">{currencyFormatter.format(seguroEstimado)}</p>
                  </div>
                  <label className="inline-flex items-center gap-2 text-sm font-medium text-slate-700">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={form.seguroTransporte}
                      onChange={(event) =>
                        setForm((current) => ({
                          ...current,
                          seguroTransporte: event.target.checked,
                        }))
                      }
                    />
                    Agregar
                  </label>
                </div>
              <button
                  type="button"
                  className="mt-3 text-sm font-semibold"
                  style={{ color: accentColor }}
                  onClick={() => setShowSeguroInfo((current) => !current)}
                >
                  + info
                </button>
                {showSeguroInfo ? (
                  <div className="mt-2 rounded-[1rem] border border-amber-200 bg-amber-50 px-3 py-2.5 text-xs leading-5 text-amber-900">
                    Cubre perdida o rotura del producto durante el transporte.
                  </div>
                ) : null}
              </div>

              <label className="space-y-2">
                <span className="text-sm font-medium text-slate-700">Cupon de descuento</span>
                <input
                  className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm uppercase outline-none transition focus:border-slate-500"
                  value={form.codigoDescuento}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      codigoDescuento: event.target.value.toUpperCase(),
                    }))
                  }
                  placeholder="Opcional"
                />
              </label>

              <div className="space-y-3">
                <p className="text-sm font-semibold text-slate-950">Forma de pago</p>
                <div className={`grid gap-3 ${tenant.tipoStorefront === "REVENDEDOR" && tenant.branding?.transferenciaHabilitada ? "sm:grid-cols-2" : ""}`}>
                  {mercadoPagoHabilitado ? <button type="button" onClick={() => setMetodoPago("PSP")} className={`rounded-2xl border p-4 text-left ${metodoPago === "PSP" ? "border-sky-400 bg-sky-50 ring-2 ring-sky-100" : "border-slate-200 bg-white"}`}><span className="block font-semibold text-slate-950">Mercado Pago</span><span className="mt-1 block text-sm text-slate-600">Tarjetas y cuotas disponibles.</span></button> : null}
                  {transferenciaHabilitada ? <button type="button" onClick={() => setMetodoPago("TRANSFERENCIA")} className={`rounded-2xl border p-4 text-left ${metodoPago === "TRANSFERENCIA" ? "border-emerald-400 bg-emerald-50 ring-2 ring-emerald-100" : "border-slate-200 bg-white"}`}><span className="block font-semibold text-slate-950">Transferencia bancaria</span><span className="mt-1 block text-sm text-slate-600">Datos enviados por email.</span></button> : null}
                </div>
              </div>

              <label className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <input
                  type="checkbox"
                  className="mt-1 h-4 w-4 rounded border-slate-300"
                  checked={form.aceptacionTerminos}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      aceptacionTerminos: event.target.checked,
                    }))
                  }
                />
                  <span className="text-sm leading-6 text-slate-700">
                  Acepto los terminos y condiciones.
                  </span>
                </label>

              {validationError ? (
                <FeedbackAlert variant="warning" title="Revisa el formulario">
                  {validationError}
                </FeedbackAlert>
              ) : null}

              {requestError ? (
                <FeedbackAlert variant="error" title="No se pudo crear el pedido">
                  {requestError}
                </FeedbackAlert>
              ) : null}

              {prepararPagoError ? (
                <FeedbackAlert variant="error" title="No se pudo iniciar el pago">
                  {prepararPagoError}
                </FeedbackAlert>
              ) : null}

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <button
                  type="button"
                  className="inline-flex min-h-[44px] w-full items-center justify-center rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 sm:w-auto"
                  onClick={() => setCurrentStep(1)}
                >
                  Volver
                </button>
                <button
                  type="submit"
                  className="inline-flex min-h-[44px] w-full items-center justify-center rounded-full px-5 py-3 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
                  style={{ backgroundColor: accentColor }}
                  disabled={crearPedido.isPending || !metodoPago}
                >
                  {crearPedido.isPending ? "Procesando..." : metodoPago === "TRANSFERENCIA" ? "Solicitar datos de transferencia" : "Pagar con Mercado Pago"}
                </button>
              </div>
            </form>
          ) : null}
        </div>

        <aside className="space-y-4">
          <section className="rounded-[2rem] border border-slate-200 bg-slate-50 p-6 shadow-sm">
            <h2 className="text-lg font-semibold text-slate-950">Resumen estimado</h2>
            <div className="mt-4 grid gap-3 rounded-[1.5rem] border border-slate-200 bg-white text-sm text-slate-700">
              <ResumenRow label="Cantidad" value={String(form.cantidad)} />
              <ResumenRow label="Subtotal" value={currencyFormatter.format(subtotalEstimado)} />
              <ResumenRow label="Seguro" value={seguroResumen} />
              {recargoMercadoPago > 0 ? <ResumenRow label="Recargo Mercado Pago" value={currencyFormatter.format(recargoMercadoPago)} /> : null}
              <ResumenRow
                label="Codigo de descuento"
                value={form.codigoDescuento.trim() ? form.codigoDescuento.trim().toUpperCase() : "No aplicado"}
              />
              <ResumenRow label="Total estimado" value={currencyFormatter.format(totalEstimado)} highlight />
            </div>
          </section>
        </aside>
      </div>
    </section>
  )
}

function getCheckoutUrl(payload: StorefrontPrepararPagoPspResponse): string | null {
  const candidate = payload.checkoutUrl
  if (typeof candidate !== "string" || !candidate.trim()) {
    return null
  }

  try {
    const parsed = new URL(candidate)
    return parsed.protocol === "http:" || parsed.protocol === "https:" ? parsed.toString() : null
  } catch {
    return null
  }
}

function ResumenRow({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`flex items-center justify-between gap-4 rounded-2xl px-4 py-3 ${highlight ? "bg-slate-900 text-white" : "bg-slate-50"}`}>
      <span className={`font-medium ${highlight ? "text-slate-200" : "text-slate-600"}`}>{label}</span>
      <span className="text-right font-semibold break-all">{value}</span>
    </div>
  )
}

function CheckoutSpotlightCard({
  label,
  value,
  tone = "default",
}: {
  label: string
  value: string
  tone?: "default" | "highlight"
}) {
  return (
    <article className={`rounded-[1.5rem] border px-4 py-4 shadow-sm ${tone === "highlight" ? "border-slate-300 bg-white" : "border-slate-200 bg-white"}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className={`mt-2 text-sm leading-6 ${tone === "highlight" ? "font-semibold text-slate-950" : "text-slate-800"}`}>{value}</p>
    </article>
  )
}

function normalizeUuid(value: string | null) {
  if (!value) {
    return null
  }
  const trimmed = value.trim()
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(trimmed)
    ? trimmed
    : null
}

function validateForm(form: StorefrontCheckoutFormState): string | null {
  if (!form.nombreApellido.trim()) return "El nombre y apellido es obligatorio."
  if (!form.email.trim()) return "El email es obligatorio."
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return "El email no tiene un formato valido."
  if (!form.telefonoWhatsapp.trim()) return "El telefono o WhatsApp es obligatorio."
  if (!form.provincia.trim()) return "La provincia es obligatoria."
  if (!form.localidad.trim()) return "La localidad es obligatoria."
  if (!form.codigoPostal.trim()) return "El codigo postal es obligatorio."
  if (!form.calle.trim()) return "La calle es obligatoria."
  if (!form.numero.trim()) return "El numero es obligatorio."
  if (form.cantidad < 1) return "La cantidad minima es 1."
  if (!form.aceptacionTerminos) return "Debes aceptar los terminos para crear el pedido."
  return null
}

function validateBuyerStep(form: StorefrontCheckoutFormState): string | null {
  if (!form.nombreApellido.trim()) return "El nombre y apellido es obligatorio."
  if (!form.email.trim()) return "El email es obligatorio."
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) return "El email no tiene un formato valido."
  if (!form.telefonoWhatsapp.trim()) return "El telefono o WhatsApp es obligatorio."
  if (!form.provincia.trim()) return "La provincia es obligatoria."
  if (!form.localidad.trim()) return "La localidad es obligatoria."
  if (!form.codigoPostal.trim()) return "El codigo postal es obligatorio."
  if (!form.calle.trim()) return "La calle es obligatoria."
  if (!form.numero.trim()) return "El numero es obligatorio."
  return null
}

function getRequestErrorMessage(error: unknown): string | null {
  if (!error) {
    return null
  }

  if (axios.isAxiosError(error)) {
    const message = typeof error.response?.data?.message === "string" ? error.response.data.message : null
    const detail = Array.isArray(error.response?.data?.details) ? error.response.data.details[0] : null
    return message || detail || "Ocurrio un error al crear el pedido."
  }

  return "Ocurrio un error al crear el pedido."
}
