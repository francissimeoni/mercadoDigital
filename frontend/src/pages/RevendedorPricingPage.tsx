import { useEffect, useMemo, useState } from "react"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useAuth } from "../auth/useAuth"
import {
  useDeleteRevendedorMarkupOverride,
  useRevendedorPricingConfig,
  useRevendedorPricingPreview,
  useUpdateRevendedorPricingConfig,
  useUpsertRevendedorMarkupOverride,
} from "../hooks/useRevendedorPricing"

type PricingMode = "PERCENTAGE" | "FIXED"

type PricingFormState = {
  markupMode: PricingMode
  markupValue: string
  commissionMode: PricingMode
  commissionValue: string
  discountCode: string
  discountValue: string
}

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

const percentFormatter = new Intl.NumberFormat("es-AR", {
  style: "percent",
  maximumFractionDigits: 2,
})

function deriveFormState(config: {
  markupPorcentaje: number
  markupFijo: number
  comisionAsesorPorcentaje: number
  comisionAsesorFija: number
  codigoDescuento?: string | null
  descuentoFijo?: number
}): PricingFormState {
  const markupUsesFixed = config.markupFijo > 0
  const commissionUsesFixed = config.comisionAsesorFija > 0

  return {
    markupMode: markupUsesFixed ? "FIXED" : "PERCENTAGE",
    markupValue: markupUsesFixed ? String(config.markupFijo) : String((config.markupPorcentaje ?? 0) * 100),
    commissionMode: commissionUsesFixed ? "FIXED" : "PERCENTAGE",
    commissionValue: commissionUsesFixed
      ? String(config.comisionAsesorFija)
      : String((config.comisionAsesorPorcentaje ?? 0) * 100),
    discountCode: config.codigoDescuento ?? "",
    discountValue: String(config.descuentoFijo ?? 0),
  }
}

function parseNumericInput(value: string) {
  const normalized = value.replace(",", ".").trim()
  if (!normalized) {
    return 0
  }
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : Number.NaN
}

function buildRequestPayload(form: PricingFormState) {
  const markupValue = parseNumericInput(form.markupValue)
  const commissionValue = parseNumericInput(form.commissionValue)
  const discountValue = parseNumericInput(form.discountValue)

  if (!Number.isFinite(markupValue) || markupValue < 0) {
    throw new Error("El valor de markup debe ser un numero positivo o cero.")
  }

  if (!Number.isFinite(commissionValue) || commissionValue < 0) {
    throw new Error("El valor de comision debe ser un numero positivo o cero.")
  }

  if (form.markupMode === "PERCENTAGE" && markupValue > 100) {
    throw new Error("El markup porcentual no puede superar 100%.")
  }

  if (form.commissionMode === "PERCENTAGE" && commissionValue > 100) {
    throw new Error("La comision porcentual no puede superar 100% del markup.")
  }

  if (!Number.isFinite(discountValue) || discountValue < 0) {
    throw new Error("El descuento fijo debe ser un numero positivo o cero.")
  }

  return {
    markupPorcentaje: form.markupMode === "PERCENTAGE" ? markupValue / 100 : 0,
    markupFijo: form.markupMode === "FIXED" ? markupValue : 0,
    comisionAsesorPorcentaje: form.commissionMode === "PERCENTAGE" ? commissionValue / 100 : 0,
    comisionAsesorFija: form.commissionMode === "FIXED" ? commissionValue : 0,
    codigoDescuento: form.discountCode.trim() || null,
    descuentoFijo: discountValue,
  }
}

