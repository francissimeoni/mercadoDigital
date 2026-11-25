import type { ReactNode } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link } from "react-router-dom"
import { useAuth } from "../../auth/useAuth"
import { getRevendedorPricingConfig } from "../../api/pricingApi"
import StorefrontErrorState from "./StorefrontErrorState"

/** Accesos privados del modelo anterior; el servicio de escritura conserva su propio bloqueo. */
export default function CatalogoOperacionGate({ children }: { children: ReactNode }) {
  const id = useAuth().session?.revendedorId
  const config = useQuery({ queryKey: ["catalogo-operacion-modo", id], queryFn: () => getRevendedorPricingConfig(id!), enabled: Boolean(id), staleTime: 0, refetchOnMount: "always", refetchOnWindowFocus: false, retry: false })
  if (!id) return <StorefrontErrorState title="Cuenta no disponible" message="Ingresá con la cuenta del responsable de la tienda para continuar." />
  if (config.isPending || config.isFetching || !config.isFetchedAfterMount) return <p role="status">Verificando opciones de la tienda...</p>
  if (config.isError || config.data?.revendedorId !== id || typeof config.data?.modoCatalogo !== "boolean") return <StorefrontErrorState title="No se pudieron verificar las opciones" message="Reintentá para consultar la configuración actual de tu tienda." actionLabel="Reintentar" onAction={() => void config.refetch()} />
  if (config.data.modoCatalogo) return <section className="mx-auto max-w-2xl rounded-3xl border border-emerald-200 bg-white p-6 shadow-sm sm:p-8">
    <h1 className="text-2xl font-semibold">Tu tienda funciona como catálogo</h1>
    <p className="mt-3 leading-7 text-slate-600">Las compras nuevas se coordinan por WhatsApp. Para cargar productos y configurar precios, moneda y envío, ingresá al panel de tu tienda.</p>
    <p className="mt-3 text-sm text-slate-600">Tus pedidos anteriores conservan su seguimiento y gestión.</p>
    <Link to="/pedidos" className="mt-5 inline-flex rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white">Ver pedidos anteriores</Link>
  </section>
  return children
}
