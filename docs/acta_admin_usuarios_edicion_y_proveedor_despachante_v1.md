# Acta - Admin usuarios editable y proveedor despachante v1

Fecha: 2026-05-07

## Objetivo

Adaptar la pantalla `Admin > Usuarios y roles` para que:

- permita seleccionar usuarios creados desde la nomina,
- habilite edicion de datos y alta/baja logica (`activo`),
- integre el flag funcional `proveedor despachante`.

## Cambios backend

- Nuevo endpoint:
  - `PUT /admin/usuarios/{id}`
- Nuevo request:
  - `AdminUsuarioUpdateRequest`
- Logica de `AdminUsuarioService.actualizar(...)`:
  - actualiza nombre/email/tipo/activo/roles,
  - upsert de entidad `Proveedor` o `Revendedor` asociada cuando corresponde,
  - permite persistir `proveedorDespachante` para proveedores.

## Cambios frontend

- `AdminUsuariosPage` ahora:
  - hace seleccion por fila en "Usuarios creados",
  - carga el usuario seleccionado en modo edicion,
  - permite guardar cambios y cancelar edicion,
  - permite "Dar de baja / Reactivar" via campo `activo`,
  - muestra checkbox `Proveedor despachante` para tipo proveedor.
- Integracion API:
  - `updateAdminUsuario(...)`
  - `getAdminProveedorDespachoConfig(...)`
  - `updateAdminProveedorDespachoConfig(...)`

## Validacion

- Backend:
  - `./mvnw -q -DskipTests compile`
  - `./mvnw -q "-Dtest=AdminUsuarioControllerTest,AdminProveedorControllerTest,PedidoServiceCanonicoTest" test`
- Frontend:
  - `npm run build`

## Resultado funcional

La vista de admin usuarios ya queda coherente con el flujo operativo:

- alta de proveedor con rol operativo,
- ajuste posterior de si despacha o no,
- mantenimiento de usuarios existentes sin salir de la pantalla.
