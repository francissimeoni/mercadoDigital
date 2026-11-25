import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import { useEffect, useMemo, useState } from "react"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useAdminCategoriasProducto,
  useAdminCorreoMdConfig,
  useAdminCostoMdConfig,
  useAdminMercadoPagoMdConfig,
  useAdminMercadoPagoMdOAuthLink,
  useAdminPspConfig,
  useCreateAdminCategoriaProducto,
  useUpdateAdminMercadoPagoMdConfig,
  useUpdateAdminCorreoMdConfig,
  useUpdateAdminCategoriaProducto,
  useUpdateAdminCostoMdConfig,
  useUpdateAdminPspConfig,
  useTestAdminCorreoMdConfig,
} from "../../hooks/useAdmin"

type FormState = Record<string, string>
type TipoCalculoState = Record<string, "MONTO_FIJO" | "PORCENTAJE">

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

function parseAmount(value: string) {
  const normalized = value.replace(",", ".").trim()
  if (!normalized) {
    return 0
  }
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : Number.NaN
}

function formatTimestamp(value: string | null) {
  if (!value) {
    return "Sin ediciones todavia"
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-AR")
}

export default function AdminConfiguracionPage() {
  const configQuery = useAdminCostoMdConfig()
  const categoriasProductoQuery = useAdminCategoriasProducto()
  const updateConfig = useUpdateAdminCostoMdConfig()
  const createCategoriaProducto = useCreateAdminCategoriaProducto()
  const updateCategoriaProducto = useUpdateAdminCategoriaProducto()
  const pspConfigQuery = useAdminPspConfig()
  const correoMdConfigQuery = useAdminCorreoMdConfig()
  const mercadoPagoMdConfigQuery = useAdminMercadoPagoMdConfig()
  const updatePspConfig = useUpdateAdminPspConfig()
  const updateCorreoMdConfig = useUpdateAdminCorreoMdConfig()
  const updateMercadoPagoMdConfig = useUpdateAdminMercadoPagoMdConfig()
  const testCorreoMdConfig = useTestAdminCorreoMdConfig()
  const mercadoPagoMdOAuthLink = useAdminMercadoPagoMdOAuthLink()
  const [form, setForm] = useState<FormState>({})
  const [tipoCalculoForm, setTipoCalculoForm] = useState<TipoCalculoState>({})
  const [pspPorcentaje, setPspPorcentaje] = useState("")
  const [seguroPorcentaje, setSeguroPorcentaje] = useState("")
  const [correoFromEmail, setCorreoFromEmail] = useState("")
  const [correoFromName, setCorreoFromName] = useState("")
  const [correoReplyTo, setCorreoReplyTo] = useState("")
  const [correoSmtpHost, setCorreoSmtpHost] = useState("")
  const [correoSmtpPort, setCorreoSmtpPort] = useState("")
  const [correoSmtpUsername, setCorreoSmtpUsername] = useState("")
  const [correoSmtpPassword, setCorreoSmtpPassword] = useState("")
  const [correoSmtpAuth, setCorreoSmtpAuth] = useState(true)
  const [correoSmtpStartTls, setCorreoSmtpStartTls] = useState(true)
  const [correoTestTo, setCorreoTestTo] = useState("")
  const [mdMercadoPagoUserId, setMdMercadoPagoUserId] = useState("")
  const [mdPublicKey, setMdPublicKey] = useState("")
  const [mdAccessToken, setMdAccessToken] = useState("")
  const [mdRefreshToken, setMdRefreshToken] = useState("")
  const [mdTokenExpiresAt, setMdTokenExpiresAt] = useState("")
  const [mdLiveMode, setMdLiveMode] = useState(false)
  const [mdScope, setMdScope] = useState("")
  const [mdPayoutEmail, setMdPayoutEmail] = useState("")
  const [feedback, setFeedback] = useState<{ variant: "success" | "error" | "warning"; message: string } | null>(null)
  const [nuevaCategoria, setNuevaCategoria] = useState("")

  useEffect(() => {
    if (configQuery.data) {
      setForm(
        Object.fromEntries(configQuery.data.items.map((item) => [item.categoriaPaquete, String(item.monto)])),
      )
      setTipoCalculoForm(
        Object.fromEntries(configQuery.data.items.map((item) => [item.categoriaPaquete, item.tipoCalculo ?? "MONTO_FIJO"])),
      )
    }
  }, [configQuery.data])

  useEffect(() => {
    if (pspConfigQuery.data) {
      setPspPorcentaje(String(pspConfigQuery.data.pspFeePorcentaje))
      setSeguroPorcentaje(String(pspConfigQuery.data.seguroTransportePorcentaje))
    }
  }, [pspConfigQuery.data])

  useEffect(() => {
    if (correoMdConfigQuery.data) {
      setCorreoFromEmail(correoMdConfigQuery.data.fromEmail ?? "")
      setCorreoFromName(correoMdConfigQuery.data.fromName ?? "")
      setCorreoReplyTo(correoMdConfigQuery.data.replyTo ?? "")
      setCorreoSmtpHost(correoMdConfigQuery.data.smtpHost ?? "")
      setCorreoSmtpPort(correoMdConfigQuery.data.smtpPort != null ? String(correoMdConfigQuery.data.smtpPort) : "")
      setCorreoSmtpUsername(correoMdConfigQuery.data.smtpUsername ?? "")
      setCorreoSmtpPassword("")
      setCorreoSmtpAuth(correoMdConfigQuery.data.smtpAuth)
      setCorreoSmtpStartTls(correoMdConfigQuery.data.smtpStartTls)
    }
  }, [correoMdConfigQuery.data])

  useEffect(() => {
    if (mercadoPagoMdConfigQuery.data) {
      setMdMercadoPagoUserId(mercadoPagoMdConfigQuery.data.mercadoPagoUserId ?? "")
      setMdPublicKey(mercadoPagoMdConfigQuery.data.publicKey ?? "")
      setMdAccessToken("")
      setMdRefreshToken("")
      setMdTokenExpiresAt(mercadoPagoMdConfigQuery.data.tokenExpiresAt ?? "")
      setMdLiveMode(mercadoPagoMdConfigQuery.data.liveMode ?? false)
      setMdScope(mercadoPagoMdConfigQuery.data.scope ?? "")
      setMdPayoutEmail(mercadoPagoMdConfigQuery.data.payoutEmail ?? "")
    }
  }, [mercadoPagoMdConfigQuery.data])

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (!MERCADO_PAGO_DISPONIBLE) return
    const status = params.get("mp_oauth_status")
    const subjectType = params.get("mp_subject_type")
    const message = params.get("mp_message")
    if (!status || subjectType !== "MERCADO_DIGITAL") {
      return
    }

    setFeedback({
      variant: status === "success" ? "success" : "error",
      message:
        message ??
        (status === "success"
          ? "Mercado Pago MD vinculado correctamente."
          : "No se pudo vincular Mercado Pago MD."),
    })
    params.delete("mp_oauth_status")
    params.delete("mp_message")
    params.delete("mp_subject_type")
    params.delete("mp_subject_id")
    const next = params.toString()
    window.history.replaceState({}, "", `${window.location.pathname}${next ? `?${next}` : ""}`)
    void mercadoPagoMdConfigQuery.refetch()
  }, [mercadoPagoMdConfigQuery])

  const items = configQuery.data?.items ?? []
  const resumen = useMemo(() => {
    const totalFijo = items
      .filter((item) => (tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo) === "MONTO_FIJO")
      .reduce((sum, item) => sum + item.monto, 0)
    const ultimoCambio = items
      .map((item) => item.updatedAt)
      .filter((value): value is string => Boolean(value))
      .sort()
      .at(-1)

    return {
      totalFijo,
      categorias: items.length,
      ultimoCambio: ultimoCambio ?? null,
    }
  }, [items, tipoCalculoForm])

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-4xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Configuracion operativa</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Ajusta el costo operativo MD por categoria de paquete sin redeploy. Esta base alimenta el costo compuesto
              que luego usa pricing, ofertas y split logico.
            </p>
          </div>
          <div className="rounded-3xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Cambio administrable para pruebas y ajuste fino del negocio
          </div>
        </div>
      </header>

      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      <div className="grid gap-4 md:grid-cols-3">
        <SummaryCard label="Categorias activas" value={String(resumen.categorias)} detail="Base actual de costos MD configurables." />
        <SummaryCard label="Costo fijo total" value={currencyFormatter.format(resumen.totalFijo)} detail="Suma de categorias configuradas en monto fijo." />
        <SummaryCard label="Ultimo cambio" value={formatTimestamp(resumen.ultimoCambio)} detail="Referencia de la ultima edicion persistida." />
      </div>

      {MERCADO_PAGO_DISPONIBLE && (<section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">PSP</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Comision de pasarela</h3>
            <p className="mt-2 text-sm text-slate-600">
              Configuracion global del porcentaje PSP usado por pricing para calcular precio final en storefront.
            </p>
          </div>
          <button
            type="button"
            className="rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            disabled={updatePspConfig.isPending || pspConfigQuery.isLoading}
            onClick={() => {
              setFeedback(null)
              const value = parseAmount(pspPorcentaje)
              const seguroValue = parseAmount(seguroPorcentaje)
              if (!Number.isFinite(value) || value < 0 || value > 100) {
                setFeedback({ variant: "warning", message: "El porcentaje PSP debe estar entre 0 y 100." })
                return
              }
              if (!Number.isFinite(seguroValue) || seguroValue < 0 || seguroValue > 100) {
                setFeedback({ variant: "warning", message: "El porcentaje de seguro debe estar entre 0 y 100." })
                return
              }
              updatePspConfig.mutate(
                { pspFeePorcentaje: value, seguroTransportePorcentaje: seguroValue },
                {
                  onSuccess: () => setFeedback({ variant: "success", message: "Configuracion PSP guardada correctamente." }),
                  onError: () => setFeedback({ variant: "error", message: "No se pudo guardar la configuracion PSP." }),
                },
              )
            }}
          >
            {updatePspConfig.isPending ? "Guardando PSP..." : "Guardar PSP"}
          </button>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Porcentaje PSP</span>
            <input
              type="text"
              inputMode="decimal"
              value={pspPorcentaje}
              onChange={(event) => setPspPorcentaje(event.target.value)}
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              placeholder="Ejemplo: 8"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Seguro de transporte (%)</span>
            <input
              type="text"
              inputMode="decimal"
              value={seguroPorcentaje}
              onChange={(event) => setSeguroPorcentaje(event.target.value)}
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              placeholder="Ejemplo: 2"
            />
          </label>
          <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Factor aplicado</p>
            <p className="mt-2 text-2xl font-semibold text-slate-900">
              PSP: {Number.isFinite(parseAmount(pspPorcentaje)) ? `${(parseAmount(pspPorcentaje) / 100).toFixed(4)}` : "0.0000"}
              <br />
              Seguro: {Number.isFinite(parseAmount(seguroPorcentaje)) ? `${(parseAmount(seguroPorcentaje) / 100).toFixed(4)}` : "0.0000"}
            </p>
          </article>
        </div>
      </section>)}

      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Correo MD</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Credenciales internas</h3>
            <p className="mt-2 text-sm text-slate-600">
              Configuracion del correo que usa MD para avisos internos a revendedor, proveedor y logistica.
            </p>
          </div>
          <button
            type="button"
            className="rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            disabled={updateCorreoMdConfig.isPending || correoMdConfigQuery.isLoading}
            onClick={() => {
              setFeedback(null)
              if (!correoFromEmail.trim() || !correoFromName.trim()) {
                setFeedback({ variant: "warning", message: "Completa from email y from name para guardar correo MD." })
                return
              }
              const smtpPortParsed = correoSmtpPort.trim() ? Number(correoSmtpPort.trim()) : null
              if (smtpPortParsed !== null && (!Number.isInteger(smtpPortParsed) || smtpPortParsed <= 0)) {
                setFeedback({ variant: "warning", message: "El puerto SMTP debe ser un entero positivo." })
                return
              }
              updateCorreoMdConfig.mutate(
                {
                  fromEmail: correoFromEmail.trim(),
                  fromName: correoFromName.trim(),
                  replyTo: correoReplyTo.trim() || null,
                  smtpHost: correoSmtpHost.trim() || null,
                  smtpPort: smtpPortParsed,
                  smtpUsername: correoSmtpUsername.trim() || null,
                  smtpPassword: correoSmtpPassword.trim() || null,
                  smtpAuth: correoSmtpAuth,
                  smtpStartTls: correoSmtpStartTls,
                },
                {
                  onSuccess: () => {
                    setCorreoSmtpPassword("")
                    setFeedback({ variant: "success", message: "Configuracion de correo MD guardada correctamente." })
                  },
                  onError: () => setFeedback({ variant: "error", message: "No se pudo guardar la configuracion de correo MD." }),
                },
              )
            }}
          >
            {updateCorreoMdConfig.isPending ? "Guardando correo..." : "Guardar correo MD"}
          </button>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-3">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">From email</span>
            <input type="email" value={correoFromEmail} onChange={(event) => setCorreoFromEmail(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">From name</span>
            <input type="text" value={correoFromName} onChange={(event) => setCorreoFromName(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Reply-to</span>
            <input type="email" value={correoReplyTo} onChange={(event) => setCorreoReplyTo(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">SMTP host</span>
            <input type="text" value={correoSmtpHost} onChange={(event) => setCorreoSmtpHost(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">SMTP puerto</span>
            <input type="number" value={correoSmtpPort} onChange={(event) => setCorreoSmtpPort(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">SMTP usuario</span>
            <input type="text" value={correoSmtpUsername} onChange={(event) => setCorreoSmtpUsername(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2 md:col-span-2">
            <span className="text-sm font-medium text-slate-700">SMTP password</span>
            <input type="password" value={correoSmtpPassword} onChange={(event) => setCorreoSmtpPassword(event.target.value)} placeholder={correoMdConfigQuery.data?.smtpConfigured ? "Dejar vacio para conservar la actual" : ""} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={correoSmtpAuth} onChange={(event) => setCorreoSmtpAuth(event.target.checked)} />
              SMTP auth
            </label>
            <label className="mt-2 flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={correoSmtpStartTls} onChange={(event) => setCorreoSmtpStartTls(event.target.checked)} />
              SMTP STARTTLS
            </label>
          </div>
          <label className="space-y-2 md:col-span-2">
            <span className="text-sm font-medium text-slate-700">Email receptor para prueba</span>
            <input type="email" value={correoTestTo} onChange={(event) => setCorreoTestTo(event.target.value)} placeholder="ejemplo@dominio.com" className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <div className="md:col-span-1">
            <button
              type="button"
              className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-medium text-slate-800 transition hover:border-slate-400 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={testCorreoMdConfig.isPending || !correoTestTo.trim()}
              onClick={() => {
                setFeedback(null)
                testCorreoMdConfig.mutate(
                  { toEmail: correoTestTo.trim() },
                  {
                    onSuccess: () => setFeedback({ variant: "success", message: `Mail de prueba enviado a ${correoTestTo.trim()}.` }),
                    onError: () => setFeedback({ variant: "error", message: "No se pudo enviar el mail de prueba." }),
                  },
                )
              }}
            >
              {testCorreoMdConfig.isPending ? "Enviando test..." : "Test"}
            </button>
          </div>
        </div>
      </section>

      {MERCADO_PAGO_DISPONIBLE && (<section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Payout MD</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Mercado Pago de Mercado Digital</h3>
            <p className="mt-2 text-sm text-slate-600">
              Esta cuenta se usa para percibir el costo operativo MD y ejecutar payout real al marcar `RETIRADO`.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              className="rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-medium text-slate-800 transition hover:border-slate-400 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={mercadoPagoMdOAuthLink.isPending}
              onClick={() => {
                setFeedback(null)
                mercadoPagoMdOAuthLink.mutate(undefined, {
                  onSuccess: (response) => {
                    window.open(response.authorizationUrl, "_blank", "noopener,noreferrer")
                  },
                  onError: () => setFeedback({ variant: "error", message: "No se pudo generar el link OAuth de Mercado Pago MD." }),
                })
              }}
            >
              {mercadoPagoMdOAuthLink.isPending ? "Generando OAuth..." : "Generar OAuth MD"}
            </button>
            <button
              type="button"
              className="rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={updateMercadoPagoMdConfig.isPending || mercadoPagoMdConfigQuery.isLoading}
              onClick={() => {
                setFeedback(null)
                if (!mdPayoutEmail.trim()) {
                  setFeedback({ variant: "warning", message: "Completa el payout email de MD para poder cobrar su parte." })
                  return
                }
                updateMercadoPagoMdConfig.mutate(
                  {
                    mercadoPagoUserId: mdMercadoPagoUserId.trim(),
                    publicKey: mdPublicKey.trim(),
                    accessToken: mdAccessToken.trim(),
                    refreshToken: mdRefreshToken.trim(),
                    tokenExpiresAt: mdTokenExpiresAt.trim() ? mdTokenExpiresAt : null,
                    liveMode: mdLiveMode,
                    scope: mdScope.trim(),
                    payoutEmail: mdPayoutEmail.trim(),
                  },
                  {
                    onSuccess: () => {
                      setMdAccessToken("")
                      setMdRefreshToken("")
                      setFeedback({ variant: "success", message: "Configuracion Mercado Pago MD guardada correctamente." })
                    },
                    onError: () => setFeedback({ variant: "error", message: "No se pudo guardar Mercado Pago MD." }),
                  },
                )
              }}
            >
              {updateMercadoPagoMdConfig.isPending ? "Guardando MP MD..." : "Guardar MP MD"}
            </button>
          </div>
        </div>

        <div className="mt-5 grid gap-4 md:grid-cols-3">
          <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Estado actual</p>
            <p className="mt-2 text-xl font-semibold text-slate-900">{mercadoPagoMdConfigQuery.data?.estado ?? "PENDIENTE"}</p>
            <p className="mt-1 text-xs text-slate-500">
              Checkout: {mercadoPagoMdConfigQuery.data?.readyForCheckout ? "listo" : "pendiente"}
            </p>
          </article>
          <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Access token</p>
            <p className="mt-2 text-sm font-medium text-slate-900">
              {mercadoPagoMdConfigQuery.data?.accessTokenConfigured
                ? mercadoPagoMdConfigQuery.data.accessTokenMasked ?? "configurado"
                : "no configurado"}
            </p>
          </article>
          <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">Payout email</p>
            <p className="mt-2 text-sm font-medium text-slate-900">{mercadoPagoMdConfigQuery.data?.payoutEmail ?? "sin definir"}</p>
          </article>

          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Mercado Pago user ID</span>
            <input type="text" value={mdMercadoPagoUserId} onChange={(event) => setMdMercadoPagoUserId(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Public key</span>
            <input type="text" value={mdPublicKey} onChange={(event) => setMdPublicKey(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Payout email (MD)</span>
            <input type="email" value={mdPayoutEmail} onChange={(event) => setMdPayoutEmail(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Access token</span>
            <input type="text" value={mdAccessToken} onChange={(event) => setMdAccessToken(event.target.value)} placeholder={mercadoPagoMdConfigQuery.data?.accessTokenConfigured ? "Dejar vacio para conservar el actual" : ""} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Refresh token</span>
            <input type="text" value={mdRefreshToken} onChange={(event) => setMdRefreshToken(event.target.value)} placeholder={mercadoPagoMdConfigQuery.data?.refreshTokenConfigured ? "Dejar vacio para conservar el actual" : ""} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Token expira</span>
            <input type="datetime-local" value={mdTokenExpiresAt} onChange={(event) => setMdTokenExpiresAt(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <label className="space-y-2 md:col-span-2">
            <span className="text-sm font-medium text-slate-700">Scope</span>
            <input type="text" value={mdScope} onChange={(event) => setMdScope(event.target.value)} className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500" />
          </label>
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
            <label className="flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={mdLiveMode} onChange={(event) => setMdLiveMode(event.target.checked)} />
              Cuenta en modo live
            </label>
          </div>
        </div>
      </section>)}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Costo MD por categoria</p>
              <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Montos vigentes</h3>
            </div>
            <button
              type="button"
              className="rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={configQuery.isLoading || updateConfig.isPending || items.length === 0}
              onClick={() => {
                setFeedback(null)
                try {
                  const payloadItems = items.map((item) => {
                    const value = parseAmount(form[item.categoriaPaquete] ?? "")
                    if (!Number.isFinite(value) || value < 0) {
                      throw new Error(`El monto para ${item.categoriaPaquete} debe ser un numero valido mayor o igual a cero.`)
                    }
                    return {
                      categoriaPaquete: item.categoriaPaquete,
                      monto: value,
                      tipoCalculo: tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo ?? "MONTO_FIJO",
                    }
                  })

                  updateConfig.mutate(
                    { items: payloadItems },
                    {
                      onSuccess: () => {
                        setFeedback({ variant: "success", message: "La configuracion de costos MD se guardo correctamente." })
                      },
                      onError: () => {
                        setFeedback({ variant: "error", message: "No se pudo guardar la configuracion. Reintenta en unos segundos." })
                      },
                    },
                  )
                } catch (error) {
                  setFeedback({
                    variant: "warning",
                    message: error instanceof Error ? error.message : "Revisa los montos cargados antes de guardar.",
                  })
                }
              }}
            >
              {updateConfig.isPending ? "Guardando..." : "Guardar costos MD"}
            </button>
          </div>

          {configQuery.isLoading ? (
            <div className="mt-6 grid gap-4 md:grid-cols-3">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="h-44 animate-pulse rounded-[1.75rem] bg-slate-100" />
              ))}
            </div>
          ) : null}

          {configQuery.isError ? (
            <div className="mt-6">
              <FeedbackAlert variant="error" title="No se pudo cargar la configuracion">
                Reintenta la lectura del backend para editar los montos por categoria.
              </FeedbackAlert>
            </div>
          ) : null}

          {!configQuery.isLoading && !configQuery.isError ? (
            <div className="mt-6 grid gap-4 md:grid-cols-3">
              {items.map((item) => (
                <article key={item.categoriaPaquete} className="rounded-[1.75rem] border border-slate-200 bg-slate-50 p-5">
                  <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{item.categoriaPaquete}</p>
                  <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
                    {(tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo) === "PORCENTAJE"
                      ? `${(parseAmount(form[item.categoriaPaquete] ?? String(item.monto)) || 0).toFixed(2)}%`
                      : currencyFormatter.format(parseAmount(form[item.categoriaPaquete] ?? String(item.monto)) || 0)}
                  </p>
                  <p className="mt-2 text-sm text-slate-600">
                    Ajustable desde admin para combustible, embalaje, retiro y operacion.
                  </p>

                  <div className="mt-5">
                    <label className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Tipo de calculo</label>
                    <select
                      value={tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo ?? "MONTO_FIJO"}
                      onChange={(event) =>
                        setTipoCalculoForm((current) => ({
                          ...current,
                          [item.categoriaPaquete]: event.target.value as "MONTO_FIJO" | "PORCENTAJE",
                        }))
                      }
                      className="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-300"
                    >
                      <option value="MONTO_FIJO">Monto fijo</option>
                      <option value="PORCENTAJE">Porcentaje</option>
                    </select>
                  </div>

                  <div className="mt-5">
                    <label className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">
                      {(tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo) === "PORCENTAJE" ? "Porcentaje actual" : "Monto actual"}
                    </label>
                    <input
                      type="text"
                      inputMode="decimal"
                      value={form[item.categoriaPaquete] ?? ""}
                      onChange={(event) => {
                        setForm((current) => ({ ...current, [item.categoriaPaquete]: event.target.value }))
                      }}
                      className="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-300"
                      placeholder={(tipoCalculoForm[item.categoriaPaquete] ?? item.tipoCalculo) === "PORCENTAJE" ? "Ejemplo: 8.5" : "Ejemplo: 2500"}
                    />
                  </div>

                  <p className="mt-4 text-xs text-slate-500">Ultima edicion: {formatTimestamp(item.updatedAt)}</p>
                </article>
              ))}
            </div>
          ) : null}
        </section>

        <aside className="space-y-6">
          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Como impacta</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Efecto inmediato</h3>
            <div className="mt-6 space-y-3">
              <InfoCard text="Afecta el costo compuesto que usa la preview del revendedor." />
              <InfoCard text="Afecta ofertas internas y la construccion de split logico por item." />
              <InfoCard text="No requiere cambiar variables de entorno ni redeploy de la plataforma." />
            </div>
          </section>

          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Nota operativa</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Alcance de esta v1</h3>
            <p className="mt-4 text-sm leading-6 text-slate-600">
              En esta iteracion la configuracion es global por categoria de paquete. Mas adelante, si el negocio lo
              pide, podemos sumar overrides por proveedor, zona o temporada sin romper esta base.
            </p>
          </section>

          <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Categorias comerciales</p>
            <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Categorias de producto</h3>
            <p className="mt-4 text-sm leading-6 text-slate-600">
              Estas categorias se usan en el alta de productos proveedor. Si una categoria es sugerida por proveedor,
              queda visible aqui para activarla o corregirla.
            </p>

            <div className="mt-5 flex flex-col gap-3 sm:flex-row">
              <input
                type="text"
                value={nuevaCategoria}
                onChange={(event) => setNuevaCategoria(event.target.value)}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm outline-none transition focus:border-blue-300"
                placeholder="Ejemplo: Electrohogar"
              />
              <button
                type="button"
                className="rounded-xl bg-slate-950 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
                disabled={createCategoriaProducto.isPending}
                onClick={() => {
                  const nombre = nuevaCategoria.trim()
                  if (!nombre) {
                    setFeedback({ variant: "warning", message: "Escribe un nombre valido para crear la categoria." })
                    return
                  }
                  createCategoriaProducto.mutate(
                    { nombre, activa: true },
                    {
                      onSuccess: () => {
                        setNuevaCategoria("")
                        setFeedback({ variant: "success", message: "Categoria comercial creada correctamente." })
                      },
                      onError: () => {
                        setFeedback({ variant: "error", message: "No se pudo crear la categoria comercial." })
                      },
                    },
                  )
                }}
              >
                {createCategoriaProducto.isPending ? "Creando..." : "Agregar categoria"}
              </button>
            </div>

            {categoriasProductoQuery.isLoading ? (
              <div className="mt-4 text-sm text-slate-500">Cargando categorias...</div>
            ) : null}

            {categoriasProductoQuery.isError ? (
              <div className="mt-4">
                <FeedbackAlert variant="error" title="No se pudieron cargar las categorias">
                  Reintenta para revisar y administrar categorias comerciales.
                </FeedbackAlert>
              </div>
            ) : null}

            {!categoriasProductoQuery.isLoading && !categoriasProductoQuery.isError ? (
              <div className="mt-5 space-y-3">
                {(categoriasProductoQuery.data ?? []).map((categoria) => (
                  <article key={categoria.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-semibold text-slate-900">{categoria.nombre}</p>
                        <p className="mt-1 text-xs text-slate-500">slug: {categoria.slug}</p>
                        <p className="mt-1 text-xs text-slate-500">
                          {categoria.sugerida ? "Sugerida por proveedor" : "Categoria definida por admin"}
                        </p>
                      </div>
                      <button
                        type="button"
                        className={`rounded-full border px-4 py-2 text-xs font-semibold transition ${
                          categoria.activa
                            ? "border-red-300 text-red-700 hover:border-red-400 hover:bg-red-50"
                            : "border-emerald-300 text-emerald-700 hover:border-emerald-400 hover:bg-emerald-50"
                        }`}
                        disabled={updateCategoriaProducto.isPending}
                        onClick={() => {
                          updateCategoriaProducto.mutate(
                            {
                              id: categoria.id,
                              payload: { nombre: categoria.nombre, activa: !categoria.activa },
                            },
                            {
                              onSuccess: () => {
                                setFeedback({
                                  variant: "success",
                                  message: !categoria.activa
                                    ? "Categoria activada para uso proveedor."
                                    : "Categoria desactivada correctamente.",
                                })
                              },
                              onError: () => {
                                setFeedback({ variant: "error", message: "No se pudo actualizar la categoria." })
                              },
                            },
                          )
                        }}
                      >
                        {categoria.activa ? "Desactivar" : "Activar"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            ) : null}
          </section>
        </aside>
      </div>
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

function InfoCard({ text }: { text: string }) {
  return <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700">{text}</article>
}
