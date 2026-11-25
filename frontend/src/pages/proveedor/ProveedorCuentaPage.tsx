import { Link } from "react-router-dom"
import { useAuth } from "../../auth/useAuth"
import { useProveedorContext } from "../../hooks/useProveedorContext"

function formatDateTime(value: string | null) {
  if (!value) {
    return "No disponible"
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  return date.toLocaleString("es-AR")
}

function buildProviderDisplayName(email: string | null) {
  if (!email) {
    return "Proveedor autenticado"
  }

  const localPart = email.split("@")[0] ?? email
  const normalized = localPart.replace(/[._-]+/g, " ").trim()
  if (!normalized) {
    return email
  }

  return normalized.replace(/\b\w/g, (match) => match.toUpperCase())
}

function AccountDatum({
  label,
  value,
  hint,
}: {
  label: string
  value: string
  hint?: string
}) {
  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-3 text-base font-semibold text-slate-900">{value}</p>
      {hint ? <p className="mt-2 text-sm text-slate-500">{hint}</p> : null}
    </article>
  )
}

export default function ProveedorCuentaPage() {
  const { session } = useAuth()
  const { proveedorId, email, roles } = useProveedorContext()
  const providerDisplayName = buildProviderDisplayName(email)

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Proveedor</p>
        <div className="mt-3 flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-3">
            <h2 className="text-3xl font-semibold tracking-tight text-slate-950">Mi cuenta y empresa</h2>
            <p className="max-w-3xl text-sm leading-6 text-slate-600">
              Esta vista centraliza la identidad disponible del acceso proveedor actual. Muestra datos reales de la
              sesion y deja preparada la superficie donde mas adelante podremos exponer razon social, datos fiscales y
              configuracion de empresa cuando exista contrato backend.
            </p>
          </div>
          <div className="rounded-3xl border border-blue-100 bg-blue-50 px-4 py-3 text-sm text-blue-700">
            Acceso operativo para {providerDisplayName}
          </div>
        </div>
      </header>

      <div className="grid gap-4 lg:grid-cols-2 xl:grid-cols-4">
        <AccountDatum
          label="Identidad"
          value={providerDisplayName}
          hint="Alias derivado del email de acceso actual para reforzar identidad en el panel."
        />
        <AccountDatum
          label="Email de acceso"
          value={email ?? "No disponible"}
          hint="Usuario autenticado con el que se opera el dashboard proveedor."
        />
        <AccountDatum
          label="Proveedor asociado"
          value={proveedorId ?? "Sin proveedor vinculado"}
          hint="Identificador tecnico hoy disponible para operar pedidos, productos e importacion."
        />
        <AccountDatum
          label="Roles"
          value={roles.length > 0 ? roles.join(", ") : "Sin roles informados"}
          hint="Permisos vigentes de la sesion actual."
        />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.4fr_1fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Estado de la cuenta</p>
              <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Datos disponibles hoy</h3>
            </div>
            <span className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-emerald-700">
              Sesion activa
            </span>
          </div>

          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <AccountDatum
              label="Usuario"
              value={session?.userId ?? "No disponible"}
              hint="Id tecnico del usuario autenticado."
            />
            <AccountDatum
              label="Inicio de sesion"
              value={formatDateTime(session?.issuedAt ?? null)}
              hint="Fecha informada por el backend al emitir la credencial."
            />
          </div>

          <div className="mt-6 rounded-3xl border border-amber-200 bg-amber-50 p-5">
            <p className="text-sm font-semibold text-amber-900">Datos de empresa pendientes de contrato real</p>
            <p className="mt-2 text-sm leading-6 text-amber-800">
              Todavia no hay una API conectada para exponer razon social, CUIT, domicilio, telefonos o contactos
              administrativos del proveedor. Esta pantalla queda lista para alojar esos datos cuando el backend los
              publique sin tener que volver a redisenar la cabecera del modulo.
            </p>
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Accesos utiles</p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Atajos de operacion</h3>
          <div className="mt-6 space-y-3">
            <Link
              to="/proveedor"
              className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Volver al dashboard proveedor
            </Link>
            <Link
              to="/proveedor/productos"
              className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Gestionar productos y oferta
            </Link>
            <Link
              to="/proveedor/pedidos"
              className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Revisar pedidos pendientes
            </Link>
            <Link
              to="/notificaciones"
              className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Ver notificaciones operativas
            </Link>
          </div>
        </section>
      </div>
    </section>
  )
}
