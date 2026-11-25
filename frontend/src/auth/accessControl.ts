export type AppRole = "CLIENTE" | "REVENDEDOR" | "PROVEEDOR" | "LOGISTICA_MD" | "ADMIN_MD"

type NavigationLink = {
  to: string
  label: string
  roles: AppRole[]
}

export type NavigationGroup = {
  id: string
  label: string
  roles: AppRole[]
  links: NavigationLink[]
}

type RouteRule = {
  matches: (pathname: string) => boolean
  roles: AppRole[]
}

const ROLE_HOME_PATHS: Record<AppRole, string> = {
  CLIENTE: "/",
  REVENDEDOR: "/catalogo",
  PROVEEDOR: "/proveedor",
  LOGISTICA_MD: "/logistica",
  ADMIN_MD: "/admin",
}

const ROLE_PRIORITY: AppRole[] = ["CLIENTE", "REVENDEDOR", "PROVEEDOR", "LOGISTICA_MD", "ADMIN_MD"]

export const navigationLinks: NavigationLink[] = [
  { to: "/catalogo", label: "Catalogo", roles: ["REVENDEDOR"] },
  { to: "/pedidos", label: "Pedidos", roles: ["REVENDEDOR"] },
  { to: "/pricing", label: "Precios y comisiones", roles: ["REVENDEDOR"] },
  { to: "/revendedor/red", label: "Red comercial", roles: ["REVENDEDOR"] },
  { to: "/revendedor/facturas", label: "Facturas y movimientos", roles: ["REVENDEDOR"] },
  { to: "/proveedor", label: "Dashboard proveedor", roles: ["PROVEEDOR"] },
  { to: "/proveedor/pedidos", label: "Pedidos", roles: ["PROVEEDOR"] },
  { to: "/proveedor/productos", label: "Productos", roles: ["PROVEEDOR"] },
  { to: "/proveedor/carga-excel", label: "Importar base de datos de productos", roles: ["PROVEEDOR"] },
  { to: "/proveedor/reclamos", label: "Reclamos", roles: ["PROVEEDOR"] },
  { to: "/logistica/envios", label: "Logistica", roles: ["LOGISTICA_MD"] },
  { to: "/notificaciones", label: "Notificaciones", roles: ["REVENDEDOR", "PROVEEDOR", "LOGISTICA_MD", "ADMIN_MD"] },
  { to: "/admin/rma", label: "RMA", roles: ["ADMIN_MD"] },
]

export const navigationGroups: NavigationGroup[] = [
  {
    id: "revendedor-compras",
    label: "Compras",
    roles: ["REVENDEDOR"],
    links: [
      { to: "/catalogo", label: "Catalogo", roles: ["REVENDEDOR"] },
      { to: "/pedidos", label: "Pedidos", roles: ["REVENDEDOR"] },
    ],
  },
  {
    id: "revendedor-comercial",
    label: "Comercial",
    roles: ["REVENDEDOR"],
    links: [
      { to: "/pricing", label: "Precios y comisiones", roles: ["REVENDEDOR"] },
      { to: "/revendedor/red", label: "Red comercial", roles: ["REVENDEDOR"] },
      { to: "/revendedor/facturas", label: "Facturas y movimientos", roles: ["REVENDEDOR"] },
    ],
  },
  {
    id: "proveedor-general",
    label: "General",
    roles: ["PROVEEDOR"],
    links: [
      { to: "/proveedor", label: "Dashboard proveedor", roles: ["PROVEEDOR"] },
    ],
  },
  {
    id: "proveedor-operacion",
    label: "Operacion",
    roles: ["PROVEEDOR"],
    links: [
      { to: "/proveedor/pedidos", label: "Pedidos", roles: ["PROVEEDOR"] },
      { to: "/proveedor/productos", label: "Productos", roles: ["PROVEEDOR"] },
      { to: "/proveedor/carga-excel", label: "Importar base de datos de productos", roles: ["PROVEEDOR"] },
    ],
  },
  {
    id: "proveedor-postventa",
    label: "Postventa",
    roles: ["PROVEEDOR"],
    links: [
      { to: "/proveedor/reclamos", label: "Reclamos", roles: ["PROVEEDOR"] },
      { to: "/notificaciones", label: "Notificaciones", roles: ["PROVEEDOR"] },
    ],
  },
  {
    id: "logistica-resumen",
    label: "Resumen",
    roles: ["LOGISTICA_MD"],
    links: [
      { to: "/logistica", label: "Dashboard logistica", roles: ["LOGISTICA_MD"] },
    ],
  },
  {
    id: "logistica-operacion",
    label: "Operacion",
    roles: ["LOGISTICA_MD"],
    links: [
      { to: "/logistica/envios", label: "Retiros y despachos", roles: ["LOGISTICA_MD"] },
      { to: "/logistica/seguimiento", label: "Seguimiento", roles: ["LOGISTICA_MD"] },
      { to: "/logistica/incidencias", label: "Incidencias", roles: ["LOGISTICA_MD"] },
      { to: "/logistica/historial", label: "Historial", roles: ["LOGISTICA_MD"] },
      { to: "/notificaciones", label: "Notificaciones", roles: ["LOGISTICA_MD"] },
    ],
  },
  {
    id: "admin-resumen",
    label: "Resumen",
    roles: ["ADMIN_MD"],
    links: [
      { to: "/admin", label: "Dashboard admin", roles: ["ADMIN_MD"] },
    ],
  },
  {
    id: "admin-operacion",
    label: "Operacion",
    roles: ["ADMIN_MD"],
    links: [
      { to: "/admin/operacion", label: "Command center", roles: ["ADMIN_MD"] },
      { to: "/admin/pedidos", label: "Pedidos", roles: ["ADMIN_MD"] },
      { to: "/admin/pagos", label: "Pagos", roles: ["ADMIN_MD"] },
      { to: "/admin/logistica", label: "Logistica", roles: ["ADMIN_MD"] },
      { to: "/admin/rma", label: "RMA", roles: ["ADMIN_MD"] },
    ],
  },
  {
    id: "admin-catalogo",
    label: "Catalogo",
    roles: ["ADMIN_MD"],
    links: [
      { to: "/admin/catalogo", label: "Catalogo canonico", roles: ["ADMIN_MD"] },
      { to: "/admin/canonizacion", label: "Canonizacion", roles: ["ADMIN_MD"] },
      { to: "/admin/proveedores", label: "Proveedores", roles: ["ADMIN_MD"] },
    ],
  },
  {
    id: "admin-comercial",
    label: "Red comercial",
    roles: ["ADMIN_MD"],
    links: [
      { to: "/admin/revendedores", label: "Revendedores", roles: ["ADMIN_MD"] },
      { to: "/admin/storefronts", label: "Storefronts", roles: ["ADMIN_MD"] },
      { to: "/admin/usuarios", label: "Usuarios y roles", roles: ["ADMIN_MD"] },
      { to: "/admin/comisiones", label: "Comisiones", roles: ["ADMIN_MD"] },
    ],
  },
  {
    id: "admin-control",
    label: "Control",
    roles: ["ADMIN_MD"],
    links: [
      { to: "/admin/notificaciones", label: "Notificaciones", roles: ["ADMIN_MD"] },
      { to: "/admin/reportes", label: "Reportes", roles: ["ADMIN_MD"] },
      { to: "/admin/auditoria", label: "Auditoria", roles: ["ADMIN_MD"] },
      { to: "/admin/configuracion", label: "Configuracion", roles: ["ADMIN_MD"] },
    ],
  },
]

