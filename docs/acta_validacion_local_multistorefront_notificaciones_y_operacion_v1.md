# Acta - validacion local multistorefront, notificaciones y operacion v1

Fecha: 2026-04-07

## Objetivo

Cerrar una validacion manual real del frente multi-storefront/tenant y del circuito operativo asociado, incluyendo mails reales, tracking endurecido y notificaciones internas alineadas al negocio.

## Validaciones realizadas

- alta y uso real de un segundo storefront
- login real del revendedor del storefront
- compra publica real en storefront correcto
- alta/vinculacion del cliente en el tenant correcto
- pedido visible solo en el storefront correcto
- proveedor correcto ve el pedido y el proveedor incorrecto no
- SMTP real por storefront validado con Gmail
- mail transaccional recibido correctamente
- tracking publico validado con dominio correcto
- hardening de tracking por `storefrontHost` para impedir cruce entre tenants
- operacion de punta a punta con distintos usuarios:
  - revendedor
  - proveedor
  - logistica

## Hallazgo importante resuelto

Se detecto que un `trackingToken` podia abrirse desde otro storefront local si se forzaba la URL bajo otro host.  
Se corrigio en backend para exigir coincidencia entre:

- `trackingToken`
- `storefrontHost`
- `revendedor.dominio` del pedido

Si el host no coincide, el backend responde `404`.

## Notificaciones internas

Se reajusto el criterio de negocio para no exponer proveedor y revendedor entre si.

Matriz interna resultante:

- `PEDIDO_CREADO`
  - proveedor
  - revendedor
- `PAGO_CONFIRMADO`
  - revendedor
- `ENVIO_LISTO_PARA_RETIRO`
  - revendedor
  - logistica
- `ENVIO_RETIRADO`
  - revendedor
- `ENVIO_DESPACHADO`
  - revendedor
- `ENVIO_ENTREGADO`
  - revendedor

Cada bloque deja auditoria explicita para admin.

## Estado tecnico al cierre

- backend: `./mvnw.cmd test` OK
- frontend: `npm run build` OK

## Pendientes siguientes recomendados

1. corregir bug de checkout persistido
2. corregir layout roto/desfasado de logistica
3. decidir si se implementan ya los mails posteriores a `pago confirmado` para cliente final
