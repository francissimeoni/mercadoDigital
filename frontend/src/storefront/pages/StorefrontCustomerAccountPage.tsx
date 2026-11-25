import { useState, type FormEvent } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, Navigate, useNavigate } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"
import { getCustomerOrders, getCustomerProfile, updateCustomerProfile, type StorefrontCustomerOrder, type StorefrontCustomerProfile, type StorefrontCustomerProfileInput } from "../api/storefrontCustomerApi"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"
import { normalizeWhatsAppPhone } from "../utils/storefrontContact"
import StorefrontErrorState from "../components/StorefrontErrorState"

// Behavior and fields recovered from FA, VA, GA and QA; layout rebuilt as readable JSX.
const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })
const date = new Intl.DateTimeFormat("es-AR", { dateStyle: "medium" })
const label = (value?: string | null) => value ? value.toLowerCase().replaceAll("_", " ").replace(/^./, c => c.toUpperCase()) : "Pendiente"

export default function StorefrontCustomerAccountPage() {
  const tenant = useTenant()
  const { session, logout } = useStorefrontAuth()
  const navigate = useNavigate()
  const cache = useQueryClient()
  const modoCatalogo = Boolean(tenant.storefront?.modoCatalogo)
  const accent = tenant.branding?.colorPrimario || "#0f172a"
  const isCustomer = !!(session?.roles.includes("CLIENTE") && session.clienteId)
  const tenantReady = tenant.status === "resolved" && Boolean(tenant.storefront && tenant.host)
  const profileKey = ["storefront-customer-profile", tenant.host, session?.clienteId]
  const orders = useQuery({ queryKey: ["storefront-customer-orders", tenant.host, session?.clienteId], queryFn: () => getCustomerOrders(tenant.host!), enabled: !!(isCustomer && tenantReady) })
  const profile = useQuery({ queryKey: profileKey, queryFn: () => getCustomerProfile(tenant.host!), enabled: !!(isCustomer && tenantReady) })
  const save = useMutation({ mutationFn: (input: StorefrontCustomerProfileInput) => updateCustomerProfile({ ...input, storefrontHost: tenant.host! }), onSuccess: result => cache.setQueryData(profileKey, result) })
  if (tenant.isLoading || tenant.status === "idle" || tenant.status === "loading") return <p role="status">Cargando cuenta de la tienda...</p>
  if (!tenantReady) return <StorefrontErrorState title="Tienda no disponible" message="No se pudo resolver la tienda para consultar tus pedidos." actionLabel="Reintentar" onAction={() => void tenant.refresh()} />
  if (!session) return <Navigate to="/ingresar?redirect=/mi-cuenta" replace />
  if (!isCustomer) return <Navigate to={session.roles.includes("REVENDEDOR") ? "/mi-tienda" : "/"} replace />
  const store = tenant.branding?.nombreComercial?.trim() || "la tienda"
  const phone = tenant.branding?.telefonoComercial ? normalizeWhatsAppPhone(tenant.branding.telefonoComercial) : null
  return <section className="mx-auto max-w-5xl space-y-5">
    <header className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
      <div className="p-6 text-white" style={{ backgroundColor: accent }}>
        <p className="text-xs font-bold uppercase">Mi cuenta</p><h1 className="mt-2 text-3xl font-bold">Hola, {session.nombre}</h1>
        <p className="mt-2 text-sm">{modoCatalogo ? `Consultá tus pedidos anteriores con ${store}. Las compras nuevas se coordinan directamente por WhatsApp desde el catálogo.` : `Tus compras, pagos, envíos y gestiones con ${store}, siempre disponibles.`}</p>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 break-all p-5"><p>{session.email}</p><button className="rounded-full border px-4 py-2" onClick={() => { logout(); navigate("/", { replace: true }) }}>Cerrar sesión</button></div>
    </header>
    {profile.isLoading ? <div className="h-32 animate-pulse rounded-3xl bg-slate-200" /> : null}
    {profile.isError && <div role="alert" className="rounded-2xl border border-red-200 p-4 text-sm text-red-800">No pudimos cargar tus datos. <button type="button" className="underline" onClick={() => void profile.refetch()}>Reintentar datos</button></div>}
    {profile.data ? <CustomerProfile profile={profile.data} saving={save.isPending} failed={profile.isError || save.isError} saved={save.isSuccess} accent={accent} onSave={input => save.mutate(input)} /> : null}
    <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-2xl font-bold">{modoCatalogo ? "Pedidos anteriores" : "Mis compras"}</h2><Link to="/catalogo" className="rounded-full border px-4 py-2">{modoCatalogo ? "Ver catálogo" : "Seguir comprando"}</Link></div>
    {orders.isLoading ? <div className="h-52 animate-pulse rounded-3xl bg-slate-200" /> : null}
    {orders.isError ? <div role="alert" className="rounded-3xl bg-red-50 p-6 text-red-700"><p>No pudimos cargar tus pedidos.</p><button type="button" className="mt-2 font-semibold underline" onClick={() => void orders.refetch()}>Reintentar</button></div> : null}
    {orders.data?.length === 0 ? <div className="rounded-3xl border border-dashed p-10 text-center"><h3>{modoCatalogo ? "No tenés pedidos anteriores en esta tienda" : "Todavía no tenés compras en esta tienda"}</h3><p className="mt-2 text-sm text-slate-600">{modoCatalogo ? "Las conversaciones y ventas acordadas por WhatsApp no se registran automáticamente en esta cuenta." : "Cuando compres usando este email, el pedido aparecerá automáticamente acá."}</p></div> : null}
    {orders.data?.map(order => <CustomerOrder key={order.pedidoId} order={order} store={store} phone={phone} accent={accent} />)}
  </section>
}

