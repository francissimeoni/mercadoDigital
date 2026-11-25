# Acta - Seed Local de Usuarios Demo y Datos Operativos

Fecha: 2026-03-09

## Objetivo
Disponer datos demo locales consistentes para validar manualmente flujos por rol en backend/frontend.

## Enfoque aplicado
- Seed implementado via bootstrap en backend:
  - `LocalDemoDataSeeder`
- Activacion controlada:
  - solo perfil `local`
  - solo si `app.seed-demo.enabled=true`
- Produccion protegida:
  - en `application.yml` queda `app.seed-demo.enabled=false`
  - fuera de `local` el seed no corre

## Archivos agregados/modificados
- `backend/src/main/java/ar/com/mercadodigital/backend/bootstrap/LocalDemoDataSeeder.java`
- `backend/src/main/resources/application-local.yml`
- `backend/src/main/resources/application.yml` (agrega `app.seed-demo.enabled=false`)

## Usuarios demo creados
Todos tienen `activo=true` y password en hash BCrypt.

1. REVENDEDOR
- Email: `revendedor.demo@md.local`
- Password: `Revendedor123!`
- Tipo usuario: `REVENDEDOR`
- Rol: `REVENDEDOR`

2. PROVEEDOR
- Email: `proveedor.demo@md.local`
- Password: `Proveedor123!`
- Tipo usuario: `PROVEEDOR`
- Rol: `PROVEEDOR`

3. LOGISTICA_MD
- Email: `logistica.demo@md.local`
- Password: `Logistica123!`
- Tipo usuario: `ADMIN_MD` (con rol operativo logistica)
- Rol: `LOGISTICA_MD`

4. ADMIN_MD
- Email: `admin.demo@md.local`
- Password: `Admin123!`
- Tipo usuario: `ADMIN_MD`
- Rol: `ADMIN_MD`

5. CLIENTE (opcional)
- Email: `cliente.demo@md.local`
- Password: `Cliente123!`
- Tipo usuario: `CLIENTE`
- Rol: `CLIENTE`

## Datos demo operativos creados
- Roles base: `REVENDEDOR`, `PROVEEDOR`, `LOGISTICA_MD`, `ADMIN_MD`, `CLIENTE`
- Revendedor demo
- Proveedor demo
- Producto canónico + producto proveedor asociado
- Stock proveedor > 0
- Markup revendedor activo
- Media principal del producto
- Transportista + tarifa base
- 3 pedidos demo con items, pago y envío:
  - pedido A: `PENDIENTE_PREPARACION` (proveedor puede marcar listo para retiro)
  - pedido B: `LISTO_PARA_RETIRO` (logística puede marcar retirado)
  - pedido C: `RETIRADO` (logística puede despachar)
- Notificaciones demo asociadas

## IDs útiles para pruebas frontend
- revendedorId: `33333333-3333-3333-3333-333333333301`
- proveedorId: `33333333-3333-3333-3333-333333333302`
- usuarioId revendedor: `22222222-2222-2222-2222-222222222201`
- usuarioId proveedor: `22222222-2222-2222-2222-222222222202`
- usuarioId logística: `22222222-2222-2222-2222-222222222203`
- usuarioId cliente: `22222222-2222-2222-2222-222222222205`
- transportistaId demo: `99999999-9999-9999-9999-999999999901`

## Activación local
Ejecutar backend con perfil local:

`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

o equivalente con `SPRING_PROFILES_ACTIVE=local`.

## Validación
- Ejecutado: `./mvnw -q test`
- Resultado: OK
