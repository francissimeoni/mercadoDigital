# Acta - Canonizacion admin v2 operativa

Fecha: 2026-04-26

## Objetivo

Cerrar la brecha entre maqueta y operacion real del modulo de canonizacion en admin, dejando:

- endpoints backend reales
- vista admin conectada de punta a punta
- filtros operativos para trabajo diario

## Alcance implementado

### Backend

Se implemento `AdminCanonizacionController` con contrato real bajo `/admin/canonizacion`:

- `GET /admin/canonizacion/pendientes`
- `GET /admin/canonizacion/candidatos?q=&limit=`
- `PUT /admin/canonizacion/productos/{productoId}/asociar/{productoCanonicoId}`
- `POST /admin/canonizacion/productos/{productoId}/crear-canonico`

Servicio principal:

- `AdminCanonizacionService`
  - lista pendientes: productos activos sin canonico
  - busca candidatos canonicos activos por `nombrePublico`, `marca`, `modelo`
  - asocia a canonico existente
  - crea canonico y asocia en una sola accion

Repositorios:

- `ProductoRepository`: `findByProductoCanonicoIsNullAndActivoTrueOrderByUpdatedAtDesc()`
- uso de stock real (`ProveedorStockRepository`) y media real (`ProductoMediaRepository`) para enriquecer la bandeja.

DTOs nuevos:

- request: `AdminCanonizacionCrearCanonicoRequest`
- response:
  - `AdminCanonizacionPendienteResponse`
  - `AdminCanonizacionCandidatoResponse`
  - `AdminCanonizacionResultadoResponse`

### Frontend admin

Se reemplazo la pantalla maqueta por una operativa en `AdminCanonizacionPage`:

- columna de pendientes reales
- panel para asociar a canonico existente
- panel para crear canonico nuevo y asociar
- mensajes de feedback de exito/error

Se agregaron filtros en la bandeja de pendientes:

- texto (`nombre`, `sku`, `proveedor`)
- proveedor
- categoria de paquete
- estado de stock (`todos`, `con stock`, `sin stock`)
- boton `Limpiar filtros`

Mejoras de navegacion:

- acceso desde menu admin (`/admin/canonizacion`)
- acceso rapido desde dashboard admin (`Aprobar canonizacion`)

## Validacion tecnica

Validaciones ejecutadas:

- backend test:
  - `AdminCanonizacionControllerTest` OK
- backend build:
  - `./mvnw -DskipTests package` OK
- frontend build:
  - `npm run build` OK

## Impacto funcional

- Se habilita el circuito real de aprobacion/canonizacion desde admin.
- No se altera el circuito de compra publica en produccion.
- No se introduce automatizacion de decisiones: la canonizacion sigue siendo accion humana.

## Estado

- Canonizacion admin v2: **operativa**
- Storefront comercial: **cerrado por ahora** (pasada final de copy/UI de produccion aplicada)