const fields = [
  ["nombre", "Nombre completo"], ["telefonoWhatsapp", "WhatsApp"], ["provincia", "Provincia"],
  ["localidad", "Localidad"], ["codigoPostal", "Código postal"], ["calle", "Calle"], ["numero", "Número"], ["pisoDepto", "Piso / depto."],
] as const

function CustomerProfile({ profile, saving, failed, saved, accent, onSave }: {
  profile: StorefrontCustomerProfile; saving: boolean; failed: boolean; saved: boolean; accent: string; onSave: (input: StorefrontCustomerProfileInput) => void
}) {
  const [editing, setEditing] = useState(false)
  return <section className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
    <div className="flex items-center justify-between p-5"><h2 className="text-xl font-bold">Contacto y entrega</h2><button className="rounded-full border px-4 py-2" onClick={() => setEditing(value => !value)}>{editing ? "Cerrar" : "Editar datos"}</button></div>
    {editing ? <ProfileForm profile={profile} saving={saving} failed={failed} saved={saved} accent={accent} onSave={onSave} /> : <dl className="grid gap-4 break-words border-t bg-slate-50 p-5 sm:grid-cols-3">
      {[["Nombre", profile.nombre], ["Email de acceso", profile.email], ["WhatsApp", profile.telefonoWhatsapp || "Sin informar"],
        ["Dirección", profile.calle && profile.numero ? `${profile.calle} ${profile.numero}${profile.pisoDepto ? `, ${profile.pisoDepto}` : ""}` : "Sin informar"],
        ["Localidad", profile.localidad && profile.provincia ? `${profile.localidad}, ${profile.provincia}${profile.codigoPostal ? ` (${profile.codigoPostal})` : ""}` : "Sin informar"],
        ["Indicaciones", profile.observacionesEnvio || "Sin indicaciones"]].map(([name, value]) => <div key={name}><dt className="text-xs text-slate-500">{name}</dt><dd>{value}</dd></div>)}
    </dl>}
  </section>
}

