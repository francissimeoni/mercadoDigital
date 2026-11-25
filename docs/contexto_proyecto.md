# Mercado Digital - Contexto del Proyecto

## 1. Qué es Mercado Digital

Mercado Digital (MD) es una plataforma tecnológica de comercio B2B2C orientada inicialmente a dropshipping privado.

El sistema conecta:

- proveedores mayoristas
- revendedores
- asesores de venta
- logística
- clientes finales

MD no vende productos directamente.  
MD presta infraestructura tecnológica y operativa para que el ecosistema funcione.

La plataforma automatiza:

- catálogo consolidado multi-proveedor
- pricing
- pedidos
- pagos vía PSP
- logística
- RMA
- notificaciones
- trazabilidad de operaciones


---

# 2. Objetivo de la plataforma

El objetivo de Mercado Digital es permitir que un revendedor pueda operar un negocio completo de venta online sin tener que:

- manejar stock propio
- coordinar proveedores manualmente
- gestionar logística compleja
- desarrollar tecnología

MD resuelve:

- infraestructura tecnológica
- consolidación de proveedores
- procesos operativos
- arbitraje de conflictos
- trazabilidad de pedidos


---

# 3. Modelo operativo

El modelo inicial es **dropshipping privado**.

Flujo:

Cliente final  
→ compra en la web del revendedor  

Revendedor  
→ vende y factura al cliente  

Proveedor  
→ abastece el producto  

Mercado Digital  
→ provee infraestructura, operación y coordinación


Reglas clave:

- el proveedor no debe quedar expuesto al cliente final
- el proveedor tampoco debe quedar expuesto innecesariamente al revendedor
- el revendedor mantiene su relación comercial con el cliente

Además, el revendedor cumple un doble rol operativo:

- vende a clientes finales desde su tienda white-label con precio comercial
- también puede comprar desde su propia operación a precio de costo

Esto implica que la plataforma debe distinguir entre:

- flujo público/comercial para cliente final
- flujo interno del revendedor a costo

Además, a nivel de dominios y superficies, el sistema también debe separar:

- portal madre de Mercado Digital
- storefront público de cada revendedor

Modelo esperado:

- `www.mercado-digital.com.ar` = plataforma madre, paneles operativos y administración
- `www.ventasnavarro.com` = tienda pública de un revendedor específico

Eso evita mezclar el acceso administrativo de MD con la experiencia pública/comercial de cada tienda.


---

# 4. Rol de Mercado Digital

Mercado Digital actúa como:

- infraestructura tecnológica
- coordinador operativo
- árbitro de conflictos
- operador logístico parcial
- administrador de incidencias

MD **no actúa como vendedor**.

El sistema debe permitir escalar la operación manteniendo control sobre:

- pagos
- logística
- disputas
- reputación de proveedores


---

# 5. Filosofía de diseño del sistema

La plataforma se diseña con estos principios:

### Backend como fuente de verdad

Toda lógica de negocio vive en el backend.

El frontend:

- no recalcula precios
- no replica reglas de negocio
- no decide estados operativos

Solo muestra información proveniente del backend.

---

### Operación real antes que sofisticación

El sistema debe ser **operable por personas reales**.

Antes de agregar sofisticación visual o automatizaciones complejas se prioriza:

- trazabilidad
- auditabilidad
- flujos claros
- control de errores

---

### Eventos auditables

Eventos críticos deben quedar registrados:

- pedidos
- pagos
- envíos
- RMA
- cambios de estado

La auditoría permite reconstruir operaciones y resolver conflictos.

---

### Evolución progresiva

El sistema está pensado para evolucionar sin reescrituras grandes.

Por ejemplo:

- pricing preparado para cambios
- PSP preparado para captura diferida
- split preparado para implementación real
- catálogo preparado para múltiples proveedores

---

# 6. Componentes principales del sistema

## Catálogo

El catálogo está basado en **producto canónico**.

Esto permite:

- agrupar ofertas equivalentes
- comparar proveedores
- consolidar visualización

El catálogo muestra:

- una sola fila por producto canónico
- la mejor oferta disponible


---

## Pedidos

Los pedidos se crean sobre producto canónico.

El sistema selecciona automáticamente la mejor oferta disponible.

Al crear un pedido se congela:

- precio
- costos
- markup
- seguro
- proveedor

Importante:

- no todos los pedidos significan lo mismo comercialmente
- un pedido público de cliente final debe congelar precio comercial
- una compra interna del revendedor debe congelar el precio costo operativo correspondiente


---

## Pagos

Todos los pagos deben ingresar por PSP.

El sistema:

- recibe confirmaciones vía webhook
- valida idempotencia
- confirma el pedido


---

## Logística

MD coordina el flujo logístico.

Flujo simplificado:

1. pedido pagado
2. proveedor prepara producto
3. logística retira
4. logística despacha
5. cliente recibe


---

## RMA

Las devoluciones se gestionan centralizadamente.

Todo producto vuelve primero al depósito de MD para inspección antes de resolverse.


---

# 7. Alcance inicial del proyecto

La versión actual del sistema busca lograr un **MVP operativo completo**.

Incluye:

- catálogo consolidado
- pricing automático
- pedidos
- stock reservado
- pagos PSP
- logística básica
- rótulos de envío
- RMA
- roles y seguridad
- notificaciones internas


---

# 8. Evoluciones futuras posibles

La arquitectura permite agregar en el futuro:

- multimoneda
- split PSP real
- marketplace público
- integraciones con transportistas
- integraciones con ERP de proveedores
- dashboards analíticos
- herramientas de marketing
- automatizaciones logísticas
- marketplaces municipales o regionales


---

# 9. Enfoque actual de desarrollo

El desarrollo sigue esta estrategia:

1️⃣ backend completamente funcional  
2️⃣ frontend operativo mínimo  
3️⃣ paneles operativos  
4️⃣ optimización y mejoras  

El objetivo es construir primero una plataforma **funcional y confiable**, antes de invertir en complejidad visual o features avanzados.
