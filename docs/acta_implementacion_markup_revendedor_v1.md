# Acta - Implementacion markup revendedor v1

Fecha: 2026-04-02

## Alcance implementado

Se implemento la base operativa de la v1 del modulo comercial del revendedor:

- configuracion global de markup
- configuracion global de comision de asesor
- override puntual por producto
- preview por producto con resultado economico estimado
- pantalla inicial dentro del dashboard del revendedor

## Backend

Quedo incorporado:

- configuracion default de markup/comision en `Revendedor`
- override por producto en `RevendedorMarkup`
- migracion de base para nuevos campos
- servicio `RevendedorPricingV1Service`
- controller `RevendedorPricingController`
- integracion del resolvedor de pricing con ofertas canonicas y pedido comercial

Endpoints nuevos:

- `GET /revendedores/{revendedorId}/pricing-v1/config`
- `PUT /revendedores/{revendedorId}/pricing-v1/config`
- `GET /revendedores/{revendedorId}/pricing-v1/preview`
- `PUT /revendedores/{revendedorId}/pricing-v1/productos/{productoId}`
- `DELETE /revendedores/{revendedorId}/pricing-v1/productos/{productoId}`

## Frontend

Quedo incorporado:

- ruta `/pricing` para rol `REVENDEDOR`
- acceso desde el menu lateral del dashboard
- formulario para regla global
- preview de productos con:
  - costo operativo
  - markup bruto
  - comision asesor
  - ganancia neta revendedor
  - precio final estimado
- editor puntual de override por producto

## Validacion tecnica

- `backend`: `./mvnw.cmd test` OK
- `frontend`: `npm run build` OK

## Estado resultante

La v1 ya no esta solo definida en docs: quedo implementada en backend y frontend.

Todavia no la marco como modulo totalmente cerrado porque falta:

- validacion funcional manual mas profunda
- casos demo con diferencias de markup mas visibles
- definir si la operacion real va a necesitar reglas masivas o si alcanza la v1 actual
