# Acta - tracking publico experiencia cliente v1

Fecha: 2026-04-07

## Objetivo

Volver la pantalla publica de seguimiento mas elegante, minimalista y didactica para cliente final.

## Problema anterior

La vista mostraba demasiada logica operativa en formato de ficha tecnica:

- estados crudos
- bloques economicos y tecnicos con demasiado peso visual
- poca guia narrativa sobre "donde esta mi pedido"

## Criterio aplicado

Se redefinio el tracking publico para que funcione como superficie principal de comunicacion con el cliente:

- foco en estado actual claro
- recorrido simple por etapas
- lenguaje mas humano y menos interno
- mismo enlace util tambien para postventa cuando el pedido ya fue entregado

## Cambios principales

- nueva cabecera con estado actual resumido
- timeline/recorrido simple del pedido:
  - pedido recibido
  - pago confirmado
  - preparando envio
  - en camino
  - entregado
- bloque de resumen con "estado actual" y "proximo paso"
- detalle economico mas liviano
- bloque final de ayuda/postventa
- si el pedido ya fue entregado, el mismo tracking se reutiliza como punto de contacto para garantia o soporte

## Decision funcional

Para cliente final, el tracking pasa a ser la superficie principal de verdad.

Eso habilita una estrategia de mails mas simple:

- mail inicial con link de seguimiento
- mail en hitos grandes si hace falta
- menos dependencia de enviar un mail por cada microestado interno

## Archivo tocado

- `frontend/src/storefront/pages/StorefrontPedidoTrackingPage.tsx`

## Verificacion tecnica

- `npm run build` en frontend: OK
