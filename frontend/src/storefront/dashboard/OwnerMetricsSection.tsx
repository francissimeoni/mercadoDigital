import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { getStorefrontMetrics, type StorefrontMetrics, type MetricaRanking } from "../api/storefrontOwnerApi"
import ProductThumbnail from "../components/ProductThumbnail"
import { DashboardHeading, DashboardState, field } from "./ui"

function dateInput(date: Date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}` }
function daysAgo(days: number) { const date = new Date(); date.setDate(date.getDate() - days); return dateInput(date) }
export default function MetricsSection({ id, catalogo = false }: { id: string; catalogo?: boolean }) {
  const [desde, setDesde] = useState(() => daysAgo(29))
  const [hasta, setHasta] = useState(() => dateInput(new Date()))
  const [period, setPeriod] = useState("30")
  const valid = Boolean(desde && hasta && desde <= hasta && hasta <= dateInput(new Date()) && (Date.parse(hasta) - Date.parse(desde)) / 86_400_000 < 366)
  const query = useQuery({ queryKey: ["owner-metrics", id, desde, hasta], queryFn: () => getStorefrontMetrics(id, desde, hasta), enabled: valid })
  return <section className="dashboard-section"><DashboardHeading title="Métricas" />
    <div className="dashboard-panel metrics-period"><label>Período<select aria-label="Período" className={field} value={period} onChange={e => { setPeriod(e.target.value); if (e.target.value !== "custom") { setDesde(daysAgo(Number(e.target.value) - 1)); setHasta(dateInput(new Date())) } }}><option value="7">Últimos 7 días</option><option value="30">Últimos 30 días</option><option value="custom">Personalizado</option></select></label><label>Desde<input type="date" className={field} value={desde} max={hasta || dateInput(new Date())} onChange={e => { setPeriod("custom"); setDesde(e.target.value) }} /></label><label>Hasta<input type="date" className={field} value={hasta} min={desde} max={dateInput(new Date())} onChange={e => { setPeriod("custom"); setHasta(e.target.value) }} /></label></div>
    {!valid ? <DashboardState error>Elegí un período de hasta 366 días, con fechas válidas y sin días futuros.</DashboardState> : query.isPending ? <DashboardState>Cargando métricas…</DashboardState> : query.isError || !query.data ? <DashboardState error retry={() => void query.refetch()}>No se pudieron cargar las métricas de este período.</DashboardState> : <><p className="dashboard-help metrics-range">Del {displayDate(desde)} al {displayDate(hasta)}{query.isFetching ? " · Actualizando…" : ""}</p><MetricsDashboard data={query.data} catalogo={catalogo} /></>}
  </section>
}
function displayDate(value: string) { return value.split("-").reverse().join("/") }
function MetricsDashboard({ data, catalogo }: { data: StorefrontMetrics; catalogo: boolean }) {
  const cards = [["Visitantes únicos estimados", data.visitantesUnicos, "Identificados por navegador"], ["Visitas", data.visitas, "Entradas registradas a la tienda"], ["Vistas de productos", data.vistasProductos, "Aperturas de fichas"], ["Clics en WhatsApp", data.clicksWhatsapp, "Intenciones de contacto"], ...(!catalogo ? [["Consultas", data.consultas, "Consultas enviadas"]] : [])] as [string, number, string][]
  return <><div className="metrics-cards">{cards.map(([label, value]) => <article key={label} className="dashboard-panel metric-card"><h2>{label}</h2><p className="metric-value">{value.toLocaleString("es-AR")}</p></article>)}</div>
    <div className="metrics-rankings"><Ranking title="Productos más vistos" items={data.productosMasVistos} products /><Ranking title="Rubros más vistos" items={data.rubrosMasVistos} /></div>
    </>
}
function Ranking({ title, items, products = false }: { title: string; items: MetricaRanking[]; products?: boolean }) {
  const [expanded, setExpanded] = useState(false)
  const maximum = Math.max(1, ...items.map(item => item.total))
  return <article className="dashboard-panel metric-ranking"><h2>{title}</h2>{items.length === 0 ? <p className="dashboard-empty">Todavía no hay vistas registradas en este período.</p> : <ol>{(expanded ? items : items.slice(0, 5)).map((item, index) => <li key={`${item.productoId ?? "rubro"}-${item.nombre}`}><span className="ranking-position">{index + 1}</span>{products && <ProductThumbnail src={item.imagenPrincipalUrl} />}<div className="ranking-name"><p>{item.nombre}</p>{!products && <div className="ranking-track" aria-hidden="true"><span style={{ width: `${item.total / maximum * 100}%` }} /></div>}</div><span className="ranking-total">{item.total.toLocaleString("es-AR")}<span className="dashboard-help">vistas</span></span></li>)}</ol>}{items.length > 5 && <button type="button" className="dashboard-link" aria-expanded={expanded} onClick={() => setExpanded(value => !value)}>{expanded ? "Mostrar menos" : `Ver los ${items.length} resultados`}</button>}</article>
}
