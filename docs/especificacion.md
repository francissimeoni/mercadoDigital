# Mercado Digital - Especificación Funcional y Operativa

## 1. Naturaleza del sistema

Mercado Digital (MD) es una plataforma tecnológica y operativa de marketplace B2B2C orientada inicialmente a dropshipping privado con revendedores white-label.

MD no vende productos directamente.
MD presta:

- infraestructura tecnológica
- automatización comercial
- logística
- arbitraje operativo
- gestión de incidencias
- pricing y consolidación de oferta
- notificaciones
- trazabilidad

El objetivo principal es que la plataforma permita operar un ecosistema real entre:

- proveedores
- revendedores
- asesores de venta
- clientes finales
- logística MD

---

## 2. Modelo comercial inicial

### 2.1 Dropshipping privado

El modelo inicial del sistema es dropshipping privado.

Reglas:

- el cliente final compra en la web del revendedor
- el proveedor no debe quedar expuesto al cliente final
- el proveedor no debe quedar expuesto al revendedor si no es estrictamente necesario
- el revendedor es el Merchant of Record
- MD no actúa como vendedor
- MD presta servicio tecnológico y operativo

### 2.2 Facturación

- el revendedor factura al cliente final
- el proveedor factura al revendedor
- MD factura únicamente su comisión al revendedor
- MD no factura el total de la venta

### 2.3 Dinero

- todo pago debe entrar por PSP
- si no entra por PSP, no opera
- no se permite pago manual por fuera del sistema
- no se admite cuenta recaudadora propia de MD
- el objetivo estructural es split automático
- mientras no exista split real PSP productivo, se mantiene split lógico persistido

---

## 3. Principios funcionales irrenunciables

- MD NO vende productos
- el proveedor NO debe quedar expuesto en el modelo privado
- el revendedor nunca debe perder margen por cambios de precio internos
- el frontend nunca debe recalcular lógica de negocio
- los precios se congelan al crear el pedido
- todo evento sensible debe ser auditable
- la plataforma debe ser operable por personas reales, no solo “correcta técnicamente”
- primero estructura y operación; después sofisticación visual

---

## 4. Catálogo y producto canónico

### 4.1 Producto canónico

Los productos equivalentes entre múltiples proveedores deben agruparse por `producto_canonico`.

Esto permite:

- comparar ofertas
- consolidar catálogo
- mostrar un único producto al revendedor/cliente
- desacoplar nombres o SKUs propios del proveedor

### 4.2 Ofertas

Una oferta concreta corresponde a un producto específico de un proveedor.

Cada producto canónico puede tener múltiples ofertas activas.

### 4.3 Regla de visibilidad del catálogo

El catálogo principal debe mostrar:

- una sola fila por producto canónico
- solo productos con al menos una oferta con stock > 0

No deben mostrarse productos sin stock.

### 4.4 Selección de mejor oferta

La mejor oferta se determina por este orden:

1. menor precio final
2. menor demora estimada
3. mayor stock disponible

### 4.5 Demora por defecto

Si no hay código postal o información suficiente de destino:

- backend devuelve demora estimada = 5
- esa demora representa un rango visual de 3 a 7 días

### 4.6 Moneda

- moneda actual: ARS
- no se implementa multimoneda real por ahora
- la arquitectura debe permitir agregarla en el futuro

---

## 5. Pricing

### 5.1 Fórmula base

PrecioFinal = round((CostoProveedor + MargenRevendedor + CostoEnvio + Seguro) / (1 - PSP))

### 5.2 Reglas

- redondeo entero con `Math.round()`
- el revendedor no debe perder margen
- el precio mostrado al cliente ya incluye el costo del PSP
- el frontend nunca recalcula precio
- el backend es la única fuente de verdad del precio final

### 5.3 Costo proveedor consolidado

El revendedor ve un costo consolidado.

Ese costo puede incluir internamente:

- costo proveedor
- costo logístico interno MD
- otros costos operativos definidos por MD

El cliente final no ve esa estructura interna.

### 5.4 Congelamiento de precio

Al crear un pedido, se congelan:

- precio costo
- markup aplicado
- monto seguro
- precio final por ítem

Aunque luego cambien precios internos, el pedido no se recalcula.

---

## 6. Markups y revendedor

### 6.1 Markup

El revendedor debe poder trabajar con reglas de margen / remarke sobre los productos disponibles.

Version objetivo inicial del modulo:

- el revendedor define un markup comercial sobre el costo operativo
- ese markup puede expresarse como porcentaje o monto fijo
- el sistema debe devolver precio final estimado, markup bruto, ganancia neta del revendedor y referencia clara del costo operativo
- el frontend no calcula precio por su cuenta; solo muestra simulacion y resultado entregado por backend

### 6.1.1 Comision de asesor de venta

El revendedor tambien debe poder definir una comision para asesores de venta.

Reglas:

