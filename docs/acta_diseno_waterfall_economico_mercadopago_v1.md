# Acta - Diseno waterfall economico Mercado Pago v1

Fecha: 2026-04-06

## Objetivo

Definir el criterio funcional y economico que debe gobernar el flujo de fondos entre:

- cliente final
- revendedor
- proveedor
- Mercado Digital

La decision busca destrabar la etapa siguiente de validacion comercial real sin implementar todavia el split PSP productivo definitivo.

## Contexto de negocio que no cambia

Reglas madre ya vigentes en especificacion:

- el revendedor es el Merchant of Record
- el revendedor factura al cliente final
- el proveedor factura al revendedor
- Mercado Digital no vende productos
- Mercado Digital factura unicamente su comision al revendedor
- todo pago entra por PSP
- no se admite cuenta recaudadora propia de MD
- mientras no exista split PSP real productivo, se mantiene split logico persistido

## Estado real del repo al tomar esta decision

Hoy el backend ya persiste `SplitPedidoItem`, pero todavia no representa el waterfall completo.

Estado actual real:

- `monto_proveedor` se persiste
- `monto_revendedor` se persiste
- `monto_seguro` se persiste
- `monto_md` hoy queda en `0`
- `monto_envio` hoy queda en `0`

Conclusion:

- el proyecto ya tiene base de split logico
- todavia falta cerrar la semantica economica de MD y del envio
- no corresponde fingir que el split real ya esta cerrado

## Alternativas consideradas

### Opcion A - el revendedor cobra y luego paga al resto

Descripcion:

- el PSP cobra para el revendedor
- luego el revendedor paga a proveedor y a MD fuera del PSP

Ventajas:

- alinea facil con Merchant of Record
- simple conceptualmente

Problemas:

- deja mucha operacion critica fuera del sistema
- complica auditoria y conciliacion
- debilita control real de pagos a proveedor y de la comision MD
- aumenta riesgo comercial y operativo

### Opcion B - MD cobra como cuenta recaudadora

Descripcion:

- el PSP cobra en una cuenta central de MD
- MD reparte luego a revendedor y proveedor

Ventajas:

- control operativo muy alto

Problemas:

- contradice la regla vigente de no admitir cuenta recaudadora propia de MD
- desordena el rol comercial y fiscal del revendedor
- acerca a MD a un rol de vendedor que la especificacion rechaza

### Opcion C - cobro al revendedor con split automatico hacia proveedor y MD

Descripcion:

- el PSP cobra para la cuenta del revendedor
- en la misma operacion o conciliacion automatizada quedan definidos los montos de proveedor y MD
- mientras el PSP split real no este cerrado, el sistema conserva split logico persistido y conciliacion operativa

Ventajas:

- respeta que el revendedor sea Merchant of Record
- mantiene a MD fuera del rol de vendedor
- preserva trazabilidad
- es compatible con evolucion progresiva hacia split PSP real

Problemas:

- requiere definicion cuidadosa de fee PSP, envio, seguro, cancelaciones y RMA
- puede necesitar etapa intermedia de conciliacion antes del split productivo final

## Decision recomendada

Se adopta la **Opcion C** como waterfall economico objetivo del proyecto.

Principio rector:

- el dinero de la venta pertenece comercialmente al revendedor
- el sistema debe poder separar logicamente y luego automaticamente lo que corresponde a:
  - proveedor
  - Mercado Digital
  - envio
  - seguro
  - margen revendedor

## Waterfall funcional recomendado

Para cada `pedido_item`, el precio compuesto base del producto se interpreta en este orden:

1. costo proveedor
2. costo operativo MD
3. fee PSP embebido
4. margen comercial bruto del revendedor

Por fuera de ese precio compuesto:

5. costo de envio
6. seguro, si aplica

Luego, a nivel de reparto economico neto:

1. se reconoce el monto proveedor
2. se reconoce el costo operativo MD
3. se reconoce el costo logistico/envio segun quien ejecute la expedicion
4. se reconoce el ingreso por seguro para MD
5. el remanente comercial del markup queda para el revendedor

Nota:

- el fee PSP ya viene incorporado en el costo compuesto que recibe el revendedor
- por lo tanto termina trasladado al cliente final dentro del precio publico

## Interpretacion de cada componente

### Proveedor

- corresponde al costo proveedor congelado por item
- no depende del precio final posterior ni del fee PSP

### Revendedor

- ve un precio compuesto base del producto y no el desglose interno
- ese precio compuesto base ya incluye:
  - costo proveedor
  - costo operativo MD
  - fee PSP
- sobre ese precio compuesto aplica su markup comercial
- conserva el remanente de su markup comercial luego de costos que haya decidido absorber
- si ofrece envio gratis, ese impacto sale de su margen y no del proveedor ni del seguro
- la comision de asesor, cuando exista operativamente en pedido, sale del markup del revendedor

### Mercado Digital

MD debe medir por separado:

- costo operativo MD por infraestructura/operacion
- ingreso por seguro
- recupero logistico/envio segun politica operativa

Ese costo operativo MD cubre, en principio:

- retiro cuando corresponda
- embalaje
- despacho
- gestion operativa

MD no debe apropiarse del total de venta ni del margen comercial del revendedor por defecto.

### Envio

- se trata como linea economica separada del producto
- debe ser cobrado por MD como criterio operativo general
- si el proveedor despacha directamente desde su localidad, MD luego puede transferirle internamente el monto logistico correspondiente
- puede ser absorbido por cliente o por revendedor segun politica comercial visible
- internamente debe seguir medido aparte y no mezclarse con margen revendedor

### Seguro

