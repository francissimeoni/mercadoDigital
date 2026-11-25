ALTER TABLE rmas
    ADD COLUMN monto_compensacion DECIMAL(18,2) NULL,
    ADD COLUMN referencia_compensacion VARCHAR(160) NULL;
