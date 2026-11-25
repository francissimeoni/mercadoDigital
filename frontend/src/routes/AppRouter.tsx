import CatalogoCompraGate from "../storefront/components/CatalogoCompraGate"
import CatalogoOperacionGate from "../storefront/components/CatalogoOperacionGate"
import { BrowserRouter, Route, Routes } from "react-router-dom"
import MainLayout from "../components/layout/MainLayout"
import AdminCanonizacionPage from "../pages/admin/AdminCanonizacionPage"
import AdminConfiguracionPage from "../pages/admin/AdminConfiguracionPage"
import AdminDashboardPage from "../pages/admin/AdminDashboardPage"
import AdminModulePlaceholderPage from "../pages/admin/AdminModulePlaceholderPage"
import AdminRmaPage from "../pages/admin/AdminRmaPage"
import AdminRevendedoresPage from "../pages/admin/AdminRevendedoresPage"
import AdminStorefrontsPage from "../pages/admin/AdminStorefrontsPage"
import AdminUsuariosPage from "../pages/admin/AdminUsuariosPage"
import LogisticaDashboardPage from "../pages/logistica/LogisticaDashboardPage"
import LogisticaEnviosPage from "../pages/logistica/LogisticaEnviosPage"
import LogisticaPreparedModulePage from "../pages/logistica/LogisticaPreparedModulePage"
import NotificacionesPage from "../pages/NotificacionesPage"
import PedidoDetallePage from "../pages/PedidoDetallePage"
import PedidoConfirmadoPage from "../pages/PedidoConfirmadoPage"
import PedidosPage from "../pages/PedidosPage"
import ProductoDetallePage from "../pages/ProductoDetallePage"
import RevendedorFacturasPage from "../pages/RevendedorFacturasPage"
import RevendedorPricingPage from "../pages/RevendedorPricingPage"
import RevendedorRedComercialPage from "../pages/RevendedorRedComercialPage"
import ProveedorCargaExcelPage from "../pages/proveedor/ProveedorCargaExcelPage"
import ProveedorCuentaPage from "../pages/proveedor/ProveedorCuentaPage"
import ProveedorDashboardPage from "../pages/proveedor/ProveedorDashboardPage"
import ProveedorPedidosPage from "../pages/proveedor/ProveedorPedidosPage"
import ProveedorProductosPage from "../pages/proveedor/ProveedorProductosPage"
import ProveedorReclamosPage from "../pages/proveedor/ProveedorReclamosPage"
import RmaDetallePage from "../pages/rma/RmaDetallePage"
import RmaPage from "../pages/rma/RmaPage"
import LoginPage from "../pages/login/LoginPage"
import PublicStoreLayout from "../storefront/layout/PublicStoreLayout"
import StorefrontCheckoutPage from "../storefront/pages/StorefrontCheckoutPage"
import StorefrontCompraResumenPage from "../storefront/pages/StorefrontCompraResumenPage"
import StorefrontPagoRetornoPage from "../storefront/pages/StorefrontPagoRetornoPage"
import StorefrontPedidoTrackingPage from "../storefront/pages/StorefrontPedidoTrackingPage"
import StorefrontProductDetailPage from "../storefront/pages/StorefrontProductDetailPage"
import StorefrontLoginPage from "../storefront/pages/StorefrontLoginPage"
import StorefrontGoogleCallbackPage from "../storefront/pages/StorefrontGoogleCallbackPage"
import StorefrontCustomerAccountPage from "../storefront/pages/StorefrontCustomerAccountPage"
import StorefrontOwnerPage from "../storefront/pages/StorefrontOwnerPage"
import StorefrontTransferReceiptPage from "../storefront/pages/StorefrontTransferReceiptPage"
import AppFallbackRoute from "./AppFallbackRoute"
import CatalogoRouteGate from "./CatalogoRouteGate"
import HomeRouteGate from "./HomeRouteGate"
import PortalEntryRoute from "./PortalEntryRoute"
import ProtectedRoute from "./ProtectedRoute"
import QuienesSomosRouteGate from "./QuienesSomosRouteGate"

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeRouteGate />} />
        <Route path="/quienes-somos" element={<QuienesSomosRouteGate />} />
        <Route path="/portal" element={<PortalEntryRoute />} />
        <Route path="/catalogo" element={<CatalogoRouteGate />} />
        <Route element={<PublicStoreLayout />}>
          <Route path="/ingresar" element={<StorefrontLoginPage />} />
          <Route path="/ingresar/google" element={<StorefrontGoogleCallbackPage />} />
          <Route path="/mi-cuenta" element={<StorefrontCustomerAccountPage />} />
          <Route path="/producto/:productoCanonicoId" element={<StorefrontProductDetailPage />} />
          <Route path="/producto/:productoCanonicoId/resumen-compra" element={<CatalogoCompraGate><StorefrontCompraResumenPage /></CatalogoCompraGate>} />
          <Route path="/producto/:productoCanonicoId/checkout" element={<CatalogoCompraGate><StorefrontCheckoutPage /></CatalogoCompraGate>} />
          <Route path="/pago/retorno" element={<StorefrontPagoRetornoPage />} />
          <Route path="/pedido/:trackingToken" element={<StorefrontPedidoTrackingPage />} />
          <Route path="/transferencia/:token" element={<StorefrontTransferReceiptPage />} />
        </Route>
        <Route path="/mi-tienda" element={<StorefrontOwnerPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/portal/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<MainLayout />}>
            <Route path="/catalogo/:productoId" element={<CatalogoOperacionGate><ProductoDetallePage /></CatalogoOperacionGate>} />
            <Route path="/pedidos/confirmado/:pedidoId" element={<PedidoConfirmadoPage />} />
            <Route path="/pedidos/:pedidoId" element={<PedidoDetallePage />} />
            <Route path="/pedidos" element={<PedidosPage />} />
            <Route path="/pricing" element={<CatalogoOperacionGate><RevendedorPricingPage /></CatalogoOperacionGate>} />
            <Route path="/revendedor/red" element={<RevendedorRedComercialPage />} />
            <Route path="/revendedor/facturas" element={<RevendedorFacturasPage />} />
            <Route path="/proveedor" element={<ProveedorDashboardPage />} />
            <Route path="/proveedor/mi-cuenta" element={<ProveedorCuentaPage />} />
            <Route path="/proveedor/pedidos" element={<ProveedorPedidosPage />} />
            <Route path="/proveedor/productos" element={<ProveedorProductosPage />} />
            <Route path="/proveedor/carga-excel" element={<ProveedorCargaExcelPage />} />
            <Route path="/proveedor/reclamos" element={<ProveedorReclamosPage />} />
            <Route path="/admin" element={<AdminDashboardPage />} />
            <Route
              path="/admin/operacion"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Operacion"
                  description="Superficie tipo command center para consolidar alertas, salud de modulos y timeline critico. La maqueta queda preparada para integrar pedidos, pagos, logistica, catalogo y storefront sin perder foco operativo."
                  status="conectable"
                  bullets={[
                    "Timeline de eventos criticos y alertas por severidad.",
                    "Indicadores de salud operativa cruzados entre pagos, logistica, catalogo y RMA.",
                    "Cola priorizada de tareas administrativas.",
                  ]}
                  primaryCta={{ label: "Volver al dashboard admin", to: "/admin" }}
                  secondaryCta={{ label: "Abrir RMA", to: "/admin/rma" }}
                />
              }
            />
            <Route
              path="/admin/pedidos"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Pedidos"
                  description="Bandeja administrativa integral para seguimiento transversal de pedidos, pagos, envios y referencias relacionadas. En esta fase se deja estructurada sin inventar un contrato admin que todavia no fue expuesto."
                  status="parcial"
                  bullets={[
                    "Listado con filtros por estado, fecha, revendedor, proveedor y origen.",
                    "Detalle admin con cliente, proveedor, pago, envio y auditoria.",
                    "Accesos rapidos a pago, envio, RMA y exportacion.",
                  ]}
                  primaryCta={{ label: "Ir a operacion", to: "/admin/operacion" }}
                  secondaryCta={{ label: "Abrir auditoria", to: "/admin/auditoria" }}
                />
              }
            />
            <Route
              path="/admin/pagos"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Pagos"
                  description="Bandeja de supervision financiera para revisar estados PSP, inconsistencias y futuro flujo de conciliacion. Queda preparada pero sin inventar un admin de pagos que el backend aun no expone."
                  status="planificado"
                  bullets={[
                    "Pendientes, confirmados, fallidos y pagos en revision.",
                    "Detalle con referencia PSP, pedido asociado, timestamps y webhook.",
                    "Base futura para conciliacion, split real y reintentos.",
                  ]}
                  primaryCta={{ label: "Ver dashboard admin", to: "/admin" }}
                  secondaryCta={{ label: "Ir a auditoria", to: "/admin/auditoria" }}
                />
              }
            />
            <Route
              path="/admin/logistica"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Logistica"
                  description="Vista ejecutiva para seguir retiros, despachos, entregas e incidencias desde la perspectiva admin. El flujo operativo real ya existe en el modulo de logistica y aca se prepara su supervision central."
                  status="conectable"
                  bullets={[
                    "Retiros pendientes, despachos pendientes y entregas del dia.",
                    "Envios demorados y tracking incompleto.",
                    "Detalle de envio con tracking, rotulo PDF y auditoria.",
                  ]}
                  primaryCta={{ label: "Ver command center", to: "/admin/operacion" }}
                  secondaryCta={{ label: "Abrir notificaciones admin", to: "/admin/notificaciones" }}
                />
              }
            />
            <Route path="/admin/rma" element={<AdminRmaPage />} />
            <Route path="/admin/rma/:rmaId" element={<RmaDetallePage />} />
            <Route
              path="/admin/catalogo"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Catalogo canonico"
                  description="Vista administrativa del catalogo maestro para inspeccionar productos canonicos, ofertas proveedor asociadas y calidad de ficha. Se deja estructurada sobre el modelo existente, pero sin cerrar la superficie admin completa en esta etapa."
                  status="parcial"
                  bullets={[
                    "Busqueda de producto canonico y revision de visibilidad.",
                    "Inspeccion de media y calidad de ficha.",
                    "Comparativa de ofertas proveedor y stock asociado.",
                  ]}
                  primaryCta={{ label: "Ir a canonizacion", to: "/admin/canonizacion" }}
                  secondaryCta={{ label: "Volver al dashboard admin", to: "/admin" }}
                />
              }
            />
            <Route path="/admin/canonizacion" element={<AdminCanonizacionPage />} />
            <Route
              path="/admin/proveedores"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Proveedores"
                  description="Bandeja de supervision de proveedores para revisar importaciones, catalogo cargado, pedidos por preparar y media. La estructura queda lista y alineada con el crecimiento del modulo proveedor ya operativo."
                  status="parcial"
                  bullets={[
                    "Listado con estado, usuario asociado y volumen de productos.",
                    "Tabs de resumen, productos, importaciones, pedidos, media e incidencias.",
                    "Accesos a productos pendientes y errores de importacion.",
                  ]}
                  primaryCta={{ label: "Ir a canonizacion", to: "/admin/canonizacion" }}
                  secondaryCta={{ label: "Volver al dashboard admin", to: "/admin" }}
                />
              }
            />
            <Route path="/admin/revendedores" element={<AdminRevendedoresPage />} />
            <Route path="/admin/storefronts" element={<AdminStorefrontsPage />} />
            <Route path="/admin/usuarios" element={<AdminUsuariosPage />} />
            <Route path="/admin/notificaciones" element={<NotificacionesPage />} />
            <Route
              path="/admin/reportes"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Reportes"
                  description="Capa de informes inicial para ventas, pedidos, RMA, envios y volumen por entidad. Se deja el cascaron profesional sin prometer metricas que hoy el backend aun no consolida en endpoints dedicados."
                  status="planificado"
                  bullets={[
                    "Ventas por periodo y pedidos por estado.",
                    "RMA por periodo y envios por transportista.",
                    "Top de productos, proveedores y revendedores por volumen.",
                  ]}
                  primaryCta={{ label: "Volver al dashboard admin", to: "/admin" }}
                  secondaryCta={{ label: "Abrir revendedores", to: "/admin/revendedores" }}
                />
              }
            />
            <Route
              path="/admin/auditoria"
              element={
                <AdminModulePlaceholderPage
                  eyebrow="Admin MD"
                  title="Auditoria"
                  description="Vista central para reconstruccion operativa, soporte y debugging funcional. Esta pantalla queda lista para consolidar eventos de pedidos, pagos, envios, RMA y acciones administrativas."
                  status="conectable"
                  bullets={[
                    "Linea temporal de eventos por fecha, actor, entidad y resultado.",
                    "Filtros por tipo de evento, referencia y rango temporal.",
                    "Uso operativo para disputas, soporte y reconstruccion de casos.",
                  ]}
                  primaryCta={{ label: "Volver al dashboard admin", to: "/admin" }}
                  secondaryCta={{ label: "Abrir RMA", to: "/admin/rma" }}
                />
              }
            />
            <Route
              path="/admin/configuracion"
              element={<AdminConfiguracionPage />}
            />
            <Route
              path="/admin/comisiones"
              element={<AdminConfiguracionPage />}
            />
            <Route path="/logistica" element={<LogisticaDashboardPage />} />
            <Route path="/logistica/envios" element={<LogisticaEnviosPage />} />
            <Route
              path="/logistica/seguimiento"
              element={
                <LogisticaPreparedModulePage
                  eyebrow="Logistica MD"
                  title="Seguimiento / En tránsito"
                  description="Superficie preparada para listado consolidado de envíos ya despachados, tracking, transportista y última actualización. Queda visible y lista para conectarse cuando exista endpoint suficiente."
                  status="parcial"
                  sections={[
                    { title: "Listado previsto", detail: "Pedido, estado, tracking, transportista y última actualización." },
                    { title: "Detalle previsto", detail: "Acceso a ver detalle y marcar entregado cuando el flujo real lo soporte." },
                    { title: "Estado actual", detail: "El módulo está diseñado pero depende de una fuente backend más completa para poblar la bandeja." },
                  ]}
                />
              }
            />
            <Route
              path="/logistica/incidencias"
              element={
                <LogisticaPreparedModulePage
                  eyebrow="Logistica MD"
                  title="Incidencias logísticas"
                  description="Módulo futuro preparado para retiros fallidos, despachos observados, problemas de tracking, dirección inválida, cliente ausente y otras incidencias de entrega."
                  status="pendiente"
                  sections={[
                    { title: "Filtros previstos", detail: "Tipo, estado, pedido, fecha, transportista y criticidad." },
                    { title: "Bandeja prevista", detail: "Listado vacío hasta contar con persistencia real de incidencias." },
                    { title: "Resolución futura", detail: "Caja lateral preparada para observaciones, seguimiento y cierre del caso." },
                  ]}
                />
              }
            />
            <Route
              path="/logistica/historial"
              element={
                <LogisticaPreparedModulePage
                  eyebrow="Logistica MD"
                  title="Historial reciente"
                  description="Pantalla preparada para futuros listados de retiros realizados, despachos, entregas confirmadas y acciones recientes del rol logístico."
                  status="pendiente"
                  sections={[
                    { title: "Tabla prevista", detail: "Tipo de acción, pedido, fecha, actor, estado final y referencia." },
                    { title: "Filtros previstos", detail: "Rango de fechas, tipo de evento, transportista y pedido." },
                    { title: "Estado actual", detail: "Visible como módulo futuro, sin simular actividad persistida que todavía no existe." },
                  ]}
                />
              }
            />
            <Route path="/notificaciones" element={<NotificacionesPage />} />
            <Route path="/rma" element={<RmaPage />} />
            <Route path="/rma/:rmaId" element={<RmaDetallePage />} />
          </Route>
        </Route>
        <Route path="*" element={<AppFallbackRoute />} />
      </Routes>
    </BrowserRouter>
  )
}
