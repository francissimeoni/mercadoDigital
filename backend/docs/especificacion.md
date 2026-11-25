# Mercado Digital - Especificación (Decisiones cerradas)

## Principios
- Mercado Digital (MD) NO vende productos. Solo presta servicio tecnológico.
- El revendedor es el Merchant of Record (vende y factura al cliente final).
- El proveedor factura al revendedor.
- MD factura solo su comisión al revendedor.
- MD es árbitro final de disputas/RMA.

## Pagos
- Canal de cobro: PSP obligatorio (si no entra por PSP, no opera).
- No custodia: el dinero NO entra completo a cuenta de MD.
- Se implementará split por ítem/producto (carrito multi-proveedor).
- Captura inmediata (por ahora). (La arquitectura debe permitir captura diferida a futuro sin reescritura total.)
- El precio final al cliente incluye costo PSP:
  PrecioFinal = (CostoProveedor + MargenRevendedor + ComisionMD + CostoEnvio) / (1 - %PSP)

## Envíos
- Envío es línea separada del producto.
- MD define reglas de envío (revendedor puede ofrecer “envío gratis” pero lo absorbe de su margen; MD siempre cobra logística + envío si aplica).
- Se manejarán categorías de paquete (chico/mediano/voluminoso) con tarifas base por transportista.
- Transportistas: Andreani, Vía Cargo, Credifin Express, Correo Argentino.
- Tracking: si existe se registra; si no, se permite “sin tracking”.

## Seguro de Transporte
- Seguro opcional por PRODUCTO, activado solo por el cliente final.
- Precio: 2% del valor del producto (con excepciones manuales por producto).
- Con seguro: resolución prioritaria tras recibir e inspeccionar el producto devuelto.
- Sin seguro: MD gestiona reclamo igualmente pero queda atado a los tiempos del transportista.

## RMA
- Todo RMA vuelve al depósito de MD para inspección antes de resolver.
- Envío devolución:
  - Con seguro: MD cubre.
  - Sin seguro: cliente paga; si se confirma falla cubierta por garantía, se reintegra.
- Decisión final siempre MD.

## Roles
- ADMIN_MD (panel MD)
- PROVEEDOR
- REVENDEDOR
- ASESOR_VENTA
- CLIENTE

## Asesores
- Revendedor puede crear asesores.
- Asesor tiene foto y nombre.
- Cliente puede elegir asesor (no obligatorio).
- Asesor ve ventas/comisiones en tiempo real y por rango de fechas.
- Ventas manuales: solo vía PSP (nunca pagos por fuera).

## Reportes
- Informe mensual pre-automatizado de intervenciones: envíos + RMA + consultas.
- MD audita y “pulir” antes de enviar al revendedor.
