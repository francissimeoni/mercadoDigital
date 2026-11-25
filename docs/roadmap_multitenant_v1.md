Roadmap — Multitenant v1

Estado: Plan de implementación
Relacionado con: docs/acta_arquitectura_multitenant_v1.md

1. Objetivo

Implementar soporte multitenant white-label en Mercado Digital permitiendo:

múltiples tiendas de revendedores

dominio propio por tienda

clientes propios por revendedor

storefront público

backend compartido

Todo esto sin romper la operación actual del sistema.

2. Principio de implementación

La implementación seguirá estas reglas:

cambios incrementales

backend primero

frontend público después

compatibilidad hacia atrás

acta por cada etapa

Cada etapa debe finalizar con:

docs/acta_multitenant_[etapa].md
3. Etapas del roadmap
MT1 — Modelo de dominio

Objetivo: introducir soporte de multitenant en base de datos.

Cambios

Agregar entidad:

revendedor_storefront

Campos iniciales sugeridos:

id

revendedor_id

nombre_comercial

logo_url

favicon_url

color_primario

color_secundario

hero_titulo

hero_subtitulo

hero_imagen_url

email_comercial

telefono_comercial

direccion

footer_texto

publicada

Relación:

revendedor 1 -> 0..1 revendedor_storefront

Agregar entidad:

cliente_revendedor

Campos:

id

usuario_id

revendedor_id

fecha_alta

activo

origen

Relación:

usuario 1 -> * cliente_revendedor
revendedor 1 -> * cliente_revendedor
Tareas

1 crear migración Flyway

V?__multitenant_v1.sql

2 crear entidades JPA

RevendedorStorefront
ClienteRevendedor

3 crear repositories

RevendedorStorefrontRepository
ClienteRevendedorRepository

4 crear tests básicos de persistencia

Resultado esperado

El modelo soporta:

tienda pública por revendedor

clientes asociados a tienda

Acta
docs/acta_multitenant_modelo_v1.md
MT2 — Resolución de tienda por dominio

Objetivo: permitir identificar la tienda a partir del dominio.

Endpoint nuevo
GET /public/storefront/resolve

Input:

host

Ejemplo:

juanperez.com
Respuesta
revendedorId
nombreComercial
logo
colores
hero
publicada
Tareas

1 crear controller

StorefrontController

2 crear service

StorefrontService

3 implementar consulta:

host -> revendedor -> storefront

4 agregar DTOs

StorefrontResponse
Resultado esperado

El backend puede identificar qué tienda se está cargando.

Acta
docs/acta_multitenant_storefront_resolve.md
MT3 — Catálogo público

Objetivo: exponer catálogo para clientes finales.

Endpoints nuevos
GET /public/catalogo
GET /public/catalogo/{productoCanonicoId}

Parámetros:

revendedorId
Reglas

El endpoint:

reutiliza PricingService

reutiliza lógica de CatalogoService

no expone proveedor

no expone costo

no expone split

Tareas

1 crear controller público

PublicCatalogoController

2 reutilizar servicios existentes

3 DTOs públicos de catálogo

Resultado esperado

El catálogo puede ser consultado sin autenticación.

Acta
docs/acta_multitenant_catalogo_publico.md
MT4 — Seguridad de endpoints

Objetivo: asegurar que solo endpoints correctos queden públicos.

Cambios

Revisar SecurityConfig.

Separar:

Público
/auth/login
/public/**
Autenticado
/notificaciones
Revendedor
/catalogo
/pedido
/revendedores
Proveedor
/proveedores
Logística
/logistica
/envios
Admin
/rma
Resultado esperado

No quedan endpoints abiertos accidentalmente.

Acta
docs/acta_multitenant_seguridad_endpoints.md
MT5 — Administración de tiendas

Objetivo: permitir que admin gestione revendedores/tiendas.

Endpoints
GET /admin/revendedores
POST /admin/revendedores
PUT /admin/revendedores/{id}
PUT /admin/revendedores/{id}/storefront
Funcionalidades

listar revendedores

crear revendedor

activar/desactivar

configurar storefront

configurar dominio

Resultado esperado

Admin puede gestionar tiendas.

Acta
docs/acta_multitenant_admin_tiendas.md
MT6 — Frontend: contexto de tenant

Objetivo: que el frontend sepa qué tienda está cargando.

Nueva capa
src/tenant

Archivos:

tenantApi.ts
TenantContext.tsx
useTenant.ts
tenantTypes.ts
Responsabilidad

resolver storefront por dominio

exponer branding

exponer revendedorId

manejar loading

Resultado esperado

Frontend conoce la tienda actual.

Acta
frontend/docs/acta_multitenant_tenant_context.md
MT7 — Storefront público

Objetivo: crear la tienda white-label.

Layout nuevo
PublicStoreLayout
Rutas
/
catalogo
producto/:id
carrito
Componentes

Home

ProductGrid

ProductCard

ProductDetail

Cart

Resultado esperado

Cada revendedor tiene tienda pública.

Acta
frontend/docs/acta_storefront_publico_v1.md
MT8 — Clientes por tienda

Objetivo: aislar clientes por revendedor.

Cambios backend

Cuando un cliente compra:

verificar relación cliente_revendedor

crear si no existe

Resultado esperado

Cada tienda tiene sus clientes.

Acta
docs/acta_multitenant_clientes_revendedor.md
MT9 — Pruebas end-to-end

Objetivo: validar flujo completo.

Escenario
cliente -> tienda -> catálogo -> pedido -> pago -> despacho
Verificaciones

catálogo correcto por tienda

precios correctos

pedido ligado al revendedor

proveedor no expuesto

Acta
docs/acta_multitenant_e2e_v1.md
4. Reglas para prompts de Codex

Todos los prompts deben incluir lectura obligatoria de:

docs/contexto_proyecto.md
docs/especificacion.md
docs/convenciones.md
docs/contexto_avance.md
docs/acta_arquitectura_multitenant_v1.md
docs/roadmap_multitenant_v1.md

Frontend además:

frontend/docs/convenciones_frontend.md
frontend/docs/arquitectura_frontend.md
5. Restricciones

No se debe:

duplicar lógica del backend

recalcular precios en frontend

exponer proveedor

exponer costo

exponer split

romper endpoints existentes

6. Resultado final esperado

Al finalizar multitenant v1:

Mercado Digital soportará:

múltiples tiendas white-label

dominios propios

clientes por tienda

catálogo público por revendedor

backend centralizado

Manteniendo el principio fundamental:

Mercado Digital no vende productos; provee infraestructura tecnológica.