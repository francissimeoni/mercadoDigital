import { useEffect, useRef, useState } from "react"
import { useMutation } from "@tanstack/react-query"
import type { CountryCode } from "libphonenumber-js/max"
import { guardarAsesor, type Asesor } from "../api/storefrontOwnerApi"
import ContactAvatar from "../components/ContactAvatar"
import { catalogoError, confirmCatalogoLeave, useCatalogoDraft } from "../hooks/useCatalogoDraft"
import { parseCommercialPhone, phoneCountries, phoneCountry } from "./contactPhone"
import { normalizeCommercialPhone } from "./whatsapp"
import { button, field, secondary, DashboardState } from "./ui"

const provinces = ["Buenos Aires", "Ciudad Autónoma de Buenos Aires", "Catamarca", "Chaco", "Chubut", "Córdoba", "Corrientes", "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja", "Mendoza", "Misiones", "Neuquén", "Río Negro", "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe", "Santiago del Estero", "Tierra del Fuego", "Tucumán"]
export default function AdvisorEditor({ id, advisor, onSaved, onClose }: { id: string; advisor?: Asesor; onSaved: () => Promise<unknown>; onClose: () => void }) {
  const initial = { nombre: advisor?.nombre ?? "", whatsappComercial: advisor?.whatsappComercial ?? "", provincia: advisor?.provincia ?? "", localidad: advisor?.localidad ?? "", zonaAtencion: advisor?.zonaAtencion ?? "", activo: advisor?.activo ?? true, email: "", password: "" }
  const [form, setForm] = useState(initial)
  const [country, setCountry] = useState<CountryCode>(() => phoneCountry(advisor?.whatsappComercial))
  const [mobile, setMobile] = useState(true)
  const [access, setAccess] = useState(false)
  const [photo, setPhoto] = useState<File | null>(null)
  const [removePhoto, setRemovePhoto] = useState(false)
  const [photoUrl, setPhotoUrl] = useState<string | null>(null)
  const [photoError, setPhotoError] = useState("")
  const input = useRef<HTMLInputElement>(null)
  const nameInput = useRef<HTMLInputElement>(null)
  const sending = useRef(false)
  useEffect(() => { nameInput.current?.focus() }, [])
  useEffect(() => {
    if (!photoUrl) return
    return () => { URL.revokeObjectURL(photoUrl) }
  }, [photoUrl])
  const dirty = JSON.stringify(form) !== JSON.stringify(initial) || photo !== null || removePhoto
  useCatalogoDraft(dirty)
  const phone = parseCommercialPhone(form.whatsappComercial, country, mobile)
  const unchangedPhone = Boolean(advisor && form.whatsappComercial === advisor.whatsappComercial)
  const normalized = phone?.number ?? (unchangedPhone ? normalizeCommercialPhone(form.whatsappComercial) : null)
  const invalidPhone = Boolean(form.whatsappComercial.trim() && !normalized)
  const save = useMutation({ mutationFn: () => guardarAsesor(id, advisor?.id ?? null, {
    ...form, whatsappComercial: unchangedPhone ? advisor!.whatsappComercial ?? "" : normalized ?? "",
    paisTelefono: country, telefonoMovil: mobile, quitarFoto: removePhoto,
    email: !advisor && access ? form.email.trim() : undefined, password: !advisor && access ? form.password : undefined,
  }, photo), onSuccess: async () => { await onSaved(); onClose() }, onSettled: () => { sending.current = false } })
  useCatalogoDraft(save.isPending)
  const update = <K extends keyof typeof form>(key: K, value: typeof form[K]) => setForm(old => ({ ...old, [key]: value }))
  return <form className="dashboard-panel advisor-editor" aria-label={advisor ? "Editar asesor" : "Nuevo asesor"} onSubmit={event => {
    event.preventDefault(); if (sending.current || invalidPhone || photoError || !form.nombre.trim()) return
    sending.current = true; save.mutate()
  }}>
    <div className="dashboard-section-heading"><h2>{advisor ? "Editar asesor" : "Nuevo asesor"}</h2><button type="button" className="dashboard-link" disabled={save.isPending} onClick={() => { if (confirmCatalogoLeave()) onClose() }}>Cancelar</button></div>
    <fieldset disabled={save.isPending} className="dashboard-fields"><legend className="sr-only">Datos comerciales</legend>
      <label>Nombre<input ref={nameInput} required maxLength={120} className={field} value={form.nombre} onChange={e => update("nombre", e.target.value)} /></label>
      <div className="advisor-photo-editor"><ContactAvatar name={form.nombre} src={photo ? photoUrl : removePhoto ? null : advisor?.fotoUrl} /><div><button type="button" className={secondary} onClick={() => input.current?.click()}>{photo || (advisor?.fotoUrl && !removePhoto) ? "Cambiar foto" : "Agregar foto"}</button>{(photo || (advisor?.fotoUrl && !removePhoto)) && <button type="button" className="dashboard-link" onClick={() => { setPhoto(null); setPhotoUrl(null); setRemovePhoto(true); setPhotoError("") }}>Quitar foto</button>}<p className="dashboard-help">PNG, JPG o WebP · Máximo 5 MB</p><input ref={input} type="file" className="sr-only" aria-label="Foto del asesor" accept="image/png,image/jpeg,image/webp" onChange={e => {
        const file = e.target.files?.[0]; e.target.value = ""; if (!file) return
        if (!file.size || file.size > 5 * 1024 * 1024 || !["image/png", "image/jpeg", "image/webp"].includes(file.type)) { setPhotoError("Elegí una imagen PNG, JPG o WebP de hasta 5 MB."); return }
        setPhoto(file); setPhotoUrl(URL.createObjectURL(file)); setRemovePhoto(false); setPhotoError("")
      }} /></div></div>
      {photoError && <p role="alert" className="dashboard-error">{photoError}<button type="button" className="dashboard-link" onClick={() => setPhotoError("")}>Cerrar aviso</button></p>}
      <div className="dashboard-two-fields"><label>País del teléfono<select aria-label="País del teléfono" className={field} value={country} onChange={e => setCountry(e.target.value as CountryCode)}>{phoneCountries.map(c => <option key={c.code} value={c.code}>{c.name}</option>)}</select></label><label>WhatsApp comercial<input className={field} type="tel" maxLength={40} autoComplete="tel" value={form.whatsappComercial} onChange={e => update("whatsappComercial", e.target.value)} placeholder="Código de área y número" aria-invalid={invalidPhone} aria-describedby="advisor-phone-status" /></label></div>
      {country === "AR" && <label className="dashboard-check"><input type="checkbox" checked={mobile} onChange={e => setMobile(e.target.checked)} />Es un celular argentino</label>}
      <div id="advisor-phone-status" aria-live="polite">{invalidPhone ? <p className="dashboard-error">No pudimos interpretar el número. Revisá país, código de área y número completo.</p> : normalized ? <div className="advisor-phone-preview"><span>Se usará: <strong>{phone?.formatted ?? normalized}</strong></span><a className="dashboard-link" href={`https://wa.me/${normalized.replace(/\D/g, "")}`} target="_blank" rel="noreferrer">Probar contacto</a></div> : <p className="dashboard-help">WhatsApp pendiente</p>}</div>
      <div className="dashboard-form-section"><h3>Zona de atención <span className="dashboard-help">Opcional</span></h3><div className="dashboard-two-fields"><label>Provincia<input className={field} list="advisor-provinces" maxLength={80} value={form.provincia} onChange={e => update("provincia", e.target.value)} /></label><label>Localidad principal<input className={field} maxLength={120} value={form.localidad} onChange={e => update("localidad", e.target.value)} /></label></div><datalist id="advisor-provinces">{provinces.map(p => <option key={p} value={p} />)}</datalist><label>Detalle de la zona<input className={field} maxLength={200} value={form.zonaAtencion} placeholder="Ej. Córdoba capital y alrededores" onChange={e => update("zonaAtencion", e.target.value)} /></label></div>
      <label className="dashboard-check"><input type="checkbox" checked={form.activo} onChange={e => update("activo", e.target.checked)} />Asesor activo</label>
      {advisor ? <p className="dashboard-help">{advisor.email ? `Acceso al panel: ${advisor.email}` : "Sin acceso al panel"}</p> : <details className="dashboard-form-section"><summary>Acceso al panel · opcional</summary><label className="dashboard-check"><input type="checkbox" checked={access} onChange={e => setAccess(e.target.checked)} />Crear también una cuenta de acceso</label>{access && <div className="dashboard-two-fields"><label>Email de acceso<input required type="email" className={field} value={form.email} onChange={e => update("email", e.target.value)} /></label><label>Contraseña inicial<input required type="password" minLength={6} autoComplete="new-password" className={field} value={form.password} onChange={e => update("password", e.target.value)} /></label></div>}</details>}
    </fieldset>
    {save.isError && <DashboardState error>{catalogoError(save.error)}</DashboardState>}
    <div className="dashboard-form-actions"><button type="submit" className={button} disabled={save.isPending || invalidPhone || Boolean(photoError) || !form.nombre.trim() || Boolean(advisor && !dirty)}>{save.isPending ? "Guardando…" : advisor ? "Guardar asesor" : "Crear asesor"}</button></div>
  </form>
}