- es ingreso de MD
- debe persistirse separado
- no forma parte del margen revendedor

### Fee PSP

- se incorpora al precio compuesto base que ve el revendedor
- no debe cargarse implicitamente al proveedor
- no debe erosionar un margen ya prometido al revendedor por cambios posteriores
- termina trasladado al cliente final a traves del precio publico del producto en el storefront

## Formula comercial visible acordada

Se fija como criterio funcional:

- `PC = costo proveedor + costo MD + fee PSP`
- el revendedor trabaja sobre `PC`
- el `markup` se aplica sobre `PC`
- el cliente final ve:
  - `precio producto = PC + markup`
  - `envio` por separado
  - `seguro` por separado

Por lo tanto:

- envio no forma parte del precio del producto
- seguro no forma parte del precio del producto
- ambos deben persistirse y mostrarse aparte

## Regla operativa recomendada mientras no exista split real PSP

Hasta cerrar el split automatico productivo:

- Mercado Pago cobra sobre cuenta operativa del revendedor vinculado
- el backend persiste split logico por item como fuente de verdad contable-operativa
- las obligaciones entre revendedor, proveedor y MD se concilian contra ese split logico

Esto permite:

- validar checkout real
- no bloquear OAuth por revendedor
- mantener coherencia con Merchant of Record
- evitar una cuenta recaudadora central de MD

## Impacto esperado en el modelo `SplitPedidoItem`

La semantica objetivo debe quedar asi:

- `monto_proveedor`: costo proveedor congelado
- `monto_revendedor`: margen neto revendedor esperado
- `monto_md`: costo operativo MD imputado
- `monto_envio`: monto logistico imputado y luego conciliable segun quien despache
- `monto_seguro`: ingreso por seguro de MD

Nota importante:

El fee PSP puede:

- persistirse como nuevo campo en una etapa posterior
- o reconstruirse desde los montos congelados del pedido si se decide no ampliarlo todavia

Recomendacion:

- no mezclar fee PSP dentro de `monto_md`
- mantenerlo como componente explicito cuando se pase de definicion funcional a implementacion contable mas fina
- aunque comercialmente hoy quede absorbido dentro del costo compuesto que ve el revendedor

## Cancelaciones y RMA

### Cancelacion antes de confirmacion

- no se consolida reparto
- se revierte stock
- no corresponde deuda economica consolidada entre actores

### Pago confirmado y cancelacion posterior

- debe existir criterio de reversa contra split logico
- el proveedor no deberia quedar consolidado si la operacion se cae antes del punto operativo comprometido
- la liberacion economica al proveedor se recomienda en `RETIRADO`
- debe existir override manual excepcional desde logistica MD con justificacion auditable

### RMA con seguro

- el seguro actua como ingreso separado de MD
- MD puede absorber resolucion prioritaria segun reglas del negocio

### RMA sin seguro

- no cambia retroactivamente la naturaleza original del split
- la resolucion economica depende del motivo, cobertura y estado logistico

### Criterio de arbitraje

- ante cancelacion o RMA post pago, primero MD define la responsabilidad economica entre proveedor, revendedor y MD
- recien despues se ejecuta devolucion, ajuste o compensacion
- proveedor y revendedor no deben quedar expuestos entre si

## Lo que esta decision no cierra todavia

- split automatico productivo dentro de Mercado Pago
- renovacion/retencion/impuestos reales por jurisdiccion
- payout real automatizado a proveedor
- modelado contable final del fee PSP
- tratamiento detallado de contracargos o disputas PSP
- persistencia explicita de comision de asesor por pedido

## Siguiente implementacion recomendada

Orden sugerido despues de esta acta:

1. convertir esta definicion en reglas tecnicas de `SplitPedidoItem`
2. decidir si el fee PSP va en campo propio
3. dejar `monto_md` y `monto_envio` con semantica real en backend
4. exponer reporte interno/admin de split logico para conciliacion
5. despues recien cerrar split PSP productivo sobre Mercado Pago

## Conclusion

La salida sana para Mercado Digital no es que MD cobre todo ni que el reparto quede librado totalmente fuera del sistema.

La decision recomendada es:

- cobro por cuenta del revendedor
- split logico persistido como fuente de verdad operativa
- evolucion posterior a split automatico real
- costo compuesto visible para el revendedor sin exponer desglose interno de proveedor, MD y PSP
- envio cobrado por MD con conciliacion interna posterior si el proveedor despacha directo

Esto respeta la especificacion, mantiene a MD fuera del rol de vendedor y deja una base coherente para avanzar con Mercado Pago sin deformar el modelo comercial.

---

## Aclaracion operativa (2026-05-09)

Para evitar interpretaciones ambiguas en implementacion y operacion diaria:

- `LOGISTICA_MD` es un rol operativo de estados/envio (`RETIRADO`, `DESPACHADO`, `ENTREGADO`) y no un merchant de cobro PSP.
- en el esquema actual, no corresponde OAuth Mercado Pago por `LOGISTICA_MD` para cobrar `monto_md`.
- el cobro PSP sigue atado al merchant comercial (revendedor) y la descomposicion economica se refleja en split logico.

Semantica economica consolidada por item:

- `monto_proveedor`: costo proveedor (y envio cuando `proveedor.despacha_mercaderia = true`)
- `monto_md`: costo operativo MD
- `monto_revendedor`: margen del revendedor
- `monto_envio`: trazabilidad del envio (aunque pueda imputarse a proveedor si despacha)
- `monto_seguro`: componente economico de MD
- `monto_psp`: costo PSP

Esto no impide evolucionar a payout/split real PSP a futuro, pero esa etapa requiere decision comercial/fiscal explicita separada.