export default function RevendedorPricingPage() {
  const { session } = useAuth()
  const revendedorId = session?.revendedorId || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const configQuery = useRevendedorPricingConfig(revendedorId)
  const previewQuery = useRevendedorPricingPreview(revendedorId)
  const updateConfig = useUpdateRevendedorPricingConfig(revendedorId)
  const upsertOverride = useUpsertRevendedorMarkupOverride(revendedorId)
  const deleteOverride = useDeleteRevendedorMarkupOverride(revendedorId)

  const [globalForm, setGlobalForm] = useState<PricingFormState>({
    markupMode: "PERCENTAGE",
    markupValue: "0",
    commissionMode: "PERCENTAGE",
    commissionValue: "0",
    discountCode: "",
    discountValue: "0",
  })
  const [search, setSearch] = useState("")
  const [editingProductoId, setEditingProductoId] = useState<string | null>(null)
  const [overrideForm, setOverrideForm] = useState<PricingFormState>({
    markupMode: "PERCENTAGE",
    markupValue: "0",
    commissionMode: "PERCENTAGE",
    commissionValue: "0",
    discountCode: "",
    discountValue: "0",
  })
  const [feedback, setFeedback] = useState<{ variant: "success" | "error" | "warning" | "info"; message: string } | null>(null)

  useEffect(() => {
    if (configQuery.data) {
      setGlobalForm(deriveFormState(configQuery.data))
    }
  }, [configQuery.data])

  const items = previewQuery.data ?? []
  const filteredItems = useMemo(
    () =>
      items.filter((item) => {
        const haystack = `${item.nombrePublico} ${item.origenConfiguracion}`.toLowerCase()
        return haystack.includes(search.toLowerCase())
      }),
    [items, search],
  )

  const summary = useMemo(() => {
    const withOverride = items.filter((item) => item.origenConfiguracion === "PRODUCTO").length
    const avgMargin = items.length > 0 ? items.reduce((sum, item) => sum + item.gananciaNetaRevendedor, 0) / items.length : 0
    return {
      total: items.length,
      withOverride,
      usingGlobal: Math.max(items.length - withOverride, 0),
      avgMargin,
    }
  }, [items])

  if (!revendedorId) {
    return (
      <section className="space-y-4">
        <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <h2 className="text-3xl font-semibold tracking-tight text-slate-950">Precios y comisiones</h2>
          <p className="mt-3 text-sm text-slate-600">Esta pantalla necesita un `revendedorId` operativo en sesion.</p>
        </header>
        <FeedbackAlert variant="warning" title="Revendedor no identificado">
          Primero necesitamos conocer que revendedor esta operando para poder cargar su configuracion comercial.
        </FeedbackAlert>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Revendedor</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Precios y comisiones</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Configura el markup global de tu tienda, la comision de asesores y los ajustes puntuales por producto.
              La comision siempre sale de tu markup, para que quede claro cuanto gana el asesor y cuanto te queda limpio.
            </p>
          </div>
          <div className="rounded-3xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
            Pricing v1 operativo para {items.length} productos
          </div>
        </div>
      </header>

      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="Productos visibles" value={String(summary.total)} detail="Vista previa del catalogo comercial." />
        <SummaryCard label="Overrides propios" value={String(summary.withOverride)} detail="Productos con ajuste puntual." />
        <SummaryCard label="Usando global" value={String(summary.usingGlobal)} detail="Productos siguiendo la regla general." />
        <SummaryCard label="Ganancia neta promedio" value={currencyFormatter.format(summary.avgMargin)} detail="Margen estimado por item antes de PSP/envio final." />
      </div>

      <section className="grid gap-6 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Configuracion global</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">Regla base del storefront</h3>
            </div>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-slate-600">
              V1
            </span>
          </div>

          <div className="mt-5 space-y-5">
            <RuleEditor
              title="Markup revendedor"
              description="Define cuanto se suma al costo para armar el precio final visible al cliente."
              mode={globalForm.markupMode}
              value={globalForm.markupValue}
              onModeChange={(mode) => {
                setGlobalForm((current) => ({ ...current, markupMode: mode }))
              }}
              onValueChange={(value) => {
                setGlobalForm((current) => ({ ...current, markupValue: value }))
              }}
            />

            <RuleEditor
              title="Comision asesor"
              description="Puede ser un porcentaje de tu markup o un monto fijo por venta."
              mode={globalForm.commissionMode}
              value={globalForm.commissionValue}
              onModeChange={(mode) => {
                setGlobalForm((current) => ({ ...current, commissionMode: mode }))
              }}
              onValueChange={(value) => {
                setGlobalForm((current) => ({ ...current, commissionValue: value }))
              }}
            />

            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-5">
              <div>
                <p className="text-sm font-semibold text-slate-900">Codigo de descuento storefront</p>
                <p className="mt-1 text-sm text-slate-600">
                  Este cupon descuenta solo contra tu markup. Nunca baja costo proveedor, costo MD, PSP, envio ni seguro.
                </p>
              </div>

              <div className="mt-4 grid gap-4 md:grid-cols-2">
                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Codigo</span>
                  <input
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm uppercase outline-none transition focus:border-blue-300"
                    value={globalForm.discountCode}
                    onChange={(event) => {
                      setGlobalForm((current) => ({ ...current, discountCode: event.target.value.toUpperCase() }))
                    }}
                    placeholder="Ejemplo: PROMO10"
                  />
                </label>

                <label className="space-y-2">
                  <span className="text-sm font-medium text-slate-700">Descuento fijo</span>
                  <input
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-300"
                    value={globalForm.discountValue}
                    inputMode="decimal"
                    onChange={(event) => {
                      setGlobalForm((current) => ({ ...current, discountValue: event.target.value }))
                    }}
                    placeholder="Ejemplo: 500"
                  />
                </label>
              </div>
            </div>
          </div>

          <div className="mt-6 rounded-3xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            <p className="font-semibold text-slate-800">Como se calcula</p>
            <p className="mt-2">PC + markup = precio comercial del producto.</p>
            <p className="mt-1">Comision asesor sale del markup, no del costo ni del total vendido.</p>
            <p className="mt-1">Si hay cupón, el descuento solo puede comerse tu markup disponible.</p>
          </div>

          <button
            type="button"
            className="mt-6 rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            disabled={updateConfig.isPending || configQuery.isLoading}
            onClick={() => {
              setFeedback(null)
              try {
                const payload = buildRequestPayload(globalForm)
                updateConfig.mutate(payload, {
                  onSuccess: () => {
                    setFeedback({ variant: "success", message: "La configuracion global se guardo correctamente." })
                  },
                  onError: () => {
                    setFeedback({ variant: "error", message: "No se pudo guardar la configuracion global. Reintenta en unos segundos." })
                  },
                })
              } catch (error) {
                setFeedback({ variant: "warning", message: error instanceof Error ? error.message : "Revisa los valores cargados." })
              }
            }}
          >
            {updateConfig.isPending ? "Guardando configuracion..." : "Guardar configuracion global"}
          </button>
        </article>

        <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Preview operativo</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">Productos y resultado estimado</h3>
            </div>
            <input
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white md:max-w-xs"
              placeholder="Buscar producto"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
              }}
            />
          </div>

          {previewQuery.isLoading ? (
            <div className="mt-6 grid gap-3">
              {Array.from({ length: 5 }).map((_, index) => (
                <div key={index} className="h-24 animate-pulse rounded-3xl bg-slate-100" />
              ))}
            </div>
          ) : null}

          {previewQuery.isError ? (
            <div className="mt-6">
              <FeedbackAlert variant="error" title="No se pudo cargar la preview comercial">
                Reintenta la lectura del backend para ver precios, margenes y overrides del revendedor.
              </FeedbackAlert>
            </div>
          ) : null}

          {!previewQuery.isLoading && !previewQuery.isError ? (
            <div className="mt-6 space-y-4">
              {filteredItems.map((item) => {
                const isEditing = editingProductoId === item.productoBaseId
                return (
                  <article key={item.productoBaseId} className="rounded-[1.75rem] border border-slate-200 bg-slate-50 p-4">
                    <div className="flex flex-wrap items-start gap-4">
                      <div className="h-20 w-20 overflow-hidden rounded-2xl border border-slate-200 bg-white">
                        {item.imagenPrincipal ? (
                          <img src={item.imagenPrincipal} alt={item.nombrePublico} className="h-full w-full object-cover" />
                        ) : (
                          <div className="flex h-full items-center justify-center text-[11px] font-medium uppercase tracking-[0.25em] text-slate-400">
                            Sin imagen
                          </div>
                        )}
                      </div>

                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <h4 className="text-lg font-semibold text-slate-950">{item.nombrePublico}</h4>
                            <p className="mt-1 text-sm text-slate-500">
                              Stock {item.stockDisponible} - demora {item.demoraEstimadaDias} dias
                            </p>
                          </div>
                          <div className="flex flex-wrap gap-2">
                            <Badge tone={item.origenConfiguracion === "PRODUCTO" ? "blue" : "slate"}>
                              {item.origenConfiguracion === "PRODUCTO" ? "Override por producto" : "Usando global"}
                            </Badge>
                            <button
                              type="button"
                              className="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] text-slate-600 transition hover:border-blue-200 hover:text-blue-700"
                              onClick={() => {
                                setEditingProductoId((current) => {
                                  if (current === item.productoBaseId) {
                                    return null
                                  }
                                  setOverrideForm(deriveFormState(item))
                                  return item.productoBaseId
                                })
                              }}
                            >
                              {isEditing ? "Cerrar" : "Editar override"}
                            </button>
                          </div>
                        </div>

                        <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-5">
                          <MetricCell label="Costo operativo" value={currencyFormatter.format(item.costoOperativo)} />
                          <MetricCell label="Markup bruto" value={currencyFormatter.format(item.markupBruto)} />
                          <MetricCell label="Comision asesor" value={currencyFormatter.format(item.comisionAsesorMonto)} />
                          <MetricCell label="Ganancia neta" value={currencyFormatter.format(item.gananciaNetaRevendedor)} />
                          <MetricCell label="Precio final" value={currencyFormatter.format(item.precioFinalEstimado)} />
                        </div>

                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                          <span>Markup actual: {formatRuleValue(item.markupPorcentaje, item.markupFijo)}</span>
                          <span>-</span>
                          <span>Comision actual: {formatRuleValue(item.comisionAsesorPorcentaje, item.comisionAsesorFija)}</span>
                        </div>
                      </div>
                    </div>

                    {isEditing ? (
                      <div className="mt-5 rounded-[1.5rem] border border-blue-100 bg-white p-4">
                        <div className="grid gap-4 xl:grid-cols-2">
                          <RuleEditor
                            title="Markup puntual"
                            description="Solo para este producto."
                            mode={overrideForm.markupMode}
                            value={overrideForm.markupValue}
                            compact
                            onModeChange={(mode) => {
                              setOverrideForm((current) => ({ ...current, markupMode: mode }))
                            }}
                            onValueChange={(value) => {
                              setOverrideForm((current) => ({ ...current, markupValue: value }))
                            }}
                          />
                          <RuleEditor
                            title="Comision puntual"
                            description="Solo para este producto."
                            mode={overrideForm.commissionMode}
                            value={overrideForm.commissionValue}
                            compact
                            onModeChange={(mode) => {
                              setOverrideForm((current) => ({ ...current, commissionMode: mode }))
                            }}
                            onValueChange={(value) => {
                              setOverrideForm((current) => ({ ...current, commissionValue: value }))
                            }}
                          />
                        </div>

                        <div className="mt-4 flex flex-wrap gap-3">
                          <button
                            type="button"
                            className="rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
                            disabled={upsertOverride.isPending}
                            onClick={() => {
                              setFeedback(null)
                              try {
                                const payload = buildRequestPayload(overrideForm)
                                upsertOverride.mutate(
                                  {
                                    productoId: item.productoBaseId,
                                    request: {
                                      ...payload,
                                      activo: true,
                                    },
                                  },
                                  {
                                    onSuccess: () => {
                                      setFeedback({ variant: "success", message: `Se guardo el override de ${item.nombrePublico}.` })
                                      setEditingProductoId(null)
                                    },
                                    onError: () => {
                                      setFeedback({ variant: "error", message: `No se pudo guardar el override de ${item.nombrePublico}.` })
                                    },
                                  },
                                )
                              } catch (error) {
                                setFeedback({ variant: "warning", message: error instanceof Error ? error.message : "Revisa los valores del override." })
                              }
                            }}
                          >
                            {upsertOverride.isPending ? "Guardando..." : "Guardar override"}
                          </button>

                          <button
                            type="button"
                            className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:border-red-200 hover:bg-red-50 hover:text-red-700 disabled:cursor-not-allowed disabled:opacity-70"
                            disabled={deleteOverride.isPending || item.origenConfiguracion !== "PRODUCTO"}
                            onClick={() => {
                              deleteOverride.mutate(item.productoBaseId, {
                                onSuccess: () => {
                                  setFeedback({ variant: "success", message: `Se elimino el override de ${item.nombrePublico}.` })
                                  setEditingProductoId(null)
                                },
                                onError: () => {
                                  setFeedback({ variant: "error", message: `No se pudo eliminar el override de ${item.nombrePublico}.` })
                                },
                              })
                            }}
                          >
                            Eliminar override
                          </button>
                        </div>
                      </div>
                    ) : null}
                  </article>
                )
              })}

              {filteredItems.length === 0 ? (
                <div className="rounded-3xl border border-dashed border-slate-200 bg-slate-50 px-6 py-10 text-center text-sm text-slate-500">
                  No hay productos que coincidan con la busqueda actual.
                </div>
              ) : null}
            </div>
          ) : null}
        </article>
      </section>
    </section>
  )
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-4 text-4xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function RuleEditor({
  title,
  description,
  mode,
  value,
  onModeChange,
  onValueChange,
  compact = false,
}: {
  title: string
  description: string
  mode: PricingMode
  value: string
  onModeChange: (mode: PricingMode) => void
  onValueChange: (value: string) => void
  compact?: boolean
}) {
  return (
    <div className={`rounded-3xl border border-slate-200 bg-slate-50 ${compact ? "p-4" : "p-5"}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">{title}</p>
          <p className="mt-1 text-sm text-slate-600">{description}</p>
        </div>
        <div className="flex gap-2 rounded-full bg-white p-1 shadow-sm">
          <ModeButton active={mode === "PERCENTAGE"} onClick={() => onModeChange("PERCENTAGE")}>
            %
          </ModeButton>
          <ModeButton active={mode === "FIXED"} onClick={() => onModeChange("FIXED")}>
            $
          </ModeButton>
        </div>
      </div>

      <div className="mt-4">
        <input
          className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-300"
          value={value}
          inputMode="decimal"
          onChange={(event) => {
            onValueChange(event.target.value)
          }}
          placeholder={mode === "PERCENTAGE" ? "Ejemplo: 18" : "Ejemplo: 2500"}
        />
      </div>
    </div>
  )
}

function ModeButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] transition ${
        active ? "bg-slate-950 text-white" : "text-slate-500 hover:bg-slate-100"
      }`}
    >
      {children}
    </button>
  )
}

function MetricCell({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-white px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.25em] text-slate-400">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-900">{value}</p>
    </article>
  )
}

function Badge({ tone, children }: { tone: "blue" | "slate"; children: string }) {
  return (
    <span
      className={`rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.22em] ${
        tone === "blue" ? "bg-blue-50 text-blue-700" : "bg-slate-200 text-slate-600"
      }`}
    >
      {children}
    </span>
  )
}

function formatRuleValue(percentage: number, fixed: number) {
  if (fixed > 0) {
    return currencyFormatter.format(fixed)
  }
  return percentFormatter.format(percentage ?? 0)
}
