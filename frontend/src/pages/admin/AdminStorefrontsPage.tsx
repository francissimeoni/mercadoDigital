import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import axios from "axios"
import { useEffect, useMemo, useState } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useAdminProveedorStorefront,
  useAdminUsuarios,
  useAdminRevendedorMercadoPagoOAuthLink,
  useAdminRevendedores,
  useCreateAdminRevendedor,
  useUploadAdminStorefrontAsset,
  useUpsertAdminProveedorStorefront,
  useUpdateAdminUsuario,
  useUpdateAdminRevendedor,
  useUpsertAdminRevendedorMercadoPago,
  useUpsertAdminStorefront,
} from "../../hooks/useAdmin"
import type {
  AdminRevendedorMercadoPagoRequest,
  AdminRevendedorRequest,
  AdminRevendedorResponse,
  AdminStorefrontRequest,
  AdminUsuarioResponse,
} from "../../types/admin"

type EditorMessage = {
  variant: "success" | "error" | "info"
  title: string
  body: string
}

type OnboardingCheck = {
  label: string
  completed: boolean
  detail: string
}

type UploadAssetKind = "logo" | "hero" | "favicon"

const emptyRevendedorForm: AdminRevendedorRequest = {
  nombre: "",
  dominio: "",
  usuarioEmail: "",
  usuarioPassword: "",
  activo: true,
}

const emptyStorefrontForm: AdminStorefrontRequest = {
  dominioPublico: "",
  nombreComercial: "",
  logoUrl: "",
  faviconUrl: "",
  colorPrimario: "#0f172a",
  colorSecundario: "#e2e8f0",
  heroTitulo: "",
  heroSubtitulo: "",
  heroImagenUrl: "",
  emailComercial: "",
  telefonoComercial: "",
  direccionComercial: "",
  footerTexto: "",
  smtpHost: "",
  smtpPort: 587,
  smtpUsername: "",
  smtpPassword: "",
  smtpAuth: true,
  smtpStartTls: true,
  publicada: false,
}

const emptyMercadoPagoForm: AdminRevendedorMercadoPagoRequest = {
  mercadoPagoUserId: "",
  publicKey: "",
  accessToken: "",
  refreshToken: "",
  tokenExpiresAt: null,
  liveMode: false,
  scope: "",
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("es-AR")
}

function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return "No definido"
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-AR")
}

function toRevendedorForm(revendedor: AdminRevendedorResponse): AdminRevendedorRequest {
  return {
    nombre: revendedor.nombre ?? "",
    dominio: revendedor.dominio ?? "",
    usuarioEmail: revendedor.usuarioEmail ?? "",
    usuarioPassword: "",
    activo: revendedor.activo,
  }
}

function toStorefrontForm(revendedor: AdminRevendedorResponse): AdminStorefrontRequest {
  return {
    dominioPublico: revendedor.storefront?.dominioPublico ?? revendedor.dominio ?? "",
    nombreComercial: revendedor.storefront?.nombreComercial ?? revendedor.nombre ?? "",
    logoUrl: revendedor.storefront?.logoUrl ?? "",
    faviconUrl: revendedor.storefront?.faviconUrl ?? "",
    colorPrimario: revendedor.storefront?.colorPrimario ?? "#0f172a",
    colorSecundario: revendedor.storefront?.colorSecundario ?? "#e2e8f0",
    heroTitulo: revendedor.storefront?.heroTitulo ?? revendedor.storefront?.nombreComercial ?? revendedor.nombre ?? "",
    heroSubtitulo: revendedor.storefront?.heroSubtitulo ?? "",
    heroImagenUrl: revendedor.storefront?.heroImagenUrl ?? "",
    emailComercial: revendedor.storefront?.emailComercial ?? "",
    telefonoComercial: revendedor.storefront?.telefonoComercial ?? "",
    direccionComercial: revendedor.storefront?.direccionComercial ?? "",
    footerTexto: revendedor.storefront?.footerTexto ?? "",
    smtpHost: revendedor.storefront?.smtpHost ?? "",
    smtpPort: revendedor.storefront?.smtpPort ?? 587,
    smtpUsername: revendedor.storefront?.smtpUsername ?? "",
    smtpPassword: "",
    smtpAuth: revendedor.storefront?.smtpAuth ?? true,
    smtpStartTls: revendedor.storefront?.smtpStartTls ?? true,
    publicada: revendedor.storefront?.publicada ?? false,
  }
}

function toMercadoPagoForm(revendedor: AdminRevendedorResponse): AdminRevendedorMercadoPagoRequest {
  return {
    mercadoPagoUserId: revendedor.mercadoPago?.mercadoPagoUserId ?? "",
    publicKey: revendedor.mercadoPago?.publicKey ?? "",
    accessToken: "",
    refreshToken: "",
    tokenExpiresAt: revendedor.mercadoPago?.tokenExpiresAt ?? null,
    liveMode: revendedor.mercadoPago?.liveMode ?? false,
    scope: revendedor.mercadoPago?.scope ?? "",
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === "string") {
    return error.response.data.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

function normalizeColorValue(value: string, fallback: string) {
  return /^#[0-9a-f]{6}$/i.test(value.trim()) ? value.trim() : fallback
}

function resolveMercadoPagoStatusTone(status: string | null | undefined) {
  const normalized = status?.trim().toUpperCase()
  if (!normalized) {
    return "bg-slate-100 text-slate-700"
  }
  if (normalized.includes("VINCULADA") || normalized.includes("FALLBACK")) {
    return "bg-emerald-50 text-emerald-700"
  }
  if (normalized.includes("VENCIDA")) {
    return "bg-amber-50 text-amber-700"
  }
  return "bg-slate-100 text-slate-700"
}

function getStorefrontSimulationUrl(dominio: string | null | undefined) {
  const dominioNormalizado = dominio?.trim()
  if (!dominioNormalizado) {
    return null
  }

  const encodedHost = encodeURIComponent(dominioNormalizado)
  return `/?storefrontHost=${encodedHost}`
}

function buildOnboardingChecks(
  revendedorForm: AdminRevendedorRequest,
  storefrontForm: AdminStorefrontRequest,
  usuarioConfigurado: boolean,
  smtpConfigurado: boolean,
  mercadoPago: AdminRevendedorResponse["mercadoPago"] | null | undefined,
  storefrontSimulationUrl: string | null,
): OnboardingCheck[] {
  return [
    {
      label: "Revendedor activo",
      completed: revendedorForm.activo,
      detail: revendedorForm.activo
        ? "La entidad comercial esta habilitada."
        : "Activalo para poder publicar la tienda.",
    },
    {
      label: "Dominio configurado",
      completed: Boolean((storefrontForm.dominioPublico ?? revendedorForm.dominio).trim()),
      detail: (storefrontForm.dominioPublico ?? revendedorForm.dominio).trim()
        ? (storefrontForm.dominioPublico ?? revendedorForm.dominio).trim()
        : "Falta el dominio comercial del storefront.",
    },
    {
      label: "Acceso revendedor",
      completed: Boolean(revendedorForm.usuarioEmail.trim() && usuarioConfigurado),
      detail: revendedorForm.usuarioEmail.trim()
        ? usuarioConfigurado
          ? `Usuario listo: ${revendedorForm.usuarioEmail.trim()}`
          : "Hay email de acceso cargado, pero falta confirmar una password operativa."
        : "Conviene crear el usuario del revendedor para operar login real.",
    },
    {
      label: "Nombre comercial",
      completed: Boolean(storefrontForm.nombreComercial.trim()),
      detail: storefrontForm.nombreComercial.trim()
        ? storefrontForm.nombreComercial.trim()
        : "Define el nombre visible de la tienda.",
    },
    {
      label: "Hero listo",
      completed: Boolean(storefrontForm.heroTitulo.trim() && storefrontForm.heroSubtitulo.trim()),
      detail:
        storefrontForm.heroTitulo.trim() && storefrontForm.heroSubtitulo.trim()
          ? "La portada ya tiene titulo y subtitulo comercial."
          : "Completa el titulo y subtitulo principal para que la portada no quede generica.",
    },
    {
      label: "Contacto y mails",
      completed: Boolean(
        storefrontForm.emailComercial.trim() || storefrontForm.telefonoComercial.trim() || storefrontForm.direccionComercial.trim(),
      ),
      detail:
        storefrontForm.emailComercial.trim() || storefrontForm.telefonoComercial.trim() || storefrontForm.direccionComercial.trim()
          ? "Ya existe al menos un punto de contacto visible."
          : "Conviene definir email, telefono o direccion antes de mostrar la tienda.",
    },
    {
      label: "SMTP storefront",
      completed: Boolean(storefrontForm.smtpHost.trim() && storefrontForm.smtpUsername.trim() && (storefrontForm.smtpPassword.trim() || smtpConfigurado)),
      detail:
        storefrontForm.smtpHost.trim() && storefrontForm.smtpUsername.trim() && (storefrontForm.smtpPassword.trim() || smtpConfigurado)
          ? "La tienda ya tiene un transporte SMTP propio listo para mails reales."
          : "Si quieres mails reales por tienda, completa host, usuario y password SMTP.",
    },
    {
      label: "Publicacion",
      completed: storefrontForm.publicada,
      detail: storefrontForm.publicada
        ? "La storefront esta marcada como publicada."
        : "Activa la publicacion cuando termines la configuracion base.",
    },
    {
      label: "Coordinacion de compra",
      completed: !MERCADO_PAGO_DISPONIBLE || Boolean(mercadoPago?.readyForCheckout),
      detail: !MERCADO_PAGO_DISPONIBLE ? "Los pagos se coordinan directamente con la tienda, fuera de MercadoDigital." : mercadoPago?.readyForCheckout
        ? mercadoPago.usaCredencialesGlobalesFallback
          ? "La tienda puede cobrar usando las credenciales globales de fallback hasta completar la vinculacion propia."
          : `La cuenta del revendedor ya esta lista para Checkout Pro (${mercadoPago.estado}).`
        : "Falta cargar o vincular una cuenta Mercado Pago operativa para cerrar el onboarding comercial.",
    },
    {
      label: "Validacion local",
      completed: Boolean(storefrontSimulationUrl),
      detail: storefrontSimulationUrl
        ? "Ya puedes abrir el storefront simulado y revisar la experiencia publica."
        : "Primero guarda un dominio valido para habilitar la simulacion local.",
    },
  ]
}

function InfoSummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-4 text-4xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-2 break-words text-sm font-medium text-slate-800">{value}</p>
    </article>
  )
}