- la comision del asesor sale del markup comercial del revendedor, no del costo proveedor ni del total bruto de la venta
- el sistema debe mostrar cuanto gana el asesor y cuanto margen neto conserva el revendedor
- la comision puede expresarse como porcentaje del markup o monto fijo
- el objetivo es que revendedor y asesor puedan conocer de forma anticipada su ganancia por producto

### 6.1.2 Alcance minimo v1

La v1 del modulo de markup/comision debe cubrir:

- configuracion global de markup del revendedor
- configuracion global de comision de asesor
- override manual por producto
- grilla operativa con:
  - costo operativo
  - markup bruto
  - comision asesor
  - ganancia neta revendedor
  - precio final estimado

No es obligatoria en v1 la gestion masiva por categoria, marca o proveedor.

### 6.2 Compras internas

El revendedor también puede usar la plataforma para compras propias a costo, sin operar como cliente final.

Regla operativa:

- el revendedor puede comprar desde su propia tienda white-label
- cuando compra como revendedor, no debe tomar el mismo precio comercial que ve el cliente final
- esa compra interna debe resolverse a costo operativo definido por MD
- compra pública a cliente final y compra interna del revendedor son flujos relacionados, pero no equivalentes
- el sistema debe distinguir claramente cuándo el actor compra para vender y cuándo compra para abastecimiento propio

### 6.3 Protección comercial

El sistema debe proteger la relación comercial del revendedor y no exponer datos sensibles de abastecimiento.

---

## 7. Stock

### 7.1 Regla de stock

El stock se reserva al crear el pedido.

### 7.2 Protección contra sobreventa

La reserva de stock se hace con lock pesimista para evitar sobreventa.

### 7.3 Cancelación

Si el pedido se cancela en estado permitido:

- el stock debe reponerse automáticamente

---

## 8. Pagos y PSP

### 8.1 Canal obligatorio

- todo pago entra por PSP
- no se permite confirmar pedidos pagados por fuera del sistema

### 8.2 Webhooks

Los webhooks del PSP deben ser idempotentes.

Regla:

- `id_evento_psp` único
- si el webhook ya fue recibido, responder 200 y no reprocesar

### 8.3 Confirmación de pago

Cuando el PSP confirma el pago:

- el pedido pasa a `PAGADO`
- el pago asociado pasa a `CONFIRMADO`
- se registra auditoría
- si no existe envío, se crea

### 8.4 Futuro

La arquitectura debe permitir captura diferida a futuro sin reescritura total.

Por ahora:
- captura inmediata / confirmación inmediata

---

## 9. Split

### 9.1 Objetivo

El diseño objetivo es split automático por ítem.

### 9.2 Estado actual

Hoy se mantiene split lógico persistido por ítem.

### 9.3 Componentes del split lógico

Por cada `pedido_item` deben persistirse, al menos:

- monto_proveedor
- monto_revendedor
- monto_md
- monto_envio
- monto_seguro

---

## 10. Envíos

### 10.1 Naturaleza del envío

El envío es una línea separada del producto.

### 10.2 Reglas

- MD define reglas de envío
- el revendedor puede ofrecer “envío gratis”, pero debe absorberlo de su margen
- MD debe seguir cobrando lo necesario para cubrir la operación real

### 10.3 Categorías de paquete

El sistema opera con categorías de paquete, por ejemplo:

- CHICO
- MEDIANO
- VOLUMINOSO
- otros equivalentes definidos por el dominio

### 10.4 Transportistas

Inicialmente se contemplan:

- Andreani
- Vía Cargo
- Credifin Express
- Correo Argentino

### 10.5 Tracking

- si existe tracking, se registra
- si no existe, puede quedar sin tracking
- no se debe bloquear la operación por ausencia de tracking si el flujo comercial lo permite

---

## 11. Flujo logístico

### 11.1 Estados de envío

- PENDIENTE_PREPARACION
- LISTO_PARA_RETIRO
- RETIRADO
- DESPACHADO
- ENTREGADO

### 11.2 Flujo

- cuando el pedido se paga, se genera el envío si no existe
- el proveedor marca `LISTO_PARA_RETIRO`
- logística MD marca:
  - RETIRADO
  - DESPACHADO
  - ENTREGADO

### 11.3 Secuencia

No se deben permitir transiciones inválidas.

### 11.4 Rótulos

El sistema debe poder generar rótulos PDF para impresión y pegado de paquetes.

---

## 12. Media de producto

### 12.1 Soporte

Cada producto puede tener:

- múltiples imágenes
- opcionalmente un video

### 12.2 Catálogo

El catálogo utiliza `imagenPrincipal`.

Si no existe `es_principal=true`, debe tomarse la primera imagen por orden.

### 12.3 Persistencia

La base debe guardar URLs, no binarios.

---

## 13. Seguro de transporte

### 13.1 Regla general

- el seguro es opcional
- se activa por PRODUCTO
- solo puede activarlo el cliente final

### 13.2 Precio

- porcentaje actual: 2% del valor del producto
- se permiten excepciones manuales puntuales por producto

