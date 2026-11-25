# Mercado Digital - Arquitectura Backend

## Objetivo

Describir cómo está organizado el backend, cuáles son sus principios y cómo debe evolucionar sin romper coherencia.

---

## Stack actual

- Java 17
- Spring Boot 3
- Maven
- MySQL 8
- Flyway
- JPA / Hibernate
- Spring Security
- JWT
- Docker Compose

---

## Principios de arquitectura

### 1. Backend como fuente de verdad
Toda lógica de negocio vive en el backend.

Incluye:
- pricing
- catálogo consolidado
- selección de oferta
- reserva de stock
- confirmación de pagos
- estados de envío
- RMA
- notificaciones

El frontend no debe recalcular ni reinterpretar esta lógica.

---

### 2. Evolución incremental
El sistema se desarrolla por módulos pequeños y compilables.

Cada bloque importante debe:
- dejar una acta
- tener tests
- respetar documentación madre
- no romper lo ya existente

---

### 3. Esquema controlado por Flyway
Flyway es la única fuente de verdad del esquema.

Reglas:
- no modificar migraciones ya aplicadas
- si algo quedó mal en entorno local temprano, se resetea localmente y se ordena bien
- no usar `ddl-auto=create/update`

---

### 4. Auditoría obligatoria
Eventos críticos deben quedar en `auditoria_eventos`.

Ejemplos:
- pedido creado
- stock reservado
- pago confirmado
- envío despachado
- RMA solicitado
- RMA resuelto

---

## Organización del código

Estructura general recomendada:

```text
src/main/java/ar/com/mercadodigital/backend
├── config
├── controller
├── service
├── repository
├── entity
├── dto
│   ├── request
│   └── response
├── mapper
├── security
├── exception
└── util