function PreviewCell({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-2 break-words font-medium text-slate-800">{value}</p>
    </article>
  )
}

function OnboardingCheckRow({ check }: { check: OnboardingCheck }) {
  return (
    <article
      className={`rounded-2xl border px-4 py-4 ${
        check.completed ? "border-emerald-200 bg-emerald-50" : "border-amber-200 bg-amber-50"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">{check.label}</p>
          <p className="mt-1 text-sm leading-5 text-slate-600">{check.detail}</p>
        </div>
        <span
          className={`rounded-full px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.2em] ${
            check.completed ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"
          }`}
        >
          {check.completed ? "OK" : "Pendiente"}
        </span>
      </div>
    </article>
  )
}

function AssetUploadField({
  label,
  value,
  placeholder,
  buttonLabel,
  uploading,
  onChange,
  onUpload,
}: {
  label: string
  value: string
  placeholder: string
  buttonLabel: string
  uploading: boolean
  onChange: (value: string) => void
  onUpload: (file: File) => void
}) {
  return (
    <label className="space-y-2">
      <span className="text-sm font-medium text-slate-700">{label}</span>
      <input
        className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
      <div className="flex flex-wrap items-center gap-3">
        <label className="inline-flex cursor-pointer items-center rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-500 hover:text-slate-950">
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp,image/svg+xml,image/gif"
            className="hidden"
            onChange={(event) => {
              const file = event.target.files?.[0]
              if (file) {
                onUpload(file)
              }
              event.target.value = ""
            }}
          />
          {uploading ? "Subiendo..." : buttonLabel}
        </label>
        {value ? <span className="text-xs text-slate-500">Asset cargado</span> : <span className="text-xs text-slate-500">Puedes pegar URL o subir archivo</span>}
      </div>
    </label>
  )
}

export default function AdminStorefrontsPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [search, setSearch] = useState("")
  const [filter, setFilter] = useState<"TODAS" | "PUBLICADAS" | "BORRADOR">("TODAS")
  const [selectedRevendedorId, setSelectedRevendedorId] = useState<string | null>(null)
  const [revendedorForm, setRevendedorForm] = useState<AdminRevendedorRequest>(emptyRevendedorForm)
  const [storefrontForm, setStorefrontForm] = useState<AdminStorefrontRequest>(emptyStorefrontForm)
  const [mercadoPagoForm, setMercadoPagoForm] = useState<AdminRevendedorMercadoPagoRequest>(emptyMercadoPagoForm)
  const [createForm, setCreateForm] = useState<AdminRevendedorRequest>(emptyRevendedorForm)
  const [editorMessage, setEditorMessage] = useState<EditorMessage | null>(null)
  const [viewMode, setViewMode] = useState<"ALTA" | "EDICION">("ALTA")
  const adminUsuariosQuery = useAdminUsuarios()
  const revendedoresQuery = useAdminRevendedores()
  const createRevendedor = useCreateAdminRevendedor()
  const updateRevendedor = useUpdateAdminRevendedor()
  const updateUsuario = useUpdateAdminUsuario()
  const upsertMercadoPago = useUpsertAdminRevendedorMercadoPago()
  const mercadoPagoOAuthLink = useAdminRevendedorMercadoPagoOAuthLink()
  const upsertStorefront = useUpsertAdminStorefront()
  const upsertProveedorStorefront = useUpsertAdminProveedorStorefront()
  const uploadStorefrontAsset = useUploadAdminStorefrontAsset()
  const revendedores = revendedoresQuery.data ?? []
  const selectedProveedorId = useMemo(() => new URLSearchParams(location.search).get("proveedorId"), [location.search])
  const proveedores = useMemo(
    () =>
      (adminUsuariosQuery.data ?? []).filter(
        (usuario): usuario is AdminUsuarioResponse =>
          usuario.tipoUsuario === "PROVEEDOR" && Boolean(usuario.proveedorId),
      ),
    [adminUsuariosQuery.data],
  )
  const selectedProveedor = useMemo(
    () => proveedores.find((item) => item.proveedorId === selectedProveedorId) ?? null,
    [proveedores, selectedProveedorId],
  )
  const isProviderMode = Boolean(selectedProveedor?.proveedorId)
  const proveedorStorefrontQuery = useAdminProveedorStorefront(selectedProveedor?.proveedorId ?? null)
  const [uploadingAssetKind, setUploadingAssetKind] = useState<UploadAssetKind | null>(null)

  const storefronts = useMemo(
    () => revendedores.map((revendedor) => ({ revendedor, storefront: revendedor.storefront })),
    [revendedores],
  )
  const publicadas = storefronts.filter((item) => item.storefront?.publicada)
  const borrador = storefronts.filter((item) => !item.storefront?.publicada)
  const proveedoresFiltrados = useMemo(
    () =>
      proveedores.filter((item) => {
        const haystack = `${item.nombre} ${item.email}`.toLowerCase()
        return haystack.includes(search.toLowerCase())
      }),
    [proveedores, search],
  )
  const filtradas = useMemo(
    () =>
      storefronts.filter((item) => {
        if (filter === "PUBLICADAS" && !item.storefront?.publicada) return false
        if (filter === "BORRADOR" && item.storefront?.publicada) return false
        const haystack =
          `${item.revendedor.nombre} ${item.revendedor.dominio ?? ""} ${item.storefront?.nombreComercial ?? ""}`.toLowerCase()
        return haystack.includes(search.toLowerCase())
      }),
    [filter, search, storefronts],
  )
  const selectedRevendedor = useMemo(
    () => revendedores.find((revendedor) => revendedor.id === selectedRevendedorId) ?? null,
    [revendedores, selectedRevendedorId],
  )

  useEffect(() => {
    if (selectedProveedorId) {
      setSelectedRevendedorId(null)
      return
    }
    if (revendedores.length === 0) {
      setSelectedRevendedorId(null)
      setRevendedorForm(emptyRevendedorForm)
      setStorefrontForm(emptyStorefrontForm)
      setMercadoPagoForm(emptyMercadoPagoForm)
      return
    }

    const params = new URLSearchParams(location.search)
    const requestedRevendedorId = params.get("revendedorId")
    const requestedExists =
      requestedRevendedorId && revendedores.some((revendedor) => revendedor.id === requestedRevendedorId)
    if (requestedExists) {
      setSelectedRevendedorId(requestedRevendedorId)
      return
    }

    const stillExists = selectedRevendedorId && revendedores.some((revendedor) => revendedor.id === selectedRevendedorId)
    setSelectedRevendedorId(stillExists ? selectedRevendedorId : revendedores[0].id)
  }, [location.search, revendedores, selectedProveedorId, selectedRevendedorId])

  useEffect(() => {
    if (!selectedRevendedor) {
      return
    }
    setRevendedorForm(toRevendedorForm(selectedRevendedor))
    setStorefrontForm(toStorefrontForm(selectedRevendedor))
    setMercadoPagoForm(toMercadoPagoForm(selectedRevendedor))
  }, [selectedRevendedor])

  useEffect(() => {
    if (!selectedProveedorId || !proveedorStorefrontQuery.data) {
      return
    }
    const proveedorStorefront = proveedorStorefrontQuery.data
    setStorefrontForm({
      dominioPublico: proveedorStorefront.dominioPublico ?? "",
      nombreComercial: proveedorStorefront.nombreComercial ?? selectedProveedor?.nombre ?? "",
      logoUrl: proveedorStorefront.logoUrl ?? "",
      faviconUrl: proveedorStorefront.faviconUrl ?? "",
      colorPrimario: proveedorStorefront.colorPrimario ?? "#0f172a",
      colorSecundario: proveedorStorefront.colorSecundario ?? "#e2e8f0",
      heroTitulo: proveedorStorefront.heroTitulo ?? proveedorStorefront.nombreComercial ?? selectedProveedor?.nombre ?? "",
      heroSubtitulo: proveedorStorefront.heroSubtitulo ?? "",
      heroImagenUrl: proveedorStorefront.heroImagenUrl ?? "",
      emailComercial: proveedorStorefront.emailComercial ?? selectedProveedor?.email ?? "",
      telefonoComercial: proveedorStorefront.telefonoComercial ?? "",
      direccionComercial: proveedorStorefront.direccionComercial ?? "",
      footerTexto: proveedorStorefront.footerTexto ?? "",
      smtpHost: proveedorStorefront.smtpHost ?? "",
      smtpPort: proveedorStorefront.smtpPort ?? 587,
      smtpUsername: proveedorStorefront.smtpUsername ?? "",
      smtpPassword: "",
      smtpAuth: proveedorStorefront.smtpAuth ?? true,
      smtpStartTls: proveedorStorefront.smtpStartTls ?? true,
      publicada: proveedorStorefront.publicada ?? false,
    })
  }, [proveedorStorefrontQuery.data, selectedProveedor?.email, selectedProveedor?.nombre, selectedProveedorId])

  useEffect(() => {
    const params = new URLSearchParams(location.search)
    if (!MERCADO_PAGO_DISPONIBLE) return
    const status = params.get("mp_oauth_status")
    const message = params.get("mp_message")
    const subjectType = params.get("mp_subject_type")
    const subjectId = params.get("mp_subject_id")
    const revendedorId =
      (subjectType === "REVENDEDOR" ? subjectId : null) ??
      params.get("mp_revendedor_id") ??
      params.get("revendedorId")

    if (revendedorId) {
      setSelectedRevendedorId(revendedorId)
    }

    if (!status) {
      return
    }

    setEditorMessage({
      variant: status === "success" ? "success" : "error",
      title: status === "success" ? "Mercado Pago vinculado" : "Vinculacion Mercado Pago incompleta",
      body: message ?? (status === "success" ? "La cuenta se vinculo correctamente." : "No se pudo completar el proceso OAuth."),
    })

    navigate(revendedorId ? `/admin/storefronts?revendedorId=${revendedorId}` : "/admin/storefronts", { replace: true })
  }, [location.search, navigate])

  const handleCreateRevendedor = async () => {
    setEditorMessage(null)
    try {
      const created = await createRevendedor.mutateAsync(createForm)
      setCreateForm(emptyRevendedorForm)
      setViewMode("EDICION")
      setSelectedRevendedorId(created.id)
      setEditorMessage({
        variant: "success",
        title: "Revendedor creado",
        body: `Se dio de alta ${created.nombre} y ya quedo disponible para configurar su storefront.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo crear el revendedor",
        body: resolveErrorMessage(error, "Ocurrio un error al crear el revendedor."),
      })
    }
  }

  const handleToggleRevendedorActivo = async (revendedor: AdminRevendedorResponse) => {
    setEditorMessage(null)
    try {
      await updateRevendedor.mutateAsync({
        id: revendedor.id,
        payload: {
          nombre: revendedor.nombre,
          dominio: revendedor.dominio ?? "",
          usuarioEmail: revendedor.usuarioEmail ?? "",
          usuarioPassword: "",
          activo: !revendedor.activo,
        },
      })
      setEditorMessage({
        variant: "success",
        title: !revendedor.activo ? "Revendedor activado" : "Revendedor desactivado",
        body: `${revendedor.nombre} ahora quedo ${!revendedor.activo ? "activo" : "inactivo"}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo actualizar el estado",
        body: resolveErrorMessage(error, "Ocurrio un error al actualizar el estado del revendedor."),
      })
    }
  }

  const handleToggleProveedorActivo = async (proveedor: AdminUsuarioResponse) => {
    if (!proveedor.proveedorId) return
    setEditorMessage(null)
    try {
      await updateUsuario.mutateAsync({
        id: proveedor.id,
        payload: {
          nombre: proveedor.nombre,
          email: proveedor.email,
          tipoUsuario: "PROVEEDOR",
          activo: !proveedor.activo,
          nombreEntidad: proveedor.nombre,
          dominioRevendedor: "",
          proveedorDespachante: proveedor.proveedorDespachante ?? false,
        },
      })
      setEditorMessage({
        variant: "success",
        title: !proveedor.activo ? "Proveedor activado" : "Proveedor desactivado",
        body: `${proveedor.nombre} ahora quedo ${!proveedor.activo ? "activo" : "inactivo"}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo actualizar el estado",
        body: resolveErrorMessage(error, "Ocurrio un error al actualizar el estado del proveedor."),
      })
    }
  }

  const handleSelectRevendedor = (revendedorId: string) => {
    setViewMode("EDICION")
    setSelectedRevendedorId(revendedorId)
    navigate(`/admin/storefronts?revendedorId=${revendedorId}`, { replace: true })
  }

  const handleSelectProveedor = (proveedorId: string) => {
    setViewMode("EDICION")
    setSelectedRevendedorId(null)
    navigate(`/admin/storefronts?proveedorId=${proveedorId}`, { replace: true })
  }

  const handleSaveRevendedor = async () => {
    if (!selectedRevendedor) return
    setEditorMessage(null)
    try {
      const updated = await updateRevendedor.mutateAsync({ id: selectedRevendedor.id, payload: revendedorForm })
      setRevendedorForm((current) => ({ ...current, usuarioPassword: "" }))
      setSelectedRevendedorId(updated.id)
      setEditorMessage({
        variant: "success",
        title: "Revendedor actualizado",
        body: `La identidad comercial de ${updated.nombre} quedo guardada correctamente.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo actualizar el revendedor",
        body: resolveErrorMessage(error, "Ocurrio un error al actualizar el revendedor."),
      })
    }
  }

  const handleSaveStorefront = async () => {
    if (selectedProveedor?.proveedorId) {
      setEditorMessage(null)
      try {
        await upsertProveedorStorefront.mutateAsync({ proveedorId: selectedProveedor.proveedorId, payload: storefrontForm })
        setStorefrontForm((current) => ({ ...current, smtpPassword: "" }))
        setEditorMessage({
          variant: "success",
          title: storefrontForm.publicada ? "Storefront proveedor publicada" : "Storefront proveedor guardada",
          body: `Se guardo la configuracion de ${storefrontForm.nombreComercial || selectedProveedor.nombre}.`,
        })
      } catch (error) {
        setEditorMessage({
          variant: "error",
          title: "No se pudo guardar la storefront de proveedor",
          body: resolveErrorMessage(error, "Ocurrio un error al guardar la storefront de proveedor."),
        })
      }
      return
    }
    if (!selectedRevendedor) return
    setEditorMessage(null)
    try {
      const updated = await upsertStorefront.mutateAsync({ id: selectedRevendedor.id, payload: storefrontForm })
      setStorefrontForm((current) => ({ ...current, smtpPassword: "" }))
      setSelectedRevendedorId(updated.id)
      setEditorMessage({
        variant: "success",
        title: storefrontForm.publicada ? "Storefront publicada" : "Storefront guardada",
        body: storefrontForm.publicada
          ? `La tienda ${storefrontForm.nombreComercial || updated.nombre} ya quedo lista como storefront visible.`
          : `Se guardo la configuracion base de ${storefrontForm.nombreComercial || updated.nombre}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo guardar la storefront",
        body: resolveErrorMessage(error, "Ocurrio un error al guardar la storefront."),
      })
    }
  }

  const handleSaveMercadoPago = async () => {
    if (!selectedRevendedor) return
    setEditorMessage(null)
    try {
      const updated = await upsertMercadoPago.mutateAsync({ id: selectedRevendedor.id, payload: mercadoPagoForm })
      setMercadoPagoForm((current) => ({
        ...current,
        accessToken: "",
        refreshToken: "",
        tokenExpiresAt: updated.tokenExpiresAt,
      }))
      setEditorMessage({
        variant: "success",
        title: "Mercado Pago actualizado",
        body: updated.readyForCheckout
          ? `La tienda ${selectedRevendedor.nombre} ya quedo lista para cobrar con Checkout Pro.`
          : `Se guardo la configuracion base de Mercado Pago para ${selectedRevendedor.nombre}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo guardar Mercado Pago",
        body: resolveErrorMessage(error, "Ocurrio un error al guardar la configuracion de Mercado Pago."),
      })
    }
  }

  const handleGenerateOAuthLink = async () => {
    if (!selectedRevendedor) return
    setEditorMessage(null)
    try {
      const response = await mercadoPagoOAuthLink.mutateAsync(selectedRevendedor.id)
      void navigator.clipboard?.writeText(response.authorizationUrl).catch(() => undefined)
      window.open(response.authorizationUrl, "_blank", "noopener,noreferrer")
      setEditorMessage({
        variant: "info",
        title: "Link OAuth generado",
        body: `Se abrio la autorizacion de Mercado Pago y el link tambien quedo listo para compartir con el revendedor. Expira: ${formatDateTime(
          response.expiresAt,
        )}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo generar el link OAuth",
        body: resolveErrorMessage(error, "Ocurrio un error al generar la solicitud de autorizacion de Mercado Pago."),
      })
    }
  }

  const handleUploadAsset = async (tipo: UploadAssetKind, file: File) => {
    setEditorMessage(null)
    setUploadingAssetKind(tipo)
    try {
      const uploaded = await uploadStorefrontAsset.mutateAsync({ archivo: file, tipo })
      setStorefrontForm((current) => {
        if (tipo === "logo") {
          return {
            ...current,
            logoUrl: uploaded.url,
            faviconUrl: current.faviconUrl || uploaded.url,
          }
        }
        if (tipo === "favicon") {
          return {
            ...current,
            faviconUrl: uploaded.url,
          }
        }
        return {
          ...current,
          heroImagenUrl: uploaded.url,
        }
      })
      setEditorMessage({
        variant: "success",
        title: "Imagen cargada",
        body: `El archivo se subio correctamente y ya quedo listo para guardarse en la storefront como ${tipo}.`,
      })
    } catch (error) {
      setEditorMessage({
        variant: "error",
        title: "No se pudo subir la imagen",
        body: resolveErrorMessage(error, "Ocurrio un error al subir el asset de storefront."),
      })
    } finally {
      setUploadingAssetKind(null)
    }
  }

  const accentPreview = storefrontForm.colorPrimario || "#0f172a"
  const secondaryPreview = storefrontForm.colorSecundario || "#e2e8f0"
  const storefrontNamePreview = storefrontForm.nombreComercial || revendedorForm.nombre || "Nueva storefront"
  const storefrontSimulationUrl = getStorefrontSimulationUrl(storefrontForm.dominioPublico || revendedorForm.dominio)
  const mercadoPagoStatus = selectedRevendedor?.mercadoPago ?? null
  const onboardingChecks = useMemo(
    () =>
      buildOnboardingChecks(
        revendedorForm,
        storefrontForm,
        selectedRevendedor?.usuarioConfigurado ?? false,
        selectedRevendedor?.storefront?.smtpConfigured ?? false,
        mercadoPagoStatus,
        storefrontSimulationUrl,
      ),
    [mercadoPagoStatus, revendedorForm, selectedRevendedor?.storefront?.smtpConfigured, selectedRevendedor?.usuarioConfigurado, storefrontForm, storefrontSimulationUrl],
  )
  const onboardingCompletedCount = onboardingChecks.filter((check) => check.completed).length
  const onboardingPendingCount = onboardingChecks.length - onboardingCompletedCount
  const isOnboardingReady = onboardingPendingCount === 0
  const sectionIdentidadReady = Boolean(
    (storefrontForm.dominioPublico || revendedorForm.dominio).trim() &&
      (!isProviderMode ? revendedorForm.nombre.trim() && revendedorForm.usuarioEmail.trim() : selectedProveedor?.nombre?.trim()),
  )
  const sectionBrandingReady = Boolean(
    storefrontForm.nombreComercial.trim() && storefrontForm.heroTitulo.trim() && storefrontForm.heroSubtitulo.trim(),
  )
  const sectionContactoReady = Boolean(
    storefrontForm.emailComercial.trim() || storefrontForm.telefonoComercial.trim() || storefrontForm.direccionComercial.trim(),
  )
  const sectionSmtpReady = Boolean(
    storefrontForm.smtpHost.trim() &&
      storefrontForm.smtpUsername.trim() &&
      (storefrontForm.smtpPassword.trim() || selectedRevendedor?.storefront?.smtpConfigured || proveedorStorefrontQuery.data?.smtpConfigured),
  )

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Storefronts</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Gestion real de revendedores y storefronts white-label. Desde aca puedes dar de alta tiendas, editar dominio,
              branding base y estado de publicacion usando el backend existente como fuente de verdad.
            </p>
          </div>
          <div className="rounded-3xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
            {storefronts.length} tiendas administrables
          </div>
        </div>
      </header>

      {editorMessage ? (
        <FeedbackAlert variant={editorMessage.variant} title={editorMessage.title}>
          {editorMessage.body}
        </FeedbackAlert>
      ) : null}

      <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setViewMode("ALTA")}
            className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] ${
              viewMode === "ALTA" ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-700"
            }`}
          >
            Alta comercial
          </button>
          <button
            type="button"
            onClick={() => setViewMode("EDICION")}
            className={`rounded-full px-4 py-2 text-xs font-semibold uppercase tracking-[0.18em] ${
              viewMode === "EDICION" ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-700"
            }`}
          >
            Edicion
          </button>
        </div>
      </section>

      {viewMode === "ALTA" ? (
      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Alta comercial</p>
          <h3 className="mt-2 text-2xl font-semibold text-slate-950">Crear nuevo revendedor</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Primero se crea la entidad comercial. Despues puedes completar el storefront, branding y publicacion.
          </p>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Nombre</span>
            <input
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={createForm.nombre}
              onChange={(event) => setCreateForm((current) => ({ ...current, nombre: event.target.value }))}
              placeholder="Navarro Ventas"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Dominio</span>
            <input
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={createForm.dominio}
              onChange={(event) => setCreateForm((current) => ({ ...current, dominio: event.target.value }))}
              placeholder="www.navarroventas.com"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Usuario revendedor</span>
            <input
              type="email"
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={createForm.usuarioEmail}
              onChange={(event) => setCreateForm((current) => ({ ...current, usuarioEmail: event.target.value }))}
              placeholder="revendedor@tu-tienda.com"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Password inicial</span>
            <input
              type="password"
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={createForm.usuarioPassword}
              onChange={(event) => setCreateForm((current) => ({ ...current, usuarioPassword: event.target.value }))}
              placeholder="Minimo operativo para login real"
            />
          </label>
          <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-slate-300"
              checked={createForm.activo}
              onChange={(event) => setCreateForm((current) => ({ ...current, activo: event.target.checked }))}
            />
            Revendedor activo desde el alta
          </label>
        </div>

        <div className="mt-5">
          <button
            type="button"
            className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            onClick={() => void handleCreateRevendedor()}
            disabled={createRevendedor.isPending}
          >
            {createRevendedor.isPending ? "Creando revendedor..." : "Crear revendedor"}
          </button>
        </div>
      </section>
      ) : null}

      {viewMode === "EDICION" ? (
      <>
      <div className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <section className="space-y-4">
          <div className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
              <input
                className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
                placeholder="Buscar por nombre comercial, revendedor o dominio"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <div className="flex flex-wrap gap-2">
                {(["TODAS", "PUBLICADAS", "BORRADOR"] as const).map((option) => (
                  <button
                    key={option}
                    type="button"
                    onClick={() => setFilter(option)}
                    className={`rounded-full px-3 py-2 text-xs font-semibold uppercase tracking-[0.2em] transition ${
                      filter === option ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    {option}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1">
            {revendedoresQuery.isLoading
              ? Array.from({ length: 4 }).map((_, index) => (
                  <div key={index} className="h-40 animate-pulse rounded-[2rem] border border-slate-200 bg-slate-200" />
                ))
              : null}

            {revendedoresQuery.isError ? (
              <section className="rounded-[2rem] border border-red-200 bg-red-50 p-6 shadow-sm">
                <h3 className="text-lg font-semibold text-red-900">No se pudo cargar la gestion de storefronts</h3>
                <button
                  type="button"
                  className="mt-4 rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
                  onClick={() => {
                    void revendedoresQuery.refetch()
                  }}
                >
                  Reintentar
                </button>
              </section>
            ) : null}

            {!revendedoresQuery.isLoading && !revendedoresQuery.isError && filtradas.length === 0 ? (
              <section className="rounded-[2rem] border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
                No hay revendedores que coincidan con el filtro actual.
              </section>
            ) : null}

            {!revendedoresQuery.isLoading &&
              !revendedoresQuery.isError &&
              filtradas.map(({ revendedor, storefront }) => {
                const isSelected = revendedor.id === selectedRevendedorId

                return (
                  <article
                    key={revendedor.id}
                    className={`rounded-[2rem] border bg-white p-6 shadow-sm transition ${
                      isSelected ? "border-blue-500 bg-blue-50/40 ring-2 ring-blue-500/20" : "border-slate-200"
                    }`}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Storefront</p>
                        <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">
                          {storefront?.nombreComercial ?? revendedor.nombre}
                        </h3>
                        <p className="mt-1 text-sm text-slate-500">{revendedor.dominio ?? "Sin dominio configurado"}</p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {isSelected ? (
                          <span className="rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-white">
                            Seleccionada
                          </span>
                        ) : null}
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                            storefront?.publicada ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
                          }`}
                        >
                          {storefront?.publicada ? "Publicada" : "Borrador"}
                        </span>
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                            revendedor.activo ? "bg-blue-50 text-blue-700" : "bg-slate-200 text-slate-600"
                          }`}
                        >
                          {revendedor.activo ? "Activo" : "Inactivo"}
                        </span>
                      </div>
                    </div>

                    <div className="mt-5 grid gap-3 md:grid-cols-2">
                      <InfoCell label="Nombre comercial" value={storefront?.nombreComercial ?? "Sin branding"} />
                      <InfoCell label="Ultima actualizacion" value={formatDate(storefront?.updatedAt ?? revendedor.updatedAt)} />
                      <InfoCell label="Email comercial / remitente" value={storefront?.emailComercial ?? "Sin email"} />
                      <InfoCell label="Usuario revendedor" value={revendedor.usuarioEmail ?? "Sin acceso"} />
                      <InfoCell label="Color primario" value={storefront?.colorPrimario ?? "No definido"} />
                      <InfoCell
                        label="Cobros en plataforma"
                        value={
                          !MERCADO_PAGO_DISPONIBLE ? "Deshabilitados" : revendedor.mercadoPago?.readyForCheckout
                            ? revendedor.mercadoPago.usaCredencialesGlobalesFallback
                              ? "Fallback global"
                              : revendedor.mercadoPago.estado
                            : "Pendiente"
                        }
                      />
                    </div>

                    <div className="mt-5">
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                            isSelected ? "bg-slate-950 text-white" : "border border-slate-300 text-slate-700 hover:border-slate-400"
                          }`}
                          onClick={() => handleSelectRevendedor(revendedor.id)}
                        >
                          {isSelected ? "Editando esta tienda" : "Editar"}
                        </button>
                        <button
                          type="button"
                          className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                            revendedor.activo
                              ? "border border-rose-300 text-rose-700 hover:bg-rose-50"
                              : "border border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                          }`}
                          onClick={() => void handleToggleRevendedorActivo(revendedor)}
                          disabled={updateRevendedor.isPending}
                        >
                          {revendedor.activo ? "Dar de baja" : "Reactivar"}
                        </button>
                      </div>
                    </div>
                  </article>
                )
              })}

            {!adminUsuariosQuery.isLoading &&
              proveedoresFiltrados.map((proveedor) => {
                const isSelected = proveedor.proveedorId === selectedProveedorId
                return (
                  <article
                    key={proveedor.id}
                    className={`rounded-[2rem] border bg-white p-6 shadow-sm transition ${
                      isSelected ? "border-blue-500 bg-blue-50/40 ring-2 ring-blue-500/20" : "border-slate-200"
                    }`}
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Storefront proveedor</p>
                        <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">{proveedor.nombre}</h3>
                        <p className="mt-1 text-sm text-slate-500">{proveedor.email}</p>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {isSelected ? (
                          <span className="rounded-full bg-blue-600 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-white">
                            Seleccionada
                          </span>
                        ) : null}
                        <span
                          className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                            proveedor.activo ? "bg-blue-50 text-blue-700" : "bg-slate-200 text-slate-600"
                          }`}
                        >
                          {proveedor.activo ? "Activo" : "Inactivo"}
                        </span>
                      </div>
                    </div>
                    <div className="mt-5">
                      <div className="flex flex-wrap gap-2">
                        <button
                          type="button"
                          className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                            isSelected ? "bg-slate-950 text-white" : "border border-slate-300 text-slate-700 hover:border-slate-400"
                          }`}
                          onClick={() => {
                            if (proveedor.proveedorId) {
                              handleSelectProveedor(proveedor.proveedorId)
                            }
                          }}
                        >
                          {isSelected ? "Editando proveedor" : "Editar"}
                        </button>
                        <button
                          type="button"
                          className={`rounded-full px-4 py-2 text-sm font-semibold transition ${
                            proveedor.activo
                              ? "border border-rose-300 text-rose-700 hover:bg-rose-50"
                              : "border border-emerald-300 text-emerald-700 hover:bg-emerald-50"
                          }`}
                          onClick={() => void handleToggleProveedorActivo(proveedor)}
                          disabled={updateUsuario.isPending}
                        >
                          {proveedor.activo ? "Dar de baja" : "Reactivar"}
                        </button>
                      </div>
                    </div>
                  </article>
                )
              })}
          </div>
        </section>

        <section className="space-y-6">
          {selectedRevendedor || selectedProveedor ? (
            <>
              {!isProviderMode ? (
              <details
                open
                className={`rounded-[2rem] border p-6 shadow-sm ${sectionBrandingReady ? "border-emerald-200 bg-emerald-50/30" : "border-slate-200 bg-white"}`}
              >
                <summary className="mb-4 cursor-pointer list-none">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">Branding y publicacion</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${sectionBrandingReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {sectionBrandingReady ? "Listo" : "Pendiente"}
                    </span>
                  </div>
                </summary>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Revendedor</p>
                    <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{selectedRevendedor?.nombre}</h3>
                    <p className="mt-2 text-sm text-slate-500">Edita la identidad base de la tienda antes de ajustar el branding.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
                    <div className="flex items-center gap-2">
                      <span>ID {selectedRevendedor?.id}</span>
                      <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] ${sectionIdentidadReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                        {sectionIdentidadReady ? "Identidad lista" : "Identidad pendiente"}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-2">
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Nombre</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={revendedorForm.nombre}
                      onChange={(event) => setRevendedorForm((current) => ({ ...current, nombre: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Dominio</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={revendedorForm.dominio}
                      onChange={(event) => setRevendedorForm((current) => ({ ...current, dominio: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Usuario revendedor</span>
                    <input
                      type="email"
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={revendedorForm.usuarioEmail}
                      onChange={(event) => setRevendedorForm((current) => ({ ...current, usuarioEmail: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Nueva password</span>
                    <input
                      type="password"
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={revendedorForm.usuarioPassword}
                      onChange={(event) => setRevendedorForm((current) => ({ ...current, usuarioPassword: event.target.value }))}
                      placeholder={selectedRevendedor?.usuarioConfigurado ? "Dejar vacio para conservar la actual" : "Define una password inicial"}
                    />
                  </label>
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={revendedorForm.activo}
                      onChange={(event) => setRevendedorForm((current) => ({ ...current, activo: event.target.checked }))}
                    />
                    Revendedor activo
                  </label>
                </div>

                <div className="mt-5">
                  <button
                    type="button"
                    className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                    onClick={() => void handleSaveRevendedor()}
                    disabled={updateRevendedor.isPending}
                  >
                    {updateRevendedor.isPending ? "Guardando revendedor..." : "Guardar revendedor"}
                  </button>
                </div>
              </details>
              ) : (
                <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Proveedor</p>
                      <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{selectedProveedor?.nombre}</h3>
                      <p className="mt-2 text-sm text-slate-500">Gestion storefront tipo proveedor con catalogo propio.</p>
                    </div>
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
                      <div className="flex items-center gap-2">
                        <span>ID {selectedProveedor?.proveedorId}</span>
                        <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.15em] ${sectionIdentidadReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                          {sectionIdentidadReady ? "Identidad lista" : "Identidad pendiente"}
                        </span>
                      </div>
                    </div>
                  </div>
                </section>
              )}

              <details
                open
                className={`rounded-[2rem] border p-6 shadow-sm ${sectionIdentidadReady ? "border-emerald-200 bg-emerald-50/30" : "border-slate-200 bg-white"}`}
              >
                <summary className="mb-4 cursor-pointer list-none">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">Identidad de la tienda</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${sectionIdentidadReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {sectionIdentidadReady ? "Listo" : "Pendiente"}
                    </span>
                  </div>
                </summary>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Branding</p>
                    <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Configurar storefront</h3>
                    <p className="mt-2 text-sm text-slate-500">
                      Define nombre comercial, textos, colores, contactos y publicacion de la tienda.
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                      storefrontForm.publicada ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
                    }`}
                  >
                    {storefrontForm.publicada ? "Lista para publicar" : "Borrador"}
                  </span>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${sectionSmtpReady ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-600"}`}>
                    SMTP {sectionSmtpReady ? "Listo" : "Pendiente"}
                  </span>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-2">
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Dominio publico</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.dominioPublico ?? ""}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, dominioPublico: event.target.value }))}
                      placeholder="tienda.ejemplo.com"
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Nombre comercial</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.nombreComercial}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, nombreComercial: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Hero titulo</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.heroTitulo}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, heroTitulo: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2 md:col-span-2">
                    <span className="text-sm font-medium text-slate-700">Hero subtitulo</span>
                    <textarea
                      rows={3}
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.heroSubtitulo}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, heroSubtitulo: event.target.value }))}
                    />
                  </label>
                  <AssetUploadField
                    label="Logo"
                    value={storefrontForm.logoUrl}
                    placeholder="https://... o subir archivo"
                    buttonLabel="Subir logo"
                    uploading={uploadingAssetKind === "logo"}
                    onChange={(value) => setStorefrontForm((current) => ({ ...current, logoUrl: value }))}
                    onUpload={(file) => void handleUploadAsset("logo", file)}
                  />
                  <AssetUploadField
                    label="Hero imagen"
                    value={storefrontForm.heroImagenUrl}
                    placeholder="https://... o subir archivo"
                    buttonLabel="Subir hero"
                    uploading={uploadingAssetKind === "hero"}
                    onChange={(value) => setStorefrontForm((current) => ({ ...current, heroImagenUrl: value }))}
                    onUpload={(file) => void handleUploadAsset("hero", file)}
                  />
                  <AssetUploadField
                    label="Favicon"
                    value={storefrontForm.faviconUrl}
                    placeholder="https://... o subir archivo"
                    buttonLabel="Subir favicon"
                    uploading={uploadingAssetKind === "favicon"}
                    onChange={(value) => setStorefrontForm((current) => ({ ...current, faviconUrl: value }))}
                    onUpload={(file) => void handleUploadAsset("favicon", file)}
                  />
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Color primario</span>
                    <div className="flex items-center gap-3">
                      <input
                        type="color"
                        className="h-12 w-16 rounded-xl border border-slate-300 bg-white p-1"
                        value={normalizeColorValue(storefrontForm.colorPrimario, "#0f172a")}
                        onChange={(event) => setStorefrontForm((current) => ({ ...current, colorPrimario: event.target.value }))}
                      />
                      <input
                        className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                        value={storefrontForm.colorPrimario}
                        onChange={(event) => setStorefrontForm((current) => ({ ...current, colorPrimario: event.target.value }))}
                      />
                    </div>
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Color secundario</span>
                    <div className="flex items-center gap-3">
                      <input
                        type="color"
                        className="h-12 w-16 rounded-xl border border-slate-300 bg-white p-1"
                        value={normalizeColorValue(storefrontForm.colorSecundario, "#e2e8f0")}
                        onChange={(event) => setStorefrontForm((current) => ({ ...current, colorSecundario: event.target.value }))}
                      />
                      <input
                        className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                        value={storefrontForm.colorSecundario}
                        onChange={(event) => setStorefrontForm((current) => ({ ...current, colorSecundario: event.target.value }))}
                      />
                    </div>
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Email comercial / remitente</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.emailComercial}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, emailComercial: event.target.value }))}
                    />
                    <p className="text-xs leading-5 text-slate-500">
                      Si activas SMTP real en backend, este email se usa como contacto visible y como remitente preferido de los mails transaccionales de la tienda.
                    </p>
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Telefono comercial</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.telefonoComercial}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, telefonoComercial: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2 md:col-span-2">
                    <span className="text-sm font-medium text-slate-700">Direccion comercial</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.direccionComercial}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, direccionComercial: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2 md:col-span-2">
                    <span className="text-sm font-medium text-slate-700">Footer texto</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.footerTexto}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, footerTexto: event.target.value }))}
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">SMTP host</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.smtpHost}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, smtpHost: event.target.value }))}
                      placeholder="smtp.tu-proveedor.com"
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">SMTP port</span>
                    <input
                      type="number"
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.smtpPort ?? ""}
                      onChange={(event) =>
                        setStorefrontForm((current) => ({
                          ...current,
                          smtpPort: event.target.value ? Number(event.target.value) : null,
                        }))
                      }
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">SMTP username</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.smtpUsername}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, smtpUsername: event.target.value }))}
                      placeholder="usuario SMTP"
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">SMTP password</span>
                    <input
                      type="password"
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={storefrontForm.smtpPassword}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, smtpPassword: event.target.value }))}
                      placeholder={(selectedRevendedor?.storefront?.smtpConfigured || proveedorStorefrontQuery.data?.smtpConfigured) ? "Dejar vacio para conservar la actual" : "Password SMTP"}
                    />
                  </label>
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={storefrontForm.smtpAuth}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, smtpAuth: event.target.checked }))}
                    />
                    SMTP requiere auth
                  </label>
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={storefrontForm.smtpStartTls}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, smtpStartTls: event.target.checked }))}
                    />
                    SMTP usa STARTTLS
                  </label>
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 md:col-span-2">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={storefrontForm.publicada}
                      onChange={(event) => setStorefrontForm((current) => ({ ...current, publicada: event.target.checked }))}
                    />
                    Publicar storefront
                  </label>
                </div>

                <div className="mt-5">
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void handleSaveStorefront()}
                      disabled={upsertStorefront.isPending}
                    >
                      {upsertStorefront.isPending ? "Guardando storefront..." : "Guardar storefront"}
                    </button>
                    {storefrontSimulationUrl ? (
                      <a
                        href={storefrontSimulationUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-500 hover:text-slate-950"
                      >
                        Abrir storefront simulado
                      </a>
                    ) : null}
                  </div>
                  <p className="mt-3 text-xs leading-5 text-slate-500">
                    En local, este acceso abre la tienda publica usando el dominio configurado para simular el storefront sin cambiar
                    `VITE_STOREFRONT_HOST`.
                  </p>
                </div>
              </details>

              {MERCADO_PAGO_DISPONIBLE && (<details
                open
                className={`rounded-[2rem] border p-6 shadow-sm ${isOnboardingReady ? "border-emerald-200 bg-emerald-50/30" : "border-slate-200 bg-white"}`}
              >
                <summary className="mb-4 cursor-pointer list-none">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">Checklist de salida</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${isOnboardingReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {isOnboardingReady ? "Listo" : `${onboardingPendingCount} pendiente(s)`}
                    </span>
                  </div>
                </summary>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Cobros</p>
                    <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Mercado Pago por revendedor</h3>
                    <p className="mt-2 text-sm text-slate-500">
                      Guarda la vinculacion operativa del storefront. Por ahora queda lista la base para split/OAuth y seguimos con
                      fallback global mientras desarrollamos local.
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                      resolveMercadoPagoStatusTone(mercadoPagoStatus?.estado)
                    }`}
                  >
                    {mercadoPagoStatus?.estado ?? "PENDIENTE"}
                  </span>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-3">
                  <InfoSummaryCard
                    label="Estado"
                    value={sectionIdentidadReady ? "Listo" : "Pendiente"}
                    detail={
                      mercadoPagoStatus?.readyForCheckout
                        ? "La tienda ya tiene una salida operativa para Checkout Pro."
                        : "Todavia falta terminar la vinculacion comercial del PSP."
                    }
                  />
                  <InfoSummaryCard
                    label="Modo"
                    value={mercadoPagoStatus?.liveMode ? "Live" : "Test"}
                    detail="Usa test mientras seguimos cerrando el circuito real y el webhook publico."
                  />
                  <InfoSummaryCard
                    label="Fallback"
                    value={mercadoPagoStatus?.usaCredencialesGlobalesFallback ? "Si" : "No"}
                    detail="Permite cobrar temporalmente aunque el revendedor no tenga OAuth completo."
                  />
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-2">
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Mercado Pago user ID</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.mercadoPagoUserId}
                      onChange={(event) =>
                        setMercadoPagoForm((current) => ({ ...current, mercadoPagoUserId: event.target.value }))
                      }
                      placeholder="COLLECTOR_ID"
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Public key del vendedor</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.publicKey}
                      onChange={(event) => setMercadoPagoForm((current) => ({ ...current, publicKey: event.target.value }))}
                      placeholder="APP_USR-..."
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Access token</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.accessToken}
                      onChange={(event) => setMercadoPagoForm((current) => ({ ...current, accessToken: event.target.value }))}
                      placeholder={mercadoPagoStatus?.accessTokenMasked ?? "SELLER_ACCESS_TOKEN"}
                    />
                    <p className="text-xs text-slate-500">
                      {mercadoPagoStatus?.accessTokenConfigured
                        ? `Guardado actual: ${mercadoPagoStatus.accessTokenMasked ?? "configurado"}`
                        : "Todavia no hay token propio guardado."}
                    </p>
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Refresh token</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.refreshToken}
                      onChange={(event) =>
                        setMercadoPagoForm((current) => ({ ...current, refreshToken: event.target.value }))
                      }
                      placeholder={mercadoPagoStatus?.refreshTokenMasked ?? "SELLER_REFRESH_TOKEN"}
                    />
                    <p className="text-xs text-slate-500">
                      {mercadoPagoStatus?.refreshTokenConfigured
                        ? `Guardado actual: ${mercadoPagoStatus.refreshTokenMasked ?? "configurado"}`
                        : "Todavia no hay refresh token guardado."}
                    </p>
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Expira</span>
                    <input
                      type="datetime-local"
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.tokenExpiresAt ?? ""}
                      onChange={(event) =>
                        setMercadoPagoForm((current) => ({
                          ...current,
                          tokenExpiresAt: event.target.value ? event.target.value : null,
                        }))
                      }
                    />
                  </label>
                  <label className="space-y-2">
                    <span className="text-sm font-medium text-slate-700">Scope</span>
                    <input
                      className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                      value={mercadoPagoForm.scope}
                      onChange={(event) => setMercadoPagoForm((current) => ({ ...current, scope: event.target.value }))}
                      placeholder="offline_access payments write"
                    />
                  </label>
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 md:col-span-2">
                    <input
                      type="checkbox"
                      className="h-4 w-4 rounded border-slate-300"
                      checked={mercadoPagoForm.liveMode}
                      onChange={(event) => setMercadoPagoForm((current) => ({ ...current, liveMode: event.target.checked }))}
                    />
                    Cuenta vinculada en modo live
                  </label>
                </div>

                <div className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                  <InfoCell label="Linked at" value={formatDateTime(mercadoPagoStatus?.linkedAt)} />
                  <InfoCell label="Ultimo refresh" value={formatDateTime(mercadoPagoStatus?.lastRefreshAt)} />
                  <InfoCell label="User ID activo" value={mercadoPagoStatus?.mercadoPagoUserId ?? "No definido"} />
                  <InfoCell
                    label="Checkout"
                    value={mercadoPagoStatus?.readyForCheckout ? "Operativo" : "Todavia no operativo"}
                  />
                </div>

                <div className="mt-5">
                  <div className="flex flex-wrap gap-3">
                    <button
                      type="button"
                      className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void handleSaveMercadoPago()}
                      disabled={upsertMercadoPago.isPending}
                    >
                      {upsertMercadoPago.isPending ? "Guardando Mercado Pago..." : "Guardar Mercado Pago"}
                    </button>
                    <button
                      type="button"
                      className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-500 hover:text-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => void handleGenerateOAuthLink()}
                      disabled={mercadoPagoOAuthLink.isPending}
                    >
                      {mercadoPagoOAuthLink.isPending ? "Generando link OAuth..." : "Generar link de autorizacion"}
                    </button>
                  </div>
                  <p className="mt-3 text-xs leading-5 text-slate-500">
                    Este link sirve para enviarselo al revendedor. Mercado Pago vuelve al backend con `code`, guardamos la vinculacion y
                    luego redirigimos otra vez a esta pantalla.
                  </p>
                </div>
              </details>)}

              <details
                open
                className={`rounded-[2rem] border p-6 shadow-sm ${isOnboardingReady ? "border-emerald-200 bg-emerald-50/30" : "border-slate-200 bg-white"}`}
              >
                <summary className="mb-4 cursor-pointer list-none">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">Checklist de salida</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${isOnboardingReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {isOnboardingReady ? "Listo" : `${onboardingPendingCount} pendiente(s)`}
                    </span>
                  </div>
                </summary>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Onboarding</p>
                    <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Estado de puesta en marcha</h3>
                    <p className="mt-2 text-sm text-slate-500">
                      Esta guia resume si la tienda ya esta lista para presentacion comercial y publicacion.
                    </p>
                  </div>
                  <div
                    className={`rounded-2xl border px-4 py-3 text-sm font-medium ${
                      isOnboardingReady
                        ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                        : "border-amber-200 bg-amber-50 text-amber-700"
                    }`}
                  >
                    {isOnboardingReady
                      ? "Storefront lista para onboarding"
                      : `${onboardingPendingCount} punto(s) pendientes para completar`}
                  </div>
                </div>

                <div className="mt-6 grid gap-4 md:grid-cols-3">
                  <InfoSummaryCard
                    label="Checklist"
                    value={`${onboardingCompletedCount}/${onboardingChecks.length}`}
                    detail="Controles de salida comercial completos."
                  />
                  <InfoSummaryCard
                    label="Pendientes"
                    value={String(onboardingPendingCount)}
                    detail="Aspectos que conviene cerrar antes de presentar la tienda."
                  />
                  <InfoSummaryCard
                    label="Estado final"
                    value={isOnboardingReady ? "OK" : "Parcial"}
                    detail={
                      isOnboardingReady
                        ? "La tienda ya puede mostrarse como onboarding completo."
                        : "Aun quedan datos o validaciones por completar."
                    }
                  />
                </div>

                <div className="mt-6 grid gap-3">
                  {onboardingChecks.map((check) => (
                    <OnboardingCheckRow key={check.label} check={check} />
                  ))}
                </div>
              </details>

              <details
                open
                className={`overflow-hidden rounded-[2rem] border shadow-sm ${sectionContactoReady ? "border-emerald-200 bg-emerald-50/20" : "border-slate-200 bg-white"}`}
              >
                <summary className="cursor-pointer list-none px-6 py-4">
                  <div className="flex items-center justify-between gap-3">
                    <p className="text-sm font-semibold text-slate-900">Preview y contacto</p>
                    <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${sectionContactoReady ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {sectionContactoReady ? "Listo" : "Pendiente"}
                    </span>
                  </div>
                </summary>
                <div
                  className="p-6"
                  style={{
                    backgroundImage: `linear-gradient(145deg, ${secondaryPreview} 0%, #ffffff 38%, ${accentPreview}18 100%)`,
                  }}
                >
                  <p className="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">Preview</p>
                  <h3 className="mt-2 text-3xl font-semibold text-slate-950">{storefrontNamePreview}</h3>
                  <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-700">
                    {storefrontForm.heroSubtitulo || "Aqui ves una previsualizacion rapida del tono general de la tienda antes de guardarla."}
                  </p>
                </div>

                <div className="grid gap-4 p-6 lg:grid-cols-[0.9fr_1.1fr]">
                  <div className="rounded-[1.75rem] border border-slate-200 bg-slate-50 p-5">
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Identidad</p>
                    <div className="mt-4 flex items-center gap-4">
                      <div
                        className="flex h-16 w-16 items-center justify-center rounded-2xl text-sm font-semibold text-white"
                        style={{ backgroundColor: accentPreview }}
                      >
                        {(storefrontNamePreview || "NV").slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <p className="text-lg font-semibold text-slate-950">{storefrontNamePreview}</p>
                        <p className="text-sm text-slate-500">{storefrontForm.dominioPublico || revendedorForm.dominio || "Sin dominio configurado"}</p>
                      </div>
                    </div>
                  </div>

                  <div className="rounded-[1.75rem] border border-slate-200 bg-white p-5">
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Datos comerciales y envio</p>
                    <div className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-2">
                      <PreviewCell label="Email" value={storefrontForm.emailComercial || "No definido"} />
                      <PreviewCell label="Telefono" value={storefrontForm.telefonoComercial || "No definido"} />
                      <PreviewCell label="Direccion" value={storefrontForm.direccionComercial || "No definida"} />
                      <PreviewCell label="Estado" value={storefrontForm.publicada ? "Publicada" : "Borrador"} />
                      <PreviewCell label="Usuario" value={revendedorForm.usuarioEmail || "No definido"} />
                      <PreviewCell label="SMTP" value={storefrontForm.smtpHost || "Global / no definido"} />
                    </div>
                    {storefrontSimulationUrl ? (
                      <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-600">
                        <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">Validacion local</p>
                        <p className="mt-2 break-all">{window.location.origin}{storefrontSimulationUrl}</p>
                      </div>
                    ) : null}
                  </div>
                </div>
              </details>
            </>
          ) : (
            <section className="rounded-[2rem] border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
              Crea un revendedor o selecciona una tienda existente para comenzar a editar dominio y storefront.
            </section>
          )}
        </section>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoSummaryCard label="Totales" value={String(storefronts.length)} detail="Tiendas administrables detectadas." />
        <InfoSummaryCard label="Publicadas" value={String(publicadas.length)} detail="Storefronts visibles hoy." />
        <InfoSummaryCard label="Borrador" value={String(borrador.length)} detail="Tiendas listas para terminar de configurar." />
        <InfoSummaryCard
          label="Activas"
          value={String(revendedores.filter((item) => item.activo).length)}
          detail="Revendedores habilitados comercialmente."
        />
      </div>
      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <InfoSummaryCard label="Proveedores" value={String(proveedores.length)} detail="Usuarios proveedor disponibles para storefront propio." />
        <InfoSummaryCard label="Proveedor seleccionado" value={selectedProveedor?.nombre ?? "Ninguno"} detail="Selecciona desde la grilla para editar su storefront." />
      </div>
      </>
      ) : null}
    </section>
  )
}
