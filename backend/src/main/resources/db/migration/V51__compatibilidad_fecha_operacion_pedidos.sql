-- Algunas bases productivas anteriores a Flyway conservan esta columna obligatoria.
-- El valor por defecto permite crear pedidos desde el checkout sin perder la fecha legacy.
SET @fecha_operacion_existe = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pedidos'
      AND COLUMN_NAME = 'fecha_operacion'
);

SET @agregar_fecha_operacion = IF(
    @fecha_operacion_existe = 0,
    'ALTER TABLE pedidos ADD COLUMN fecha_operacion DATETIME(6) NULL',
    'SELECT 1'
);

PREPARE agregar_fecha_operacion_stmt FROM @agregar_fecha_operacion;
EXECUTE agregar_fecha_operacion_stmt;
DEALLOCATE PREPARE agregar_fecha_operacion_stmt;

UPDATE pedidos
SET fecha_operacion = COALESCE(fecha_operacion, created_at, CURRENT_TIMESTAMP(6))
WHERE fecha_operacion IS NULL;

ALTER TABLE pedidos
    MODIFY COLUMN fecha_operacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
