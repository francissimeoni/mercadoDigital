# Acta - Hardening Auth y Roles Frontend

Fecha: 2026-03-10

## Objetivo
Endurecer autenticacion, manejo de sesion y navegacion por rol en el frontend de Mercado Digital sin romper el login real existente.

## Alcance
- Sesion JWT en frontend
- Redireccion automatica por expiracion
- Control de acceso por rol en rutas protegidas
- Navegacion visible segun rol
- Redireccion post-login a home valida del rol

## Cambios implementados

### Mapa unico de acceso por rol
Se creo `src/auth/accessControl.ts` para concentrar:
- ruta principal por rol
- rutas permitidas por rol
- links visibles por rol

Roles cubiertos:
- `REVENDEDOR`
- `PROVEEDOR`
- `LOGISTICA_MD`
- `ADMIN_MD`

### Endurecimiento de sesion
Se ajusto `src/auth/authSession.ts` para:
- validar estructura persistida antes de reutilizarla
- seguir parseando JWT para `uid`, `sub`, `roles` y `exp`
- limpiar sesion al detectar token expirado
- emitir evento de sesion para sincronizar UI

Se ajusto `src/auth/AuthContext.tsx` para:
- inicializar solo sesiones validas
- programar expiracion automatica segun `exp`
- sincronizar cambios de sesion en la pestaña actual
- redirigir a `/login` cuando la sesion vence

### Manejo de 401
Se ajusto `src/api/axiosClient.ts` para:
- limpiar sesion ante `401`
- redirigir a `/login`
- no interferir con `POST /auth/login`

### ProtectedRoute y router
Se ajusto `src/routes/ProtectedRoute.tsx` para:
- validar autenticacion real
- validar acceso a la ruta actual segun rol
- redirigir a la home valida del rol si intenta entrar a una ruta no permitida

Se agrego `src/routes/RoleHomeRedirect.tsx` para resolver la landing correcta por rol.

### Login y layout
Se ajusto `src/pages/login/LoginPage.tsx` para:
- redirigir post-login a la home correcta segun rol
- redirigir usuarios ya autenticados a su home valida

Se ajusto `src/components/layout/MainLayout.tsx` para:
- mostrar solo links permitidos por el rol actual reutilizando el mapa central

## Rutas principales definidas
- `REVENDEDOR` -> `/catalogo`
- `PROVEEDOR` -> `/proveedor/pedidos`
- `LOGISTICA_MD` -> `/logistica/envios`
- `ADMIN_MD` -> `/rma`

## Restricciones cumplidas
- Sin librerias nuevas
- Sin romper login real
- Sin inventar permisos desde frontend
- Cambios chicos y compilables
- Sin duplicar la logica de acceso

## Validacion
- Build frontend ejecutado con `npm.cmd run build`
