# Acta - base local multi-storefront tenant v1

Fecha: 2026-04-07
Estado: implementado y validado en suite

## Objetivo

Dejar una base local concreta para validar multi-storefront/tenant sin romper el E2E ya validado.

## Cambios realizados

1. se agrego un segundo revendedor demo local con storefront publicada:
   - `Vidriera Urbana`
   - dominio: `www.vidrieraurbana.com`
   - usuario demo: `revendedor.tenant2@md.local`
2. se mantuvo `Navarro Ventas` como storefront original:
   - dominio: `www.navarroventas.com`
3. se agrego cobertura backend para validar que:
   - si un cliente ya existe por email
   - y compra en otro storefront
   - se crea la relacion `ClienteRevendedor` para el tenant correcto
4. se ajusto frontend en retorno post-pago para no reutilizar el ultimo checkout persistido de cualquier tienda:
   - ahora busca el checkout persistido solo del `revendedorId` activo

## Validacion ejecutada

- `backend`: `.\mvnw.cmd test`
- `frontend`: `npm run build`

Ambos quedaron en verde.

## Conclusion

Queda lista una base local razonable para probar:

- resolucion de segundo storefront
- alta de cliente en tenant correcto
- continuidad de pedido y mail por revendedor correcto

## Riesgo remanente visible

El tracking publico backend sigue resolviendo solo por `trackingToken`.

Eso implica que:

- el link generado por mail apunta al dominio correcto del revendedor
- pero si alguien abre manualmente un token valido bajo otro host, hoy no hay una verificacion explicita de mismatch tenant-host en el endpoint publico

## Siguiente paso recomendado

1. probar manualmente ambos storefronts en local con compras separadas
2. revisar si conviene endurecer tracking publico con identidad de tenant en contrato o validacion de host
3. luego bajar a matriz efectiva de notificaciones y mails por evento
