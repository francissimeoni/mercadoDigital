# Mercado Digital - Roadmap de Producto

## Objetivo general

Construir una plataforma operativa de dropshipping privado B2B2C que permita a revendedores vender con catálogo consolidado multi-proveedor, pagos por PSP, logística coordinada por MD, y gestión centralizada de incidencias.

El roadmap prioriza:
1. operación real
2. trazabilidad
3. confiabilidad
4. escalabilidad gradual

---

## Etapa 0 - Fundaciones técnicas
### Objetivo
Dejar una base backend robusta, versionada y coherente.

### Incluye
- Spring Boot + Maven + MySQL + Flyway
- modelo de dominio inicial
- JWT + roles
- auditoría
- documentación madre del proyecto

### Estado
✅ Completado

---

## Etapa 1 - Núcleo transaccional backend
### Objetivo
Poder operar el corazón del negocio de punta a punta.

### Incluye
- catálogo por producto canónico
- comparador multi-oferta
- pricing engine
- pedido por canónico
- congelamiento de precios
- split lógico persistido
- reserva de stock con lock pesimista
- cancelación con reposición de stock
- webhook PSP idempotente
- confirmación de pago
- flujo logístico básico
- rótulos PDF
- RMA básico
- media de productos
- notificaciones internas

### Estado
✅ Completado

---

## Etapa 2 - Frontend operativo mínimo
### Objetivo
Hacer utilizable el sistema por personas reales.

### Incluye
#### Base
- layout principal
- login
- rutas protegidas
- navegación por rol

#### Revendedor
- catálogo
- ofertas por producto canónico
- creación de pedido
- pedidos propios
- notificaciones

#### Proveedor
- pedidos pendientes
- marcar listo para retiro

#### Logística MD
- retiros pendientes
- despachos pendientes
- tracking
- descarga de rótulos

#### RMA
- solicitudes
- aprobación
- recepción en depósito
- resolución

### Estado
🟡 En curso

---

## Etapa 3 - Operación comercial real
### Objetivo
Preparar el sistema para uso comercial con primeros usuarios reales.

### Incluye
- sesión real completa frontend/backend
- manejo correcto de usuario actual y rol
- endurecimiento de permisos
- manejo de errores de usuario
- estados vacíos / loading / errores refinados
- formularios operativos reales
- testing manual end-to-end

### Estado
⏳ Pendiente

---

## Etapa 4 - Pagos productivos
### Objetivo
Salir de split lógico y acercarse al esquema real.

### Incluye
- integración PSP real
- firma de webhooks
- split automático real
- conciliación
- validaciones financieras más estrictas
- rechazo/reintento controlado

### Estado
⏳ Pendiente

---

## Etapa 5 - Logística ampliada
### Objetivo
Mejorar la operación real de envíos.

### Incluye
- consultas de envío especial
- costos de envío más realistas
- reglas por zona / transportista
- QR real en rótulos
- tracking más rico
- bandejas mejoradas
- evidencia fotográfica
- reportes de operación logística

### Estado
⏳ Pendiente

---

## Etapa 6 - Reportes y control
### Objetivo
Convertir el sistema en una herramienta de gestión real.

### Incluye
- informe mensual para revendedor
- métricas de ventas
- métricas de proveedores
- métricas de logística
- métricas de seguros
- métricas de RMA
- dashboard MD
- ranking de productos y proveedores

### Estado
⏳ Pendiente

---

## Etapa 7 - Automatización comercial
### Objetivo
Aumentar ventas y capacidad de operación.

### Incluye
- asesores de venta completos en frontend
- gestión de comisiones
- reportes por asesor
- herramientas de difusión comercial
- preparación para WhatsApp / campañas
- mejoras de onboarding de revendedores

### Estado
⏳ Pendiente

---

## Etapa 8 - Importación y escalabilidad de proveedores
### Objetivo
Reducir fricción de carga de catálogo.

### Incluye
- importador Excel/CSV
- mapeo de productos
- normalización hacia producto canónico
- actualizaciones masivas
- tolerancia a datos incompletos
- herramientas de revisión

### Estado
⏳ Pendiente

---

## Etapa 9 - Mejoras estratégicas futuras
### Posibles evoluciones
- multimoneda
- outlet / reacondicionado
- fallback automático a segunda oferta
- páginas institucionales de proveedores
- marketplace municipal / regional
- módulos premium
- integraciones ERP
- dashboards avanzados

### Estado
🔮 Futuro

---

## Prioridad actual

La prioridad inmediata del producto es:

1. terminar frontend operativo mínimo
2. conectar catálogo real
3. permitir pedidos reales desde frontend
4. completar paneles de proveedor y logística
5. estabilizar operación de punta a punta

---

## Regla de roadmap

No avanzar a una etapa nueva si la anterior no está lo suficientemente usable en operación real, salvo necesidad puntual muy justificada.