import type { ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { Navigate, useLocation, useParams } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import { resolveStorefront } from "../../tenant/tenantApi"
import StorefrontErrorState from "./StorefrontErrorState"
import { getCheckoutStorageKey, getStorefrontCheckoutKey, readCheckoutState } from "../utils/storefrontCheckoutState"

/** Impide montar el checkout (incluidos sus efectos) en tiendas de catálogo. */
export default function CatalogoCompraGate({ children }: { children: ReactNode }) {
  const tenant = useTenant()
  const { productoCanonicoId } = useParams()
  const location = useLocation()
  // La resolución del contexto puede pertenecer a una pestaña abierta antes del cambio de modo.
  const actual = useQuery({
    queryKey: ["storefront-compra-modo", tenant.host],
    queryFn: () => resolveStorefront(tenant.host!),
    enabled: tenant.status === "resolved" && Boolean(tenant.host && tenant.storefront) && !tenant.storefront?.modoCatalogo,
    staleTime: 0,
    refetchOnMount: "always",
    refetchOnWindowFocus: false,
    retry: false,
  })
  if (tenant.isLoading || tenant.status === "idle" || tenant.status === "loading") return <p role="status">Cargando tienda...</p>
  if (tenant.status !== "resolved" || !tenant.storefront || !tenant.host) return <StorefrontErrorState title="Tienda no disponible" message="No se pudo resolver la tienda para continuar." actionLabel="Reintentar" onAction={() => void tenant.refresh()} />
  if (!tenant.storefront.modoCatalogo && (actual.isPending || actual.isFetching || !actual.isFetchedAfterMount)) return <p role="status">Verificando formas de compra...</p>
  const storefront = tenant.storefront.modoCatalogo ? tenant.storefront : actual.data
  const mismaTienda = storefront?.tipoStorefront === tenant.tipoStorefront && (tenant.tipoStorefront === "PROVEEDOR" ? storefront?.proveedorId === tenant.proveedorId : storefront?.revendedorId === tenant.revendedorId)
  if (!storefront || (!tenant.storefront.modoCatalogo && actual.isError) || !mismaTienda || (tenant.tipoStorefront === "REVENDEDOR" && typeof storefront.modoCatalogo !== "boolean")) return <StorefrontErrorState title="No se pudo verificar la compra" message="Reintentá para consultar las opciones actuales de esta tienda." actionLabel="Reintentar" onAction={() => void actual.refetch()} />
  if (storefront.modoCatalogo) {
    const key = getStorefrontCheckoutKey(tenant.tipoStorefront, tenant.revendedorId, tenant.proveedorId)
    const previous = key && productoCanonicoId ? readCheckoutState(getCheckoutStorageKey(key, productoCanonicoId))?.pedidoCreado : null
    if (typeof previous?.trackingToken === "string" && previous.trackingToken.trim()) {
      // El destino consulta al backend con token y host; ningún dato guardado autoriza pagos.
      return <Navigate replace to={`/pedido/${encodeURIComponent(previous.trackingToken)}?origen=compra-anterior`} />
    }
    return <Navigate replace to={{ pathname: productoCanonicoId ? `/producto/${encodeURIComponent(productoCanonicoId)}` : "/catalogo", search: location.search }} state={{ compraCatalogo: true, pedidoSinEnlace: Boolean(previous?.pedidoId) }} />
  }
  return children
}
