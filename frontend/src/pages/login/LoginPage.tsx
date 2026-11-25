import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { getDefaultPathForRoles } from "../../auth/accessControl"
import { useAuth } from "../../auth/useAuth"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { resolveStorefrontDisplayName } from "../../storefront/utils/storefrontBranding"
import { isStorefrontBrowserHost } from "../../tenant/storefrontHost"
import { useTenant } from "../../tenant/useTenant"

export default function LoginPage() {
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const navigate = useNavigate()
  const { isAuthenticated, login, logout, session } = useAuth()
  const tenant = useTenant()
  const isStorefrontLogin = isStorefrontBrowserHost(tenant.host ?? window.location.hostname)
  const storefrontName = resolveStorefrontDisplayName(tenant.branding)

  const resolvePostLoginPath = (roles: string[]) => {
    if (isStorefrontLogin) {
      return "/"
    }

    return getDefaultPathForRoles(roles)
  }

  const resolverMensajeError = (error: unknown) => {
    if (error instanceof Error && error.message) {
      return error.message
    }

    if (
      typeof error === "object" &&
      error !== null &&
      "response" in error &&
      typeof error.response === "object" &&
      error.response !== null &&
      "data" in error.response &&
      typeof error.response.data === "object" &&
      error.response.data !== null &&
      "message" in error.response.data &&
      typeof error.response.data.message === "string"
    ) {
      return error.response.data.message
    }

    return "Credenciales invalidas o backend no disponible."
  }

  const handleLogin = async () => {
    if (!email || !password) {
      setError("Completa email y password.")
      return
    }

    setError(null)
    setIsLoading(true)

    try {
      const nextSession = await login({ email, password })
      navigate(resolvePostLoginPath(nextSession.roles), { replace: true })
    } catch (error) {
      setError(resolverMensajeError(error))
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,#dbeafe_0%,#f8fafc_38%,#ffffff_100%)] px-6 py-10">
      <div className="mx-auto w-full max-w-lg rounded-[2rem] border border-slate-200 bg-white p-6 shadow-xl sm:p-8">
        <h2 className="mb-1 text-2xl font-semibold text-slate-900">
          {isStorefrontLogin ? storefrontName || "Tienda online" : "Portal Mercado Digital"}
        </h2>
        <p className="mb-5 text-sm text-slate-600">
          {isStorefrontLogin
            ? "Ingresa con tu usuario para acceder a tu cuenta en esta tienda."
            : "Ingresa con tu usuario para entrar al panel operativo o administrativo correspondiente."}
        </p>

        {isAuthenticated && session ? (
          <div className="mb-5 rounded-3xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
            <p className="font-semibold">Ya hay una sesion iniciada.</p>
            <p className="mt-1">{session.email}</p>
            <div className="mt-4 flex flex-wrap gap-3">
              <button
                type="button"
                className="rounded-full bg-emerald-600 px-4 py-2 font-medium text-white transition hover:bg-emerald-700"
                onClick={() => {
                  navigate(resolvePostLoginPath(session.roles))
                }}
              >
                {isStorefrontLogin ? "Volver a la tienda" : "Ir al panel"}
              </button>
              <button
                type="button"
                className="rounded-full border border-emerald-300 px-4 py-2 font-medium text-emerald-800 transition hover:bg-emerald-100"
                onClick={() => {
                  logout()
                }}
              >
                Cambiar de cuenta
              </button>
            </div>
          </div>
        ) : null}

        <input
          className="mb-3 w-full rounded-2xl border border-slate-300 bg-slate-50 p-3 text-sm outline-none transition focus:border-blue-500 focus:bg-white"
          placeholder="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />

        <input
          className="mb-4 w-full rounded-2xl border border-slate-300 bg-slate-50 p-3 text-sm outline-none transition focus:border-blue-500 focus:bg-white"
          type="password"
          placeholder="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <button
          className="w-full rounded-2xl bg-blue-600 p-3 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-70"
          onClick={() => {
            void handleLogin()
          }}
          disabled={isLoading}
        >
          {isLoading ? "Ingresando..." : "Ingresar"}
        </button>

        {error ? (
          <div className="mt-3">
            <FeedbackAlert variant="error">{error}</FeedbackAlert>
          </div>
        ) : null}

        {isStorefrontLogin ? null : (
          <div className="mt-5 border-t border-slate-200 pt-5 text-sm text-slate-600">
            <p>Si buscas una tienda publica, utiliza el dominio comercial correspondiente.</p>
          </div>
        )}
      </div>
    </div>
  )
}