function ProfileForm({ profile, saving, failed, saved, accent, onSave }: {
  profile: StorefrontCustomerProfile; saving: boolean; failed: boolean; saved: boolean; accent: string; onSave: (input: StorefrontCustomerProfileInput) => void
}) {
  const [form, setForm] = useState<StorefrontCustomerProfileInput>(() => ({ nombre: profile.nombre, telefonoWhatsapp: profile.telefonoWhatsapp || "", provincia: profile.provincia || "", localidad: profile.localidad || "", codigoPostal: profile.codigoPostal || "", calle: profile.calle || "", numero: profile.numero || "", pisoDepto: profile.pisoDepto || "", observacionesEnvio: profile.observacionesEnvio || "" }))
  const change = (key: keyof StorefrontCustomerProfileInput, value: string) => setForm(previous => ({ ...previous, [key]: value }))
  function submit(event: FormEvent) { event.preventDefault(); onSave(form) }
  return <form onSubmit={submit} className="grid gap-3 border-t bg-slate-50 p-5 sm:grid-cols-2">
    {fields.map(([key, name]) => <label key={key} className="text-xs text-slate-600">{name}<input required={key !== "pisoDepto"} className="mt-1 w-full rounded-xl border p-3 text-sm" value={form[key]} onChange={event => change(key, event.target.value)} /></label>)}
    <label className="text-xs text-slate-600 sm:col-span-2">Indicaciones de entrega<textarea className="mt-1 w-full rounded-xl border p-3" rows={2} value={form.observacionesEnvio} onChange={event => change("observacionesEnvio", event.target.value)} /></label>
    {failed ? <p className="text-red-600 sm:col-span-2">No pudimos guardar los datos. Revisalos e intentá nuevamente.</p> : null}
    {saved ? <p className="text-emerald-700 sm:col-span-2">Datos actualizados correctamente.</p> : null}
    <button disabled={saving} className="rounded-full px-5 py-3 font-bold text-white disabled:opacity-50 sm:col-span-2" style={{ backgroundColor: accent }}>{saving ? "Guardando..." : "Guardar datos"}</button>
  </form>
}

function CustomerOrder({ order, store, phone, accent }: { order: StorefrontCustomerOrder; store: string; phone: string | null; accent: string }) {
  const item = order.items[0]
  const id = order.pedidoId.slice(0, 8).toUpperCase()
  const trackingUrl = `${window.location.origin}/pedido/${order.trackingToken}`
  const whatsapp = phone ? `https://wa.me/${phone}?text=${encodeURIComponent([`Hola ${store}, necesito ayuda con el pedido #${id}.`, item ? `Producto: ${item.nombreProducto}` : null, `Estado de pago: ${label(order.estadoPago)}`, `Seguimiento: ${trackingUrl}`].filter(Boolean).join("\n"))}` : null
  return <article className="overflow-hidden rounded-3xl border border-slate-200 bg-white">
    <div className="grid grid-cols-[64px_minmax(0,1fr)] items-start gap-4 p-5 sm:flex sm:flex-wrap sm:items-center">
      {item?.imagenPrincipal ? <img className="h-16 w-16 rounded-2xl object-cover sm:h-20 sm:w-20" src={resolveStorefrontAssetUrl(item.imagenPrincipal) || item.imagenPrincipal} alt={item.nombreProducto} /> : <span className="flex h-16 w-16 items-center justify-center rounded-2xl bg-slate-50 px-2 text-center text-xs text-slate-400">Sin imagen</span>}
      <div className="min-w-0 flex-1 break-words"><p className="font-bold">Pedido #{id}</p><p>{label(order.estadoPago)} · {label(order.envio?.estadoEnvio || order.estadoPedido)}</p><h3>{item?.nombreProducto || "Pedido"}{order.items.length > 1 ? ` y ${order.items.length - 1} producto(s) más` : ""}</h3><p className="text-xs text-slate-500">{date.format(new Date(order.fechaCreacion))} · {label(order.metodoPago)}</p></div>
      <p className="col-span-2 text-xl font-bold sm:ml-auto">{money.format(order.totalFinal)}</p>
    </div>
    <div className="flex flex-wrap gap-2 border-t bg-slate-50 p-4">
      <Link className="rounded-full px-4 py-2 font-bold text-white" style={{ backgroundColor: accent }} to={`/pedido/${order.trackingToken}`}>Ver seguimiento</Link>
      {order.transferenciaToken && !order.comprobanteCargado && order.estadoPago === "PENDIENTE" ? <Link className="rounded-full border px-4 py-2 text-blue-700" to={`/transferencia/${order.transferenciaToken}`}>Cargar comprobante</Link> : null}
      {whatsapp ? <a className="rounded-full border px-4 py-2 text-emerald-700" href={whatsapp} target="_blank" rel="noreferrer">Tengo un problema</a> : null}
    </div>
  </article>
}
