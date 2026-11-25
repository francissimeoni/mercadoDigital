import { useEffect, useMemo, useState, type ReactNode } from "react"
import { Link, Outlet, useLocation, useNavigate } from "react-router-dom"
import { getVisibleNavigationGroups, type NavigationGroup } from "../../auth/accessControl"
import { useAuth } from "../../auth/useAuth"
import { useNotificaciones } from "../../hooks/useNotificaciones"

export default function MainLayout({ children }: { children?: ReactNode }) {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const roles = session?.roles ?? []
  const gruposVisibles = getVisibleNavigationGroups(roles)
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({})
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const notificacionesQuery = useNotificaciones(session?.userId ?? null)
  const notificacionesNoLeidas = (notificacionesQuery.data ?? []).filter((notificacion) => !notificacion.leida).length
  const canAccessProviderAccount = Boolean(session?.proveedorId) || roles.includes("PROVEEDOR")

  const activeGroupIds = useMemo(
    () =>
      gruposVisibles
        .filter((group) => group.links.some((link) => isNavigationLinkActive(link.to, location.pathname)))
        .map((group) => group.id),
    [gruposVisibles, location.pathname],
  )

  useEffect(() => {
    setOpenGroups((current) => {
      const next = { ...current }

      for (const group of gruposVisibles) {
        if (!(group.id in next)) {
          next[group.id] = activeGroupIds.includes(group.id) || gruposVisibles.length === 1
        }
      }

      return next
    })
  }, [activeGroupIds, gruposVisibles])

  useEffect(() => {
    setMobileNavOpen(false)
  }, [location.pathname])

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="fixed top-0 left-0 right-0 z-30 h-16 border-b border-slate-200 bg-white px-4 shadow-sm sm:px-6">
        <div className="flex h-full items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <button
              type="button"
              className="inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 md:hidden"
              aria-label={mobileNavOpen ? "Cerrar menu" : "Abrir menu"}
              aria-expanded={mobileNavOpen}
              onClick={() => {
                setMobileNavOpen((current) => !current)
              }}
            >
              <MenuIcon open={mobileNavOpen} />
            </button>
            <div className="min-w-0">
            <h1 className="text-lg font-semibold tracking-tight">Mercado Digital</h1>
            {session ? (
              <p className="truncate text-xs text-slate-500">
                {session.email} {roles.length > 0 ? `- ${roles.join(", ")}` : ""}
              </p>
            ) : null}
            </div>
          </div>
          <div className="flex items-center gap-2">
            {session ? (
              <Link
                to="/notificaciones"
                reloadDocument
                className="relative inline-flex h-10 w-10 items-center justify-center rounded-full border border-slate-200 bg-white text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                aria-label="Ir a notificaciones"
                title="Notificaciones"
              >
                <BellIcon />
                {notificacionesNoLeidas > 0 ? (
                  <span className="absolute -right-1 -top-1 inline-flex min-h-5 min-w-5 items-center justify-center rounded-full bg-blue-600 px-1 text-[11px] font-semibold text-white">
                    {notificacionesNoLeidas > 99 ? "99+" : notificacionesNoLeidas}
                  </span>
                ) : null}
              </Link>
            ) : null}

            {canAccessProviderAccount ? (
              <Link
                to="/proveedor/mi-cuenta"
                reloadDocument
                className="hidden rounded-md border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 sm:inline-flex"
              >
                Mi cuenta
              </Link>
            ) : null}

            <button
              type="button"
              className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700"
              onClick={() => {
                logout()
                navigate("/portal/login", { replace: true })
              }}
            >
              Salir
            </button>
          </div>
        </div>
      </header>

      {mobileNavOpen ? (
        <button
          type="button"
          aria-label="Cerrar menu lateral"
          className="fixed inset-0 top-16 z-10 bg-slate-950/35 md:hidden"
          onClick={() => {
            setMobileNavOpen(false)
          }}
        />
      ) : null}

      <aside
        className={`fixed left-0 top-16 bottom-0 z-20 w-[86vw] max-w-72 overflow-y-auto border-r border-slate-200 bg-white transition-transform md:z-10 md:w-64 ${
          mobileNavOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
        }`}
      >
        <nav className="flex flex-col gap-3 p-4 pb-8">
          {gruposVisibles.map((group) => (
            <NavigationAccordionGroup
              key={group.id}
              group={group}
              isOpen={Boolean(openGroups[group.id])}
              pathname={location.pathname}
              onToggle={() =>
                setOpenGroups((current) => ({
                  ...current,
                  [group.id]: !current[group.id],
                }))
              }
            />
          ))}
          {gruposVisibles.length === 0 ? (
            <p className="rounded-md bg-slate-100 px-3 py-2 text-sm text-slate-600">
              No hay accesos para el rol actual.
            </p>
          ) : null}
        </nav>
      </aside>

      <main className="pt-16 md:pl-64">
        <div className="min-h-[calc(100vh-4rem)] p-4 sm:p-6">
          {children ?? <Outlet />}
        </div>
      </main>
    </div>
  )
}

function NavigationAccordionGroup({
  group,
  isOpen,
  pathname,
  onToggle,
}: {
  group: NavigationGroup
  isOpen: boolean
  pathname: string
  onToggle: () => void
}) {
  const hasActiveLink = group.links.some((link) => isNavigationLinkActive(link.to, pathname))

  return (
    <section className="rounded-2xl border border-slate-200 bg-slate-50/70 p-2">
      <button
        type="button"
        onClick={onToggle}
        className={`flex w-full items-center justify-between rounded-xl px-3 py-2 text-left text-sm font-semibold transition ${
          hasActiveLink ? "bg-blue-50 text-blue-700" : "text-slate-700 hover:bg-white"
        }`}
      >
        <span>{group.label}</span>
        <span className={`text-xs transition-transform ${isOpen ? "rotate-180" : ""}`}>▼</span>
      </button>

      {isOpen ? (
        <div className="mt-2 space-y-1">
          {group.links.map((link) => (
            <Link
              key={link.to}
              to={link.to}
              reloadDocument
              className={`block w-full rounded-xl px-3 py-2 text-left text-sm font-medium transition-colors ${
                isNavigationLinkActive(link.to, pathname) ? "bg-blue-600 text-white" : "text-slate-700 hover:bg-white"
              }`}
            >
              {link.label}
            </Link>
          ))}
        </div>
      ) : null}
    </section>
  )
}

function isNavigationLinkActive(to: string, pathname: string) {
  if (to === "/proveedor") {
    return pathname === to
  }
  return pathname === to || pathname.startsWith(`${to}/`)
}

function BellIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M6.2 16.4h11.6l-1.2-1.7a3.7 3.7 0 0 1-.7-2.1v-2.2a3.9 3.9 0 1 0-7.8 0v2.2c0 .8-.3 1.5-.7 2.1l-1.2 1.7Z" />
      <path d="M10 18.8a2.3 2.3 0 0 0 4 0" />
    </svg>
  )
}

function MenuIcon({ open }: { open: boolean }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {open ? (
        <>
          <path d="M6 6l12 12" />
          <path d="M18 6 6 18" />
        </>
      ) : (
        <>
          <path d="M4 7h16" />
          <path d="M4 12h16" />
          <path d="M4 17h16" />
        </>
      )}
    </svg>
  )
}
