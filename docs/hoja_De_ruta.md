# Mercado Digital - Hoja de Ruta Ejecutable

## Objetivo

Este documento define:

- dónde está el proyecto
- hacia dónde va
- cómo se organiza el trabajo en hilos
- qué módulo corresponde a cada hilo
- cuándo un módulo se considera terminado

Es la guía operativa principal del desarrollo.

---

# 1. Estado actual

## Backend
✅ COMPLETO (MVP funcional)

Incluye:
- catálogo canónico
- pricing
- pedidos
- stock
- webhook PSP
- logística
- RMA
- notificaciones
- media
- multitenant
- compra pública directa
- inicio PSP

---

## Frontend

Estado: 🟡 AVANZADO

Implementado:

- layout + auth
- catálogo real
- detalle producto
- carrito (operativo interno)
- pedidos
- notificaciones
- proveedor
- logística
- RMA
- multitenant storefront
- checkout público directo
- inicio PSP

---

## Gap actual crítico

❗ Falta cerrar:

- confirmación real de pago (webhook end-to-end)
- pantalla post-pago
- validación completa del flujo comercial

---

# 2. Filosofía de ejecución

- Un hilo = un módulo
- Un módulo = un objetivo claro
- Cada módulo debe:
  - compilar
  - ser testeable manualmente
  - dejar acta
- No avanzar si no está operativo

---

# 3. Módulos y Hilos

---

## 🔹 Módulo 1: Catálogo (YA COMPLETO)

Hilo: frontend-catalogo

Estado: ✅ FINALIZADO

Incluye:
- listado
- detalle
- media

---

## 🔹 Módulo 2: Checkout público directo

Hilo: frontend-checkout

Estado: ✅ FINALIZADO

Incluye:
- creación de pedido
- formulario mínimo
- conexión backend

---

## 🔹 Módulo 3: Inicio PSP

Hilo: backend-psp-init

Estado: ✅ FINALIZADO

Incluye:
- endpoint /public/pedido/{id}/pago
- integración sandbox

---

## 🔹 Módulo 4: PSP end-to-end

Hilo: frontend-psp

Estado: 🔴 EN CURSO

Objetivo:
- redirección a PSP
- retorno
- polling
- confirmación real vía webhook
- pantalla de resultado

Condición de finalización:
- pago confirmado automáticamente
- pedido pasa a PAGADO
- UI refleja estado correctamente

---

## 🔹 Módulo 5: Validación operativa real

Hilo: validacion-e2e

Estado: ⏳ PENDIENTE

Objetivo:
- flujo completo real probado
- sin mocks
- sin pasos manuales

---

## 🔹 Módulo 6: UX mínima operativa

Hilo: frontend-ux

Estado: ⏳ PENDIENTE

Objetivo:
- errores claros
- loading consistente
- feedback al usuario

---

## 🔹 Módulo 7: Hardening comercial

Hilo: hardening

Estado: ⏳ PENDIENTE

Objetivo:
- validaciones reales
- edge cases
- errores controlados

---

# 4. Reglas de avance

Un módulo se considera terminado SOLO si:

- funciona end-to-end
- no rompe otros módulos
- tiene acta documentada
- se puede probar manualmente
- pasa validación real

---

# 5. Reglas de hilos

Cada hilo debe:

- tener un único objetivo
- no mezclar módulos
- trabajar con prompts pequeños
- terminar con acta

---

# 6. Regla de oro

NO avanzar a otro módulo si el actual no funciona en la realidad.

---

# 7. Estado actual de ejecución

Módulo activo:

👉 PSP end-to-end

Siguiente módulo:

👉 Validación operativa real

---

# 8. Nota clave

Este documento es la fuente de verdad operativa.

Si un hilo contradice este documento:
👉 ESTE DOCUMENTO GANA