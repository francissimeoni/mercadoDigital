# Acta - Ajuste Permiso Proveedor Listo Para Retiro

Fecha: 2026-03-09

## Objetivo
Alinear seguridad backend con la especificacion funcional para que PROVEEDOR pueda marcar un pedido como `LISTO_PARA_RETIRO`.

## Problema detectado
El endpoint `POST /envios/{pedidoId}/listo-para-retiro` quedaba dentro de la regla general `/envios/**` restringida a `LOGISTICA_MD`, lo que bloqueaba el flujo de proveedor.

## Cambio realizado
- Archivo: `backend/src/main/java/ar/com/mercadodigital/backend/config/SecurityConfig.java`
- Se agrego una regla especifica y prioritaria:
  - `POST /envios/*/listo-para-retiro` -> `PROVEEDOR`
- Se mantuvo la regla existente:
  - `/logistica/**` y resto de `/envios/**` -> `LOGISTICA_MD`

Con esto:
- PROVEEDOR puede ejecutar la transicion `LISTO_PARA_RETIRO`.
- LOGISTICA_MD mantiene permisos sobre el resto del flujo logistico.
- No se abrieron permisos extra para otros roles.

## Cobertura de seguridad agregada
- Nuevo test:
  - `backend/src/test/java/ar/com/mercadodigital/backend/controller/SecurityProveedorEnvioAccessTest.java`
- Valida:
  - `PROVEEDOR` -> `200 OK` en `POST /envios/{pedidoId}/listo-para-retiro`
  - `LOGISTICA_MD` -> `403 Forbidden` en ese endpoint

## Validacion
- Ejecutado: `./mvnw -q test`
- Resultado: OK
