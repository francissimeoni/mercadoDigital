## Idioma
- Tablas, columnas, servicios, endpoints y clases en español (sin conjeturas).

## Base de datos
- MySQL 8.
- Flyway como única fuente de verdad del esquema.
- Hibernate ddl-auto: validate.
- Claves primarias: UUID (CHAR(36)) o BINARY(16) (definir al pasar a prod).
- Campos estándar: created_at, updated_at (timestamp).

## Estilo de código
- Paquetes:
  - controller, service, repository, entity, dto, mapper, security, exception, util, config
- DTOs:
  - dto/request y dto/response
- Errores:
  - GlobalExceptionHandler + ApiErrorResponse

## Reglas críticas
- Idempotencia obligatoria en webhooks PSP (clave id_evento_psp UNIQUE).
- Logs auditables para:
  - pagos
  - splits
  - envíos
  - rma
- No exponer proveedor al cliente final ni al revendedor (en dropshipping privado).
