import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import { useEffect, useMemo, useState } from "react"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useAdminProveedorDespachoConfig,
  useAdminProveedorMercadoPagoOAuthLink,
  useAdminRevendedorMercadoPagoOAuthLink,
  useAdminUsuarios,
  useCreateAdminUsuario,
  useUpdateAdminProveedorDespachoConfig,
  useUpdateAdminUsuario,
} from "../../hooks/useAdmin"
import type { AdminUsuarioCreateRequest, AdminUsuarioResponse, AdminUsuarioTipo, AdminUsuarioUpdateRequest } from "../../types/admin"

type EditorMessage = {
  variant: "success" | "error" | "info"
  title: string
  body: string
}

const emptyForm: AdminUsuarioCreateRequest = {
  nombre: "",
  email: "",
  password: "",
  tipoUsuario: "PROVEEDOR",
  activo: true,
  nombreEntidad: "",
  dominioRevendedor: "",
  proveedorDespachante: false,
}

const tipoLabels: Record<AdminUsuarioTipo, string> = {
  ADMIN_MD: "Admin MD",
  LOGISTICA_MD: "Logistica MD",
  PROVEEDOR: "Proveedor",
  REVENDEDOR: "Revendedor",
  ASESOR_VENTA: "Asesor de venta",
  CLIENTE: "Cliente",
}

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-AR")
}

