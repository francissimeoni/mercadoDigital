import { useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useAuth } from "../auth/useAuth"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useMarcarNotificacionLeida, useNotificaciones } from "../hooks/useNotificaciones"

const pedidoUuidRegex =
  /\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\b/

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

function getPedidoIdContextual(tipo: string, titulo: string, mensaje: string) {
  const texto = `${titulo} ${mensaje}`
  const match = texto.match(pedidoUuidRegex)

  if (match && tipo.toUpperCase().includes("PEDIDO")) {
    return match[0]
  }

  return null
}

export default function NotificacionesPage() {
  const { session } = useAuth()
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null)
  const [searchParams] = useSearchParams()
  const usuarioId =
    searchParams.get("usuarioId") || localStorage.getItem("usuarioId") || import.meta.env.VITE_USUARIO_ID || null
  const { data, isLoading, isError, refetch } = useNotificaciones(usuarioId)
  const marcarLeida = useMarcarNotificacionLeida(usuarioId)
  const esAdmin = session?.roles.includes("ADMIN_MD") ?? false
  const esProveedor = session?.roles.includes("PROVEEDOR") ?? false
  const titulo = esAdmin ? "Notificaciones admin" : esProveedor ? "Notificaciones proveedor" : "Notificaciones"
  const subtitulo = esAdmin
    ? "Eventos operativos del panel admin y alertas reales del usuario actual."
    : esProveedor
      ? "Eventos operativos del panel proveedor."
      : "Eventos operativos del usuario actual."
  const fallbackLink = useMemo(() => {
    if (esAdmin) {
      return { to: "/admin", label: "Ir al dashboard admin" }
    }
    if (esProveedor) {
      return { to: "/proveedor", label: "Ir al dashboard proveedor" }
    }
    return { to: "/pedidos", label: "Ir a pedidos" }
  }, [esAdmin, esProveedor])

  return (
    <section className="space-y-4">
      <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">{titulo}</h2>
        <p className="mt-2 text-sm text-slate-600">{subtitulo}</p>
      </header>

      {!usuarioId ? (
        <FeedbackAlert variant="warning">Falta `usuarioId` para cargar las notificaciones del usuario actual.</FeedbackAlert>
      ) : null}

      {usuarioId && isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-24 animate-pulse rounded-lg border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {usuarioId && isError ? (
        <div className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
          <p className="text-sm text-red-800">No se pudieron cargar las notificaciones.</p>
          <button
            type="button"
            className="mt-4 rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void refetch()
            }}
          >
            Reintentar
          </button>
        </div>
      ) : null}

      {usuarioId && !isLoading && !isError ? (
        data && data.length > 0 ? (
          <div className="space-y-3">
            {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

            {data.map((notificacion) => {
              const pedidoId = getPedidoIdContextual(
                notificacion.tipo,
                notificacion.titulo,
                notificacion.mensaje,
              )
              const contextualLink =
                pedidoId && !esAdmin && !esProveedor
                  ? { to: `/pedidos/${pedidoId}`, label: "Ver pedido" }
                  : notificacion.tipo.toUpperCase().includes("PEDIDO") && !esAdmin && !esProveedor
                    ? { to: "/pedidos", label: "Ir a pedidos" }
                    : fallbackLink

              return (
                <article
                  key={notificacion.id}
                  className={`rounded-lg border p-4 shadow-sm ${
                    notificacion.leida
                      ? "border-slate-200 bg-white"
                      : "border-blue-200 bg-blue-50"
                  }`}
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="text-xs uppercase tracking-wide text-slate-500">
                        {notificacion.tipo}
                      </p>
                      <h3 className="text-base font-semibold text-slate-900">{notificacion.titulo}</h3>
                    </div>
                    <span
                      className={`rounded px-2 py-1 text-xs font-medium ${
                        notificacion.leida
                          ? "bg-slate-200 text-slate-700"
                          : "bg-blue-100 text-blue-700"
                      }`}
                    >
                      {notificacion.leida ? "Leida" : "No leida"}
                    </span>
                  </div>

                  <p className="mt-2 text-sm text-slate-700">{notificacion.mensaje}</p>
                  <p className="mt-2 text-xs text-slate-500">{formatFecha(notificacion.createdAt)}</p>

                  <div className="mt-3 flex flex-wrap gap-2">
                    {!notificacion.leida ? (
                      <button
                        type="button"
                        className="rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70"
                        onClick={() => {
                          setFeedback(null)
                          marcarLeida.mutate(notificacion.id, {
                            onSuccess: () => {
                              setFeedback({ variant: "success", message: "Notificacion marcada como leida." })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo marcar la notificacion como leida. Reintenta." })
                            },
                          })
                        }}
                        disabled={marcarLeida.isPending}
                      >
                        Marcar como leida
                      </button>
                    ) : null}

                      <Link
                        to={contextualLink.to}
                        className="rounded-md bg-white px-3 py-2 text-sm font-medium text-slate-800 ring-1 ring-slate-300 hover:bg-slate-100"
                      >
                        {contextualLink.label}
                      </Link>
                  </div>
                </article>
              )
            })}
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
            No hay notificaciones pendientes para este usuario. Vuelve mas tarde para revisar novedades.
          </div>
        )
      ) : null}
    </section>
  )
}