const routeRules: RouteRule[] = [
  { matches: (pathname) => pathname === "/" || pathname === "/catalogo" || pathname.startsWith("/catalogo/"), roles: ["REVENDEDOR"] },
  { matches: (pathname) => pathname === "/pedidos" || pathname.startsWith("/pedidos/"), roles: ["REVENDEDOR"] },
  { matches: (pathname) => pathname === "/pricing" || pathname.startsWith("/pricing/"), roles: ["REVENDEDOR"] },
  { matches: (pathname) => pathname === "/revendedor/red" || pathname.startsWith("/revendedor/red/"), roles: ["REVENDEDOR"] },
  {
    matches: (pathname) => pathname === "/revendedor/facturas" || pathname.startsWith("/revendedor/facturas/"),
    roles: ["REVENDEDOR"],
  },
  { matches: (pathname) => pathname === "/proveedor" || pathname.startsWith("/proveedor/"), roles: ["PROVEEDOR"] },
  { matches: (pathname) => pathname === "/logistica" || pathname.startsWith("/logistica/"), roles: ["LOGISTICA_MD"] },
  { matches: (pathname) => pathname === "/notificaciones", roles: ["REVENDEDOR", "PROVEEDOR", "LOGISTICA_MD", "ADMIN_MD"] },
  { matches: (pathname) => pathname === "/rma" || pathname.startsWith("/rma/"), roles: ["ADMIN_MD"] },
  { matches: (pathname) => pathname === "/admin" || pathname.startsWith("/admin/"), roles: ["ADMIN_MD"] },
]

function normalizeRoles(roles: string[]): AppRole[] {
  return roles.filter((role): role is AppRole => role in ROLE_HOME_PATHS)
}

export function getDefaultPathForRoles(roles: string[]): string {
  const allowedRoles = normalizeRoles(roles)
  const primaryRole = ROLE_PRIORITY.find((role) => allowedRoles.includes(role))
  return primaryRole ? ROLE_HOME_PATHS[primaryRole] : "/portal/login"
}

export function hasFrontendAccess(roles: string[]): boolean {
  return normalizeRoles(roles).length > 0
}

export function canAccessPath(pathname: string, roles: string[]): boolean {
  const allowedRoles = normalizeRoles(roles)
  const matchedRule = routeRules.find((rule) => rule.matches(pathname))
  if (!matchedRule) {
    return false
  }
  return matchedRule.roles.some((role) => allowedRoles.includes(role))
}

export function getVisibleNavigationLinks(roles: string[]): NavigationLink[] {
  const allowedRoles = normalizeRoles(roles)
  return navigationLinks.filter((link) => link.roles.some((role) => allowedRoles.includes(role)))
}

export function getVisibleNavigationGroups(roles: string[]): NavigationGroup[] {
  const allowedRoles = normalizeRoles(roles)

  return navigationGroups
    .filter((group) => group.roles.some((role) => allowedRoles.includes(role)))
    .map((group) => ({
      ...group,
      links: group.links.filter((link) => link.roles.some((role) => allowedRoles.includes(role))),
    }))
    .filter((group) => group.links.length > 0)
}
