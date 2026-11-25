-- V34-V45 ya fueron aplicadas en produccion, pero sus fuentes se perdieron. Esta
-- migracion nueva reconcilia solamente los contratos necesarios y evita alterar
-- los checksums que Flyway ya tiene registrados.

CREATE TABLE IF NOT EXISTS producto_preguntas (
    id CHAR(36) NOT NULL,
    producto_id CHAR(36) NULL,
    producto_canonico_id CHAR(36) NULL,
    proveedor_id CHAR(36) NULL,
    revendedor_id CHAR(36) NULL,
    revendedor_producto_id CHAR(36) NULL,
    nombre_contacto VARCHAR(120) NULL,
    email_contacto VARCHAR(180) NULL,
    telefono_contacto VARCHAR(80) NULL,
    pregunta TEXT NOT NULL,
    respuesta TEXT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_pregunta DATETIME(6) NOT NULL,
    fecha_respuesta DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_producto_preguntas_revendedor (revendedor_id),
    KEY idx_producto_preguntas_propio (revendedor_producto_id),
    CONSTRAINT fk_producto_preguntas_revendedor
        FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT fk_producto_preguntas_propio
        FOREIGN KEY (revendedor_producto_id) REFERENCES revendedor_productos(id)
);

CREATE TABLE IF NOT EXISTS consultas_disponibilidad (
    id CHAR(36) NOT NULL,
    producto_id CHAR(36) NULL,
    producto_canonico_id CHAR(36) NULL,
    proveedor_id CHAR(36) NULL,
    revendedor_id CHAR(36) NULL,
    revendedor_producto_id CHAR(36) NULL,
    nombre_contacto VARCHAR(120) NULL,
    email_contacto VARCHAR(180) NULL,
    telefono_contacto VARCHAR(80) NULL,
    localidad VARCHAR(160) NOT NULL,
    observaciones TEXT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    respuesta_proveedor TEXT NULL,
    checkout_token VARCHAR(80) NULL,
    fecha_consulta DATETIME(6) NOT NULL,
    fecha_respuesta DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    accion_token VARCHAR(80) NULL,
    accion_token_expira_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultas_disponibilidad_checkout_token (checkout_token),
    UNIQUE KEY uk_consultas_disponibilidad_accion_token (accion_token),
    KEY idx_consultas_disponibilidad_revendedor (revendedor_id),
    KEY idx_consultas_disponibilidad_propio (revendedor_producto_id),
    CONSTRAINT fk_consultas_disponibilidad_revendedor
        FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT fk_consultas_disponibilidad_propio
        FOREIGN KEY (revendedor_producto_id) REFERENCES revendedor_productos(id)
);

CREATE TABLE IF NOT EXISTS storefront_credenciales (
    id CHAR(36) NOT NULL,
    revendedor_id CHAR(36) NOT NULL,
    usuario_id CHAR(36) NOT NULL,
    tipo_acceso VARCHAR(40) NOT NULL,
    email_login VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    activo BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_storefront_credenciales_tenant_email (revendedor_id, email_login),
    UNIQUE KEY uk_storefront_credenciales_tenant_usuario_tipo (revendedor_id, usuario_id, tipo_acceso),
    CONSTRAINT fk_storefront_credenciales_revendedor
        FOREIGN KEY (revendedor_id) REFERENCES revendedores(id),
    CONSTRAINT fk_storefront_credenciales_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'asesores_venta' AND column_name = 'usuario_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE asesores_venta ADD COLUMN usuario_id CHAR(36) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'asesores_venta' AND index_name = 'uk_asesores_venta_usuario');
SET @sql = IF(@index_exists = 0, 'ALTER TABLE asesores_venta ADD UNIQUE KEY uk_asesores_venta_usuario (usuario_id)', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @constraint_exists = (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'asesores_venta' AND column_name = 'usuario_id' AND referenced_table_name = 'usuarios');
SET @sql = IF(@constraint_exists = 0, 'ALTER TABLE asesores_venta ADD CONSTRAINT fk_asesores_venta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'telefono_whatsapp');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN telefono_whatsapp VARCHAR(80) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'provincia');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN provincia VARCHAR(120) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'localidad');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN localidad VARCHAR(160) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'codigo_postal');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN codigo_postal VARCHAR(20) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'calle');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN calle VARCHAR(180) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'numero');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN numero VARCHAR(30) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'piso_depto');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN piso_depto VARCHAR(80) NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'usuarios' AND column_name = 'observaciones_envio');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE usuarios ADD COLUMN observaciones_envio TEXT NULL', 'SELECT 1');
PREPARE migration_stmt FROM @sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;
