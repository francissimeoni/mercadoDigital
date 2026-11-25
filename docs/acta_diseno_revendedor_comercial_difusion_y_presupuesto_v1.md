# Acta - diseño revendedor comercial (difusion y presupuesto) v1

Fecha: 2026-04-27

## Objetivo

Formalizar el siguiente bloque funcional del panel `REVENDEDOR` para pasar de una base de pricing/pedidos a una operacion comercial diaria con difusion y cierre por checkout.

## Alcance funcional acordado

### 1) Seleccion comercial de productos (revendedor)

El revendedor debe poder trabajar sobre productos:

- canonizados
- con stock
- con precio final calculado por backend
- sin exponer proveedor ni costos sensibles

### 2) Compartir producto (redes / WhatsApp)

Cada producto comercial compartible debe incluir:

- imagen principal
- descripcion breve
- precio final visible
- link al storefront del revendedor
- CTA de compra (boton o enlace de compra)

Decision tecnica v1 (sin costo):

- no se usara API oficial de WhatsApp
- se usara boton `Compartir` del navegador/dispositivo
- fallback con enlace `https://wa.me/?text=...` cuando no haya Web Share disponible
- el contenido compartido sera texto + link (la previsualizacion de imagen la resuelve WhatsApp desde metadatos del storefront)

### 3) Consulta / presupuesto por WhatsApp

Se define una capacidad de contacto comercial previo a compra:

- mensaje prearmado
- link al producto
- precio visible
- canal abierto para consulta del cliente

Regla dura:

- aunque exista conversacion por WhatsApp, el cierre de venta debe volver a checkout con PSP
- no se admite pago manual por fuera del sistema

### 4) Presupuesto/lista para reenviar

El revendedor debe poder:

- armar una seleccion de productos
- generar un link compartible de presupuesto/lista
- reenviar ese link por WhatsApp u otro canal
- permitir que el cliente abra una vista simple, ordenada y comprable

Regla dura:

- el link de presupuesto es comercial/informativo
- la confirmacion de compra se resuelve en checkout con PSP

## Modelo de asesores de venta

### Naturaleza

- los asesores pertenecen a un revendedor
- no son proveedores ni clientes finales

### Reglas operativas

- pueden generar ventas dentro de la estructura del revendedor
- pueden ser elegidos por el cliente
- ven sus ventas y comisiones
- no ven costos internos ni proveedor
- no pueden marcar pagos manuales
- todo pago entra por PSP

### Trazabilidad comercial

Ejemplo de atribucion:

`https://tienda-revendedor.com/producto/123?asesor=juan`

Si el cliente compra:

- el pedido queda asociado al revendedor
- opcionalmente queda asociado al asesor (`juan`)
- el asesor ve su comision
- el revendedor ve la operacion completa
- MD conserva trazabilidad, operacion y auditoria

## Estado en el repo hoy

- `Precios y comisiones`: operativo v1
- `Facturas y movimientos`: conectado sobre pedidos reales (resumen comercial, no facturacion fiscal completa)
- `Red comercial`: superficie parcial, pendiente de endpoint dedicado y cierre funcional

## Fases de implementacion recomendadas

### Fase A (MVP comercial corto)

- compartir producto por WhatsApp con mensaje prearmado + link de producto, sin API paga
- tracking de `asesor` por query param para atribucion
- registrar atribucion en pedido al crear compra

### Fase B (presupuestos compartibles)

- carrito/lista comercial de cotizacion
- generacion de link publico de presupuesto
- vista publica ordenada con CTA a compra

### Fase C (red comercial completa)

- alta/edicion/estado de asesores
- resumen de ventas y comisiones por asesor
- reportes comerciales por periodo

## Fuentes funcionales alineadas

- `docs/especificacion.md` (roles, asesores, pagos por PSP, no pago manual)
- `docs/roadmap_producto.md` (etapa de automatizacion comercial y preparacion WhatsApp/campanas)
