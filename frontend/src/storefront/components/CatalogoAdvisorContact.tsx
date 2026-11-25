import { useState } from "react"
import { useMutation } from "@tanstack/react-query"
import { actualizarAsesor, type Asesor } from "../api/storefrontOwnerApi"
import { normalizeCommercialPhone } from "../dashboard/whatsapp"
import { field, secondary } from "../dashboard/ui"
import { catalogoError, useCatalogoDraft } from "../hooks/useCatalogoDraft"

export default function CatalogoAdvisorContact({ id, advisor, refresh }: { id: string; advisor: Asesor; refresh: () => Promise<unknown> }) {
  const [phone, setPhone] = useState(advisor.whatsappComercial ?? "")
  const [editing, setEditing] = useState(false)
  useCatalogoDraft(editing && phone !== (advisor.whatsappComercial ?? ""))
  const save = useMutation({ mutationFn: () => actualizarAsesor(id, advisor.id, { nombre: advisor.nombre, fotoUrl: advisor.fotoUrl, activo: advisor.activo, whatsappComercial: phone }), onSuccess: async saved => { setPhone(saved.whatsappComercial ?? ""); setEditing(false); await refresh() } })
  useCatalogoDraft(save.isPending)
  const invalid = Boolean(phone.trim() && !normalizeCommercialPhone(phone))
  return <div className="mt-2 min-w-0 text-sm">
    <p className={advisor.whatsappComercial ? "text-slate-600" : "text-amber-800"}>{advisor.whatsappComercial ? `WhatsApp: ${advisor.whatsappComercial}` : "WhatsApp pendiente para el catálogo"}</p>
    {!editing ? <button type="button" onClick={() => { setPhone(advisor.whatsappComercial ?? ""); setEditing(true) }} className="mt-1 min-h-11 font-semibold text-emerald-800 underline">Editar WhatsApp</button> : <div className="mt-2 space-y-2">
      <label className="block">WhatsApp comercial<input type="tel" maxLength={40} className={field} value={phone} disabled={save.isPending} onChange={e => setPhone(e.target.value)} placeholder="+54 9 11 1234 5678" /></label>
      <p>Usá el número internacional. Guardarlo no verifica que tenga WhatsApp activo.</p>
      {invalid && <p role="alert" className="text-amber-900">Número inválido o ambiguo. Usá formato internacional con código de país y área.</p>}
      <div className="flex flex-wrap gap-3"><button type="button" disabled={save.isPending || invalid} onClick={() => save.mutate()} className={secondary}>{save.isPending ? "Guardando..." : "Guardar contacto"}</button><button type="button" disabled={save.isPending} onClick={() => { setPhone(advisor.whatsappComercial ?? ""); setEditing(false) }}>Cancelar</button></div>
      {save.isError && <p role="alert" className="text-red-700">{catalogoError(save.error)}</p>}
    </div>}
  </div>
}
