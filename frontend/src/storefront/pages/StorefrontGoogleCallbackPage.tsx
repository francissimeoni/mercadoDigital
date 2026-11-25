import { useEffect } from "react"
import { useQuery } from "@tanstack/react-query"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import axiosClient from "../../api/axiosClient"
import { useTenant } from "../../tenant/useTenant"
import { saveStorefrontSession, type StorefrontSession } from "../auth/storefrontSession"

// Recovered from production function KA (offset 912099).
export default function StorefrontGoogleCallbackPage() {
  const tenant = useTenant()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const ticket = params.get("ticket")
  const error = params.get("error")
  const exchange = useQuery({
    queryKey: ["storefront-google-exchange", tenant.host, ticket],
    queryFn: async () => (await axiosClient.post<{ session: StorefrontSession; returnPath: string }>(
      "/public/storefront/auth/google/canjear", { ticket, storefrontHost: tenant.host },
    )).data,
    enabled: !!(ticket && tenant.host && !error),
    retry: false,
    staleTime: Infinity,
  })
  useEffect(() => {
    if (!exchange.data) return
    saveStorefrontSession(exchange.data.session)
    const returnPath = exchange.data.returnPath
    navigate(returnPath.startsWith("/") && !returnPath.startsWith("//") ? returnPath : "/mi-cuenta", { replace: true })
  }, [exchange.data, navigate])
  const failed = !!(error || !ticket || exchange.isError)
  return <section className="mx-auto max-w-lg py-10">
    <div className="rounded-3xl border border-slate-200 bg-white p-7 text-center shadow-xl sm:p-9">
      {failed ? <>
        <h1 className="text-2xl font-bold text-slate-950">No pudimos completar el acceso</h1>
        <p className="mt-2 text-sm text-slate-600">El acceso fue cancelado, venció o no pudo validarse. Podés intentarlo nuevamente.</p>
        <Link to="/ingresar" className="mt-6 inline-flex rounded-full bg-slate-950 px-5 py-3 font-bold text-white">Volver a ingresar</Link>
      </> : <>
        <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-slate-200 border-t-blue-600" />
        <h1 className="mt-5 text-2xl font-bold">Validando tu cuenta de Google</h1>
        <p className="mt-2 text-sm text-slate-600">En unos segundos vas a volver a la tienda.</p>
      </>}
    </div>
  </section>
}
