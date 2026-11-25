import axios from "axios"
import { useQuery } from "@tanstack/react-query"
import axiosClient from "../../api/axiosClient"
import { resolveApiBaseUrl } from "../../api/resolveApiBaseUrl"
import { useState, type FormEvent } from "react"
import { Navigate, useNavigate, useSearchParams } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"

const inputClass = "w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none transition focus:border-blue-400 focus:ring-2 focus:ring-blue-100"

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    return error.response?.status === 401
      ? "Email o contraseña incorrectos para esta tienda."
      : (error.response?.data?.message ?? "No se pudo completar el acceso.")
  }
  return "No se pudo completar el acceso."
}

export default function StorefrontLoginPage() {
  const tenant = useTenant()
  const { session, login, register, logout } = useStorefrontAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const requestedRedirect = searchParams.get("redirect")
  const returnPath = requestedRedirect?.startsWith("/") && !requestedRedirect.startsWith("//") ? requestedRedirect : "/mi-cuenta"
  const [mode, setMode] = useState<"login" | "register">(searchParams.get("modo") === "registro" ? "register" : "login")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState({
    nombre: "", email: searchParams.get("email") || "", password: "", telefonoWhatsapp: "", provincia: "",
    localidad: "", codigoPostal: "", calle: "", numero: "", pisoDepto: "", observacionesEnvio: "",
  })
  const googleUrl = tenant.host ? `${resolveApiBaseUrl()}/public/storefront/auth/google/iniciar?storefrontHost=${encodeURIComponent(tenant.host)}&returnPath=${encodeURIComponent(returnPath)}` : null
  const googleConfig = useQuery({
    queryKey: ["storefront-google-config"],
    queryFn: async () => (await axiosClient.get<{ habilitado: boolean }>("/public/storefront/auth/google/configuracion")).data,
    staleTime: 60_000,
  })

  if (session?.roles.some((role) => role === "REVENDEDOR" || role === "ADMIN_MD") && session.revendedorId === tenant.revendedorId) {
    return <Navigate to="/mi-tienda" replace />
  }

  if (session?.roles.includes("CLIENTE") && session.clienteId) return <Navigate to={returnPath} replace />

  if (session) {
    return (
      <section className="mx-auto max-w-xl py-6 sm:py-10">
        <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-xl shadow-slate-200/50 sm:p-8">
          <p className="text-xs font-bold uppercase tracking-[.22em] text-blue-700">Mi cuenta</p>
          <h1 className="mt-2 text-3xl font-semibold text-slate-950">Hola, {session.nombre}</h1>
          <p className="mt-2 text-sm text-slate-600">Tu sesión está activa en esta tienda.</p>
          <dl className="mt-6 grid gap-4 rounded-2xl bg-slate-50 p-4 sm:grid-cols-2">
            <div><dt className="text-xs font-bold uppercase tracking-wider text-slate-400">Email</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{session.email}</dd></div>
            <div><dt className="text-xs font-bold uppercase tracking-wider text-slate-400">Perfil</dt><dd className="mt-1 text-sm font-semibold text-slate-800">{session.roles.includes("ADMIN_MD") ? "Administrador Mercado Digital" : session.roles.includes("REVENDEDOR") ? "Propietario" : session.roles.includes("ASESOR_VENTA") ? "Asesor comercial" : "Cliente"}</dd></div>
          </dl>
          <div className="mt-6 flex flex-wrap gap-3">
            <button onClick={() => navigate("/")} className="rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800">Volver a la tienda</button>
            <button onClick={() => { logout(); navigate("/", { replace: true }) }} className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-600 hover:border-slate-400">Cerrar sesión</button>
          </div>
        </div>
      </section>
    )
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!tenant.host) return
    setBusy(true)
    setError(null)
    try {
      const next = mode === "login"
        ? await login({ storefrontHost: tenant.host, email: form.email, password: form.password })
        : await register({ storefrontHost: tenant.host, ...form })
      navigate(next.roles.some((role) => role === "REVENDEDOR" || role === "ADMIN_MD") ? "/mi-tienda" : returnPath, { replace: true })
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  const set = (name: keyof typeof form, value: string) => setForm((current) => ({ ...current, [name]: value }))

  return (
    <section className="mx-auto max-w-xl py-6 sm:py-10">
      {tenant.storefront?.modoCatalogo && <div className="mb-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm leading-6 text-emerald-950">No necesitás una cuenta para consultar productos por WhatsApp. El acceso se conserva para administrar la tienda y consultar pedidos anteriores. <a href="/catalogo" className="font-semibold underline">Ver catálogo</a></div>}
      <div className="rounded-3xl border border-slate-200 bg-white p-5 shadow-xl shadow-slate-200/50 sm:p-8">
        <p className="text-xs font-bold uppercase tracking-[.22em] text-blue-700">Cuenta de la tienda</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-950">
          {mode === "login" ? "Ingresar" : "Crear cuenta"}
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          {mode === "login" ? "Accedé como propietario, asesor o cliente." : "Registrate como cliente de esta tienda."}
        </p>

        {googleUrl && googleConfig.data?.habilitado ? <>
          <a href={googleUrl} className="mt-6 flex w-full items-center justify-center gap-3 rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-bold text-slate-800 shadow-sm hover:bg-slate-50">
            <span aria-hidden="true" className="text-blue-600">G</span>Continuar con Google
          </a>
          <p className="my-5 text-center text-xs font-semibold uppercase text-slate-400">o usá tu email</p>
        </> : null}
        <div className="mt-5 grid grid-cols-2 rounded-xl bg-slate-100 p-1">
          <button className={`rounded-lg px-3 py-2 text-sm font-semibold ${mode === "login" ? "bg-white text-slate-950 shadow-sm" : "text-slate-500"}`} onClick={() => setMode("login")}>Ingresar</button>
          <button className={`rounded-lg px-3 py-2 text-sm font-semibold ${mode === "register" ? "bg-white text-slate-950 shadow-sm" : "text-slate-500"}`} onClick={() => setMode("register")}>Registrarme</button>
        </div>

        {searchParams.get("motivo") === "sesion-expirada" ? <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">Tu sesión venció y no pudo renovarse. Ingresá nuevamente para continuar.</div> : null}

        {error ? <div className="mt-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div> : null}

        <form className="mt-5 grid gap-3 sm:grid-cols-2" onSubmit={submit}>
          {mode === "register" ? <label className="sm:col-span-2 text-xs font-medium text-slate-600">Nombre completo<input required className={`${inputClass} mt-1`} value={form.nombre} onChange={(e) => set("nombre", e.target.value)} /></label> : null}
          <label className="sm:col-span-2 text-xs font-medium text-slate-600">Email<input required type="email" className={`${inputClass} mt-1`} value={form.email} onChange={(e) => set("email", e.target.value)} /></label>
          <label className="sm:col-span-2 text-xs font-medium text-slate-600">Contraseña<input required minLength={6} type="password" className={`${inputClass} mt-1`} value={form.password} onChange={(e) => set("password", e.target.value)} /></label>
          {mode === "register" ? <>
            <label className="sm:col-span-2 text-xs font-medium text-slate-600">WhatsApp<input required className={`${inputClass} mt-1`} value={form.telefonoWhatsapp} onChange={(e) => set("telefonoWhatsapp", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Provincia<input required className={`${inputClass} mt-1`} value={form.provincia} onChange={(e) => set("provincia", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Localidad<input required className={`${inputClass} mt-1`} value={form.localidad} onChange={(e) => set("localidad", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Código postal<input required className={`${inputClass} mt-1`} value={form.codigoPostal} onChange={(e) => set("codigoPostal", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Calle<input required className={`${inputClass} mt-1`} value={form.calle} onChange={(e) => set("calle", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Número<input required className={`${inputClass} mt-1`} value={form.numero} onChange={(e) => set("numero", e.target.value)} /></label>
            <label className="text-xs font-medium text-slate-600">Piso / depto.<input className={`${inputClass} mt-1`} value={form.pisoDepto} onChange={(e) => set("pisoDepto", e.target.value)} /></label>
            <label className="sm:col-span-2 text-xs font-medium text-slate-600">Indicaciones de entrega<textarea className={`${inputClass} mt-1 resize-none`} rows={2} value={form.observacionesEnvio} onChange={(e) => set("observacionesEnvio", e.target.value)} /></label>
          </> : null}
          <button disabled={busy} className="sm:col-span-2 mt-2 rounded-xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:opacity-50">
            {busy ? "Procesando..." : mode === "login" ? "Ingresar a mi cuenta" : "Crear mi cuenta"}
          </button>
        </form>
      </div>
    </section>
  )
}
