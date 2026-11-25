-- Datos comerciales opcionales. No modifica teléfonos, precios ni habilita tiendas.
ALTER TABLE asesores_venta
    ADD COLUMN provincia VARCHAR(80) NULL,
    ADD COLUMN localidad VARCHAR(120) NULL,
    ADD COLUMN zona_atencion VARCHAR(200) NULL;
