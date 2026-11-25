Acta — Arquitectura Multitenant v1

Fecha: 2026
Estado: Acta de evolución arquitectónica
Alcance: Backend + Frontend + Modelo de dominio

1. Contexto

Mercado Digital está evolucionando hacia un modelo B2B2C white-label, donde múltiples revendedores operan sus propias tiendas públicas mientras comparten una infraestructura tecnológica común.

Hasta el momento, el sistema ya modela correctamente:

usuarios y autenticación

roles operativos

revendedores

proveedores

catálogo consolidado por producto canónico

pedidos

pagos PSP

logística

RMA

El flujo operativo actual es:

revendedor consulta catálogo

crea pedido

backend selecciona oferta

stock se reserva

PSP confirma pago

logística despacha

Este flujo ya funciona sobre un backend centralizado.

Sin embargo, el sistema aún no modela formalmente el concepto de tienda white-label pública ni el aislamiento comercial de clientes por revendedor.

Esta acta define la evolución necesaria para soportar ese modelo.

2. Objetivo de esta evolución

Introducir multitenancy comercial basado en revendedores, permitiendo:

múltiples tiendas white-label

dominios propios por revendedor

clientes propios por tienda

catálogo público por tienda

pedidos asociados al revendedor correcto

backend central compartido

Todo esto sin romper el modelo operativo actual.

3. Principio central

Se adopta la siguiente decisión arquitectónica:

Revendedor será el tenant comercial base del sistema.

Esto implica que:

cada revendedor puede tener una tienda pública

cada tienda opera con branding propio

cada tienda tiene clientes propios

todas comparten el mismo backend

4. Nuevas entidades del dominio

Se agregan dos conceptos principales.

4.1 RevendedorStorefront

Representa la presencia pública white-label del revendedor.

Separar esta entidad evita contaminar la entidad operativa Revendedor con demasiados campos visuales o de marketing.

Responsabilidad

Contener configuración pública de la tienda.

Campos esperados

Ejemplo conceptual:

RevendedorStorefront
- id
- revendedorId
- nombreComercial
- logoUrl
- faviconUrl
- colorPrimario
- colorSecundario
- heroTitulo
- heroSubtitulo
- heroImagenUrl
- emailComercial
- telefonoComercial
- direccionComercial
- footerTexto
- publicada
Relación
Revendedor 1 ---- 0..1 RevendedorStorefront

Un revendedor puede tener una tienda pública.

4.2 ClienteRevendedor

Introduce la relación explícita entre:

Usuario ↔ Revendedor

Esto permite modelar clientes por tienda.

Problema actual

Hoy:

Pedido -> Usuario

pero no existe una relación explícita cliente-revendedor.

Esto dificulta:

historial comercial por tienda

marketing

soporte

análisis comercial

login storefront por tienda

Nueva entidad
ClienteRevendedor
- id
- usuarioId
- revendedorId
- fechaAlta
- activo
- origenAlta
Relación
Usuario 1 ---- * ClienteRevendedor
Revendedor 1 ---- * ClienteRevendedor

Esto permite que un usuario sea cliente de múltiples tiendas.

5. Arquitectura del sistema

El sistema queda dividido conceptualmente en tres superficies.

5.1 Sitio institucional Mercado Digital

Dominio ejemplo:

mercadodigital.com

Objetivo:

explicar la plataforma

captar revendedores

captar proveedores

explicar el modelo

permitir registrarse

No vende productos.

5.2 Tiendas white-label de revendedores

Ejemplos:

juanperez.com
megaelectronica.com.ar
mitienda.com

Estas tiendas:

muestran catálogo

venden al cliente final

usan el backend central

no exponen proveedores

5.3 App operativa autenticada

Usada por:

revendedores

proveedores

logística

admin MD

Incluye:

gestión de pedidos

logística

RMA

bandejas operativas

El frontend actual ya cubre gran parte de esta superficie.

6. Resolución de tienda por dominio

Las tiendas white-label se identificarán por dominio.

Flujo conceptual:

request -> dominio -> revendedor -> storefront -> catálogo

El backend debe poder resolver:

host -> revendedor

Ejemplo conceptual de endpoint:

GET /public/storefront/resolve

Respuesta:

revendedorId
nombreComercial
logo
colores
hero
estadoPublicacion

Esto permitirá cargar la tienda correcta.

7. Catálogo público por revendedor

El catálogo público se resolverá usando el revendedor.

El backend ya soporta:

GET /catalogo?revendedorId={uuid}

La superficie pública deberá reutilizar esa lógica sin duplicar negocio.

El frontend público:

no recalcula precios

no decide proveedor

no calcula markup

Todo eso sigue en backend.

8. Seguridad

La superficie pública deberá cumplir:

no exponer proveedor

no exponer costo

no exponer split

no exponer lógica interna de pricing

El backend sigue siendo la fuente de verdad.

9. Frontend

El frontend se dividirá en dos áreas.

9.1 Storefront público

Nueva superficie:

PublicStoreLayout

Contendrá:

/
catalogo
producto/:id
carrito
login cliente

Este frontend:

resuelve tenant por dominio

carga branding

consulta catálogo público

crea pedidos

9.2 App operativa

La app actual se mantiene.

Mantiene:

MainLayout
ProtectedRoute
AuthContext
roles

Usada por:

revendedores

proveedores

logística

admin

10. Administración de tiendas

Se agregará un módulo de administración para ADMIN_MD.

Permitirá:

listar revendedores

crear revendedor

editar revendedor

activar/desactivar

configurar dominio

configurar storefront

consultar clientes de la tienda

11. Compatibilidad con el sistema actual

Esta evolución no rompe el modelo actual.

Se mantienen intactos:

Usuario

Revendedor

ProductoCanonico

Producto

Pedido

PedidoItem

SplitPedidoItem

Pago

Envio

RMA

Las nuevas entidades agregan capacidades sin modificar flujos existentes.

12. Roadmap de implementación

La implementación se realizará en etapas.

Ver:

docs/roadmap_multitenant_v1.md

Etapas principales:

modelo de datos

endpoints públicos storefront

seguridad de endpoints

admin de tiendas

frontend storefront

clientes por tienda

pruebas end-to-end

13. Restricciones de implementación

Todo desarrollo relacionado con multitenant debe respetar:

docs/contexto_proyecto.md

docs/especificacion.md

docs/convenciones.md

docs/contexto_avance.md

Además:

no duplicar lógica del backend

no recalcular precios en frontend

no exponer proveedor

mantener contratos consistentes

cambios pequeños y reversibles

14. Resultado esperado

Al finalizar multitenant v1, el sistema permitirá:

múltiples tiendas white-label

dominios propios por revendedor

clientes propios por tienda

catálogo público por tienda

pedidos asociados al revendedor correcto

backend central compartido

Todo manteniendo la filosofía original de Mercado Digital:

MD no vende productos; provee infraestructura tecnológica.

15. Estado

Acta aprobada como dirección arquitectónica para multitenant v1.

La implementación se realizará mediante actas incrementales por módulo.