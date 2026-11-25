# Acta - Modulo markup revendedor v1

Fecha: 2026-04-02

## Alcance

Se define la v1 del modulo comercial de revendedor para gestionar:

- markup comercial propio
- comision para asesores de venta
- preview del precio final y del reparto de margen

## Objetivo de negocio

La v1 debe permitir que el revendedor pueda:

- decidir cuanto gana por producto
- decidir cuanto cede a un asesor de venta
- ver precio final estimado antes de publicar o vender
- operar sin tener que remarcar todo el catalogo uno por uno

## Decisiones funcionales

- el backend sigue siendo la unica fuente de verdad del pricing
- el markup del revendedor puede ser porcentaje o monto fijo
- la comision del asesor sale del markup del revendedor, no del costo proveedor ni del total bruto
- el sistema debe devolver:
  - costo operativo
  - markup bruto
  - comision asesor
  - ganancia neta revendedor
  - precio final estimado

## Alcance minimo v1

- configuracion global de markup del revendedor
- configuracion global de comision de asesor
- override manual por producto
- grilla operativa en dashboard del revendedor

## Alcance explicitamente fuera de v1

- reglas masivas por categoria
- reglas masivas por marca
- reglas masivas por proveedor
- edicion bulk avanzada

## Orden recomendado de implementacion

1. backend de reglas globales y override por producto
2. calculo backend de precio final, ganancia revendedor y ganancia asesor
3. endpoints para grilla operativa
4. pantalla basica en dashboard del revendedor

## Motivo de alcance

La v1 ya resuelve el corazon comercial del modulo y alcanza para validar el modelo de margen del revendedor sin sobredisenar reglas masivas desde el inicio.
