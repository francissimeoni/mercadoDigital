SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'configuracion_psp'
      AND column_name = 'seguro_transporte_porcentaje'
);

SET @ddl := IF(
    @col_exists = 0,
    'ALTER TABLE configuracion_psp ADD COLUMN seguro_transporte_porcentaje DECIMAL(8,4) NOT NULL DEFAULT 2.0000',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
