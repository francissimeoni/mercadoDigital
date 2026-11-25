import { lazy, Suspense, useRef, useState } from "react"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { listarAsesores } from "../api/storefrontOwnerApi"
import CatalogoAdminSection from "../components/CatalogoAdminSection"
import ContactAvatar from "../components/ContactAvatar"
import { contactStatus, normalizeCommercialPhone } from "./whatsapp"
import { DashboardHeading, DashboardState, button } from "./ui"

const AdvisorEditor = lazy(() => import("./AdvisorEditor"))

export default function OwnerContactsSection({ id, notify }: { id: string; notify: (text: string) => void }) {
  const client = useQueryClient()
  const query = useQuery({ queryKey: ["owner-advisors", id], queryFn: () => listarAsesores(id), refetchOnWindowFocus: false })
  const [editing, setEditing] = useState<string | null>(null)
  const opener = useRef<HTMLButtonElement | null>(null)
  const close = () => { setEditing(null); window.requestAnimationFrame(() => opener.current?.focus()) }
  const advisor = query.data?.find(a => a.id === editing)
  return <section className="dashboard-section"><DashboardHeading title="Asesores y contacto" />
    <CatalogoAdminSection id={id} modoCatalogo view="contact" notify={notify} />
    <div className="dashboard-section-heading"><h2>Asesores de esta tienda</h2>{!editing && <button className={button} onClick={e => { opener.current = e.currentTarget; setEditing("nuevo") }}>Agregar asesor</button>}</div>
    {editing && <Suspense fallback={<DashboardState>Cargando ficha del asesor…</DashboardState>}><AdvisorEditor key={editing} id={id} advisor={advisor} onClose={close} onSaved={async () => { await client.invalidateQueries({ queryKey: ["owner-advisors", id] }); notify("Asesor guardado.") }} /></Suspense>}
    {query.isPending ? <DashboardState>Cargando asesores…</DashboardState> : query.isError ? <DashboardState error retry={() => void query.refetch()}>No se pudieron cargar los asesores.</DashboardState> : !query.data.length ? <DashboardState>Todavía no hay asesores. Agregá el primero para recibir consultas.</DashboardState> : <div className="advisor-list">{query.data.map(a => {
      const ready = a.activo && Boolean(normalizeCommercialPhone(a.whatsappComercial))
      return <article key={a.id} className="advisor-row"><ContactAvatar name={a.nombre} src={a.fotoUrl} /><div className="advisor-row-content"><h3>{a.nombre}</h3>{[a.localidad, a.provincia].filter(Boolean).length > 0 && <p>{[a.localidad, a.provincia].filter(Boolean).join(" · ")}</p>}{a.zonaAtencion && <p>{a.zonaAtencion}</p>}<p className="dashboard-help">{a.whatsappComercial ?? "WhatsApp pendiente"}</p><span className={`product-status ${ready ? "is-published" : ""}`}>{contactStatus(a.activo, a.whatsappComercial)}</span></div><button type="button" className="dashboard-link" disabled={Boolean(editing)} aria-label={`Editar asesor ${a.nombre}`} onClick={e => { opener.current = e.currentTarget; setEditing(a.id) }}>Editar</button></article>
    })}</div>}
  </section>
}