export default function AdminUsuariosPage() {
  const [form, setForm] = useState<AdminUsuarioCreateRequest>(emptyForm)
  const [selectedUsuario, setSelectedUsuario] = useState<AdminUsuarioResponse | null>(null)
  const [search, setSearch] = useState("")
  const [message, setMessage] = useState<EditorMessage | null>(null)
  const usuariosQuery = useAdminUsuarios()
  const createUsuario = useCreateAdminUsuario()
  const updateUsuario = useUpdateAdminUsuario()
  const proveedorOAuthLink = useAdminProveedorMercadoPagoOAuthLink()
  const revendedorOAuthLink = useAdminRevendedorMercadoPagoOAuthLink()
  const proveedorDespachoConfig = useAdminProveedorDespachoConfig(selectedUsuario?.proveedorId ?? null)
  const updateProveedorDespacho = useUpdateAdminProveedorDespachoConfig()
  const usuarios = usuariosQuery.data ?? []

  const usuariosFiltrados = useMemo(() => {
    const needle = search.trim().toLowerCase()
    if (!needle) return usuarios
    return usuarios.filter((item) =>
      `${item.nombre} ${item.email} ${item.tipoUsuario}`.toLowerCase().includes(needle),
    )
  }, [search, usuarios])

  const requiresEntidad = form.tipoUsuario === "PROVEEDOR" || form.tipoUsuario === "REVENDEDOR"
  const isEditing = selectedUsuario != null

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    if (!MERCADO_PAGO_DISPONIBLE) return
    const status = params.get("mp_oauth_status")
    const message = params.get("mp_message")
    const subjectType = params.get("mp_subject_type")
    if (!status || (subjectType !== "PROVEEDOR" && subjectType !== "REVENDEDOR")) {
      return
    }
    const isProveedor = subjectType === "PROVEEDOR"
    const actor = isProveedor ? "proveedor" : "revendedor"

    setMessage({
      variant: status === "success" ? "success" : "error",
      title:
        status === "success"
          ? `${isProveedor ? "Proveedor" : "Revendedor"} vinculado con Mercado Pago`
          : "No se pudo vincular Mercado Pago",
      body:
        message ??
        (status === "success"
          ? `La cuenta del ${actor} quedo vinculada correctamente.`
          : `La vinculacion OAuth del ${actor} no se pudo completar.`),
    })

    params.delete("mp_oauth_status")
    params.delete("mp_message")
    params.delete("mp_subject_type")
    params.delete("mp_subject_id")
    const next = params.toString()
    window.history.replaceState({}, "", `${window.location.pathname}${next ? `?${next}` : ""}`)
    void usuariosQuery.refetch()
  }, [usuariosQuery.refetch])

  useEffect(() => {
    if (!selectedUsuario) return
    setForm({
      nombre: selectedUsuario.nombre,
      email: selectedUsuario.email,
      password: "",
      tipoUsuario: selectedUsuario.tipoUsuario,
      activo: selectedUsuario.activo,
      nombreEntidad: "",
      dominioRevendedor: "",
      proveedorDespachante: false,
    })
  }, [selectedUsuario])

  useEffect(() => {
    if (!selectedUsuario?.proveedorId || !proveedorDespachoConfig.data) return
    setForm((current) => ({
      ...current,
      proveedorDespachante: proveedorDespachoConfig.data?.despachaMercaderia ?? false,
    }))
  }, [selectedUsuario?.proveedorId, proveedorDespachoConfig.data])

  const handleCrear = async () => {
    setMessage(null)
    try {
      const created = await createUsuario.mutateAsync(form)
      setForm((current) => ({
        ...emptyForm,
        tipoUsuario: current.tipoUsuario,
      }))
      setMessage({
        variant: "success",
        title: "Usuario creado",
        body: `Se creo ${created.email} como ${tipoLabels[created.tipoUsuario]}.`,
      })
    } catch (error) {
      const fallback = "No se pudo crear el usuario."
      const body =
        error instanceof Error && error.message
          ? error.message
          : fallback
      setMessage({
        variant: "error",
        title: "Error de alta",
        body,
      })
    }
  }

  const handleActualizar = async () => {
    if (!selectedUsuario) return
    setMessage(null)
    try {
      const payload: AdminUsuarioUpdateRequest = {
        nombre: form.nombre,
        email: form.email,
        tipoUsuario: form.tipoUsuario,
        activo: form.activo,
        nombreEntidad: form.nombreEntidad,
        dominioRevendedor: form.dominioRevendedor,
        proveedorDespachante: form.proveedorDespachante,
      }
      const updated = await updateUsuario.mutateAsync({ id: selectedUsuario.id, payload })
      if (updated.proveedorId && form.tipoUsuario === "PROVEEDOR") {
        await updateProveedorDespacho.mutateAsync({
          proveedorId: updated.proveedorId,
          despachaMercaderia: Boolean(form.proveedorDespachante),
        })
      }
      setSelectedUsuario(updated)
      setMessage({
        variant: "success",
        title: "Usuario actualizado",
        body: `Se actualizo ${updated.email}.`,
      })
    } catch (error) {
      const fallback = "No se pudo actualizar el usuario."
      const body = error instanceof Error && error.message ? error.message : fallback
      setMessage({
        variant: "error",
        title: "Error de actualizacion",
        body,
      })
    }
  }

  const handleCancelarEdicion = () => {
    setSelectedUsuario(null)
    setForm(emptyForm)
    setMessage(null)
  }

  const handleOAuthProveedor = async (proveedorId: string) => {
    setMessage(null)
    try {
      const response = await proveedorOAuthLink.mutateAsync(proveedorId)
      window.location.assign(response.authorizationUrl)
    } catch (error) {
      const fallback = "No se pudo generar el link OAuth para el proveedor."
      const body = error instanceof Error && error.message ? error.message : fallback
      setMessage({
        variant: "error",
        title: "OAuth proveedor",
        body,
      })
    }
  }

  const handleOAuthRevendedor = async (revendedorId: string) => {
    setMessage(null)
    try {
      const response = await revendedorOAuthLink.mutateAsync(revendedorId)
      window.location.assign(response.authorizationUrl)
    } catch (error) {
      const fallback = "No se pudo generar el link OAuth para el revendedor."
      const body = error instanceof Error && error.message ? error.message : fallback
      setMessage({
        variant: "error",
        title: "OAuth revendedor",
        body,
      })
    }
  }

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-blue-50 p-6 shadow-sm">
        <p className="text-xs font-semibold uppercase tracking-[0.32em] text-slate-400">Admin MD</p>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Usuarios y roles</h2>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
          Alta operativa de usuarios con credenciales reales. Para `PROVEEDOR` y `REVENDEDOR` se crea tambien la
          entidad asociada, para que puedan operar login y contexto de negocio.
        </p>
        {isEditing ? (
          <p className="mt-2 text-sm font-medium text-blue-700">Editando usuario: {selectedUsuario.email}</p>
        ) : null}
      </header>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        {message ? (
          <div className="mb-4">
            <FeedbackAlert variant={message.variant} title={message.title}>
              {message.body}
            </FeedbackAlert>
          </div>
        ) : null}

        <div className="grid gap-4 md:grid-cols-2">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Nombre</span>
            <input
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={form.nombre}
              onChange={(event) => setForm((current) => ({ ...current, nombre: event.target.value }))}
              placeholder="Nombre y apellido"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Email</span>
            <input
              type="email"
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={form.email}
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
              placeholder="usuario@dominio.com"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Password</span>
            <input
              type="password"
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={form.password}
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
              placeholder="Minimo 6 caracteres"
            />
          </label>
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Tipo de usuario</span>
            <select
              className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
              value={form.tipoUsuario}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  tipoUsuario: event.target.value as AdminUsuarioTipo,
                }))
              }
            >
              <option value="PROVEEDOR">Proveedor</option>
              <option value="REVENDEDOR">Revendedor</option>
              <option value="ADMIN_MD">Admin MD</option>
              <option value="LOGISTICA_MD">Logistica MD</option>
              <option value="ASESOR_VENTA">Asesor de venta</option>
              <option value="CLIENTE">Cliente</option>
            </select>
          </label>
          {requiresEntidad ? (
            <label className="space-y-2 md:col-span-2">
              <span className="text-sm font-medium text-slate-700">Nombre de entidad asociada</span>
              <input
                className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                value={form.nombreEntidad}
                onChange={(event) => setForm((current) => ({ ...current, nombreEntidad: event.target.value }))}
                placeholder={form.tipoUsuario === "PROVEEDOR" ? "Proveedor S.A." : "Revendedor Comercial"}
              />
            </label>
          ) : null}
          {form.tipoUsuario === "REVENDEDOR" ? (
            <label className="space-y-2 md:col-span-2">
              <span className="text-sm font-medium text-slate-700">Dominio revendedor (opcional)</span>
              <input
                className="w-full rounded-2xl border border-slate-300 px-4 py-3 text-sm outline-none transition focus:border-slate-500"
                value={form.dominioRevendedor}
                onChange={(event) => setForm((current) => ({ ...current, dominioRevendedor: event.target.value }))}
                placeholder="www.tienda.com"
              />
            </label>
          ) : null}
          {form.tipoUsuario === "PROVEEDOR" ? (
            <label className="flex items-center gap-3 rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900 md:col-span-2">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-slate-300"
                checked={Boolean(form.proveedorDespachante)}
                onChange={(event) => setForm((current) => ({ ...current, proveedorDespachante: event.target.checked }))}
              />
              Proveedor despachante (cuando venda, el envio se imputa al proveedor)
            </label>
          ) : null}
          <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700 md:col-span-2">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-slate-300"
              checked={form.activo}
              onChange={(event) => setForm((current) => ({ ...current, activo: event.target.checked }))}
            />
            Usuario activo
          </label>
        </div>

        <div className="mt-5 flex flex-wrap gap-3">
          {!isEditing ? (
            <button
              type="button"
              className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              onClick={() => void handleCrear()}
              disabled={createUsuario.isPending}
            >
              {createUsuario.isPending ? "Creando usuario..." : "Crear usuario"}
            </button>
          ) : (
            <>
              <button
                type="button"
                className="rounded-full bg-blue-700 px-5 py-3 text-sm font-semibold text-white transition hover:bg-blue-600 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => void handleActualizar()}
                disabled={updateUsuario.isPending || updateProveedorDespacho.isPending}
              >
                {updateUsuario.isPending || updateProveedorDespacho.isPending ? "Guardando..." : "Guardar cambios"}
              </button>
              <button
                type="button"
                className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-500 hover:text-slate-900"
                onClick={handleCancelarEdicion}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={`rounded-full px-5 py-3 text-sm font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-60 ${
                  form.activo ? "bg-rose-700 hover:bg-rose-600" : "bg-emerald-700 hover:bg-emerald-600"
                }`}
                onClick={() => setForm((current) => ({ ...current, activo: !current.activo }))}
              >
                {form.activo ? "Dar de baja" : "Reactivar"}
              </button>
            </>
          )}
        </div>

        <p className="mt-4 text-xs leading-5 text-slate-500">
          Nota split: para `PROVEEDOR`, este modulo ya crea su entidad operativa para que pueda cargar productos.
          El onboarding OAuth de Mercado Pago para proveedor se puede agregar con la misma metodologia usada en revendedor.
        </p>
      </section>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h3 className="text-xl font-semibold text-slate-950">Usuarios creados</h3>
          <input
            className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500 sm:max-w-sm"
            placeholder="Buscar por nombre, email o rol"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>

        {usuariosQuery.isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="h-16 animate-pulse rounded-xl border border-slate-200 bg-slate-100" />
            ))}
          </div>
        ) : null}

        {usuariosQuery.isError ? (
          <FeedbackAlert variant="error" title="No se pudo cargar usuarios">
            Reintenta la consulta para recuperar la bandeja de usuarios.
          </FeedbackAlert>
        ) : null}

        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuariosFiltrados.length === 0 ? (
          <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            No hay usuarios que coincidan con los filtros actuales.
          </div>
        ) : null}

        {!usuariosQuery.isLoading && !usuariosQuery.isError && usuariosFiltrados.length > 0 ? (
          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.14em] text-slate-500">
                <tr>
                  <th className="px-4 py-3">Usuario</th>
                  <th className="px-4 py-3">Rol</th>
                  <th className="px-4 py-3">Entidad</th>
                  <th className="px-4 py-3">Mercado Pago</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3">Alta</th>
                </tr>
              </thead>
              <tbody>
                {usuariosFiltrados.map((item) => (
                  <tr
                    key={item.id}
                    className={`cursor-pointer border-t border-slate-100 transition hover:bg-slate-50 ${
                      selectedUsuario?.id === item.id ? "bg-blue-50" : ""
                    }`}
                    onClick={() => setSelectedUsuario(item)}
                  >
                    <td className="px-4 py-3">
                      <p className="font-semibold text-slate-900">{item.nombre}</p>
                      <p className="text-slate-500">{item.email}</p>
                    </td>
                    <td className="px-4 py-3">
                      <p className="font-medium text-slate-800">{tipoLabels[item.tipoUsuario]}</p>
                      <p className="text-xs text-slate-500">{item.roles.join(", ")}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-700">
                      {item.proveedorId ? (
                        <div className="space-y-1">
                          <p>{`Proveedor: ${item.proveedorId}`}</p>
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${
                              item.proveedorDespachante ? "bg-blue-100 text-blue-800" : "bg-slate-200 text-slate-700"
                            }`}
                          >
                            Despachante: {item.proveedorDespachante ? "SI" : "NO"}
                          </span>
                        </div>
                      ) : item.revendedorId ? (
                        `Revendedor: ${item.revendedorId}`
                      ) : (
                        "-"
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {item.proveedorId || item.revendedorId ? (
                        <div className="space-y-2">
                          <span
                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.14em] ${
                              item.mercadoPago?.readyForCheckout
                                ? "bg-emerald-50 text-emerald-700"
                                : "bg-amber-50 text-amber-700"
                            }`}
                          >
                            {MERCADO_PAGO_DISPONIBLE ? item.mercadoPago?.estado ?? "PENDIENTE" : "Retirado"}
                          </span>
                          <div>
                            {MERCADO_PAGO_DISPONIBLE && (<button
                              type="button"
                              className="rounded-full border border-slate-300 px-3 py-1 text-xs font-semibold text-slate-700 transition hover:border-slate-500 hover:text-slate-900 disabled:opacity-50"
                              onClick={() => {
                                if (item.proveedorId) {
                                  void handleOAuthProveedor(item.proveedorId)
                                  return
                                }
                                if (item.revendedorId) {
                                  void handleOAuthRevendedor(item.revendedorId)
                                }
                              }}
                              disabled={proveedorOAuthLink.isPending || revendedorOAuthLink.isPending}
                            >
                              Generar OAuth
                            </button>)}
                          </div>
                        </div>
                      ) : (
                        <span className="text-slate-400">-</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.14em] ${
                          item.activo ? "bg-emerald-50 text-emerald-700" : "bg-slate-200 text-slate-600"
                        }`}
                      >
                        {item.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-600">{formatDateTime(item.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </section>
    </section>
  )
}