### 13.3 Efecto

Con seguro:

- la resolución se gestiona de forma prioritaria
- MD puede resolver antes de que termine el reclamo con transporte, según el flujo definido

Sin seguro:

- MD gestiona igual, pero queda sujeto a los tiempos y resultados del transportista

### 13.4 Naturaleza económica

El seguro es un ingreso de MD y debe medirse por separado.

---

## 14. RMA

### 14.1 Principio central

Todo RMA vuelve al depósito de MD para inspección antes de resolverse.

### 14.2 Estados

- SOLICITADO
- APROBADO
- EN_TRANSITO_DEVOLUCION
- RECIBIDO_EN_DEPOSITO
- RESUELTO
- RECHAZADO

### 14.3 Reglas

- solo se puede solicitar RMA si el pedido fue DESPACHADO o ENTREGADO
- MD tiene la palabra final
- todas las transiciones se auditan

### 14.4 Envío de devolución

Con seguro:

- MD cubre la devolución

Sin seguro:

- el cliente paga la devolución
- si luego se confirma falla cubierta por garantía, puede reintegrarse

### 14.5 Futuro

Más adelante el sistema puede contemplar:

- reacondicionado
- outlet
- reparación
- reventa

pero eso no es objetivo actual.

---

## 15. Roles

Roles actuales del sistema:

- ADMIN_MD
- PROVEEDOR
- REVENDEDOR
- ASESOR_VENTA
- LOGISTICA_MD
- CLIENTE

### 15.1 ADMIN_MD
Opera el sistema, arbitra RMA, supervisa operación y administra la red.

### 15.2 PROVEEDOR
Carga oferta / stock, ve pedidos que debe preparar, marca listo para retiro.

### 15.3 REVENDEDOR
Vende, consulta catálogo, crea pedidos, ve sus operaciones.

### 15.4 ASESOR_VENTA
Opera dentro de la estructura de un revendedor.
No ve costos internos.
Ve sus ventas y comisiones.

### 15.5 LOGISTICA_MD
Opera retiros, despachos, tracking y flujo físico.

### 15.6 CLIENTE
Compra y puede gestionar reclamos dentro del flujo permitido.

---

## 16. Asesores de venta

### 16.1 Naturaleza

Los asesores pertenecen a un revendedor.

### 16.2 Capacidades

- pueden ser elegidos por el cliente
- pueden generar ventas
- ven sus comisiones en tiempo real
- no ven costos internos
- no manejan pagos por fuera del sistema

### 16.3 Regla de pago

Toda venta sigue entrando por PSP.  
No se admite “marcar manualmente pagado”.

---

## 17. Notificaciones

Deben existir notificaciones internas para eventos como:

- PEDIDO_CREADO
- PAGO_CONFIRMADO
- ENVIO_LISTO_PARA_RETIRO
- ENVIO_DESPACHADO
- RMA_SOLICITADO

El sistema debe ser extensible para email, panel, push o integraciones futuras.

---

## 18. Reportes e informes

### 18.1 Revendedor

MD debe poder generar informe mensual pre-automatizado para el revendedor con:

- envíos
- intervenciones
- RMA
- consultas
- trabajo operativo realizado

MD audita el informe antes de enviarlo.

### 18.2 Métricas futuras

La plataforma deberá soportar métricas sobre:

- productos más vendidos
- desempeño de proveedores
- desempeño de logística
- uso de seguros
- incidencias
- evolución comercial

---

## 19. Frontend

### 19.1 Regla principal

El frontend no debe duplicar lógica de negocio del backend.

### 19.1.1 Portal madre vs storefronts

La plataforma debe distinguir claramente entre:

- portal madre de Mercado Digital
- storefronts white-label de revendedores

Reglas:

- `www.mercado-digital.com.ar` representa la plataforma madre
- en ese dominio viven:
  - admin MD
  - accesos operativos internos
  - gestión de revendedores
  - gestión de storefronts
  - módulos proveedor, logística y operación interna
- los storefronts públicos de revendedores viven en dominios propios, por ejemplo:
  - `www.ventasnavarro.com`

Conclusiones operativas:

- un usuario admin no debe ingresar al storefront de un revendedor para administrar el sistema madre
- el storefront del revendedor es superficie pública/comercial
- el portal madre de MD es la superficie operativa y administrativa
- en entorno local, un override como `VITE_STOREFRONT_HOST` solo se usa para simular un storefront específico desde `localhost`

### 19.2 Objetivo inicial

Construir frontend operativo mínimo para:

- login
- catálogo
- pedidos
- notificaciones
- proveedor
- logística
- RMA

### 19.3 Prioridad

Primero usabilidad y operación.  
Después estética sofisticada.

---

## 20. No objetivos actuales

No forman parte del objetivo inmediato:

- marketplace municipal
- multimoneda real
- split PSP productivo definitivo si no está totalmente cerrado
- dashboards avanzados
- animaciones sofisticadas
- frontend visual premium antes del MVP operativo
