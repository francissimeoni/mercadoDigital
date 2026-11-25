CREATE TABLE auditoria_eventos (
  id CHAR(36) PRIMARY KEY,
  tipo_evento VARCHAR(80) NOT NULL,
  referencia_id CHAR(36) NULL,
  payload_json JSON NULL,
  fecha_evento DATETIME(6) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE usuarios (
  id CHAR(36) PRIMARY KEY,
  email VARCHAR(180) NOT NULL UNIQUE,
  nombre VARCHAR(120) NOT NULL,
  tipo_usuario VARCHAR(40) NOT NULL,
  activo BIT(1) NOT NULL DEFAULT b'1',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
);

CREATE TABLE roles (
  id CHAR(36) PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE usuarios_roles (
  usuario_id CHAR(36) NOT NULL,
  rol_id CHAR(36) NOT NULL,
  CONSTRAINT pk_usuarios_roles PRIMARY KEY (usuario_id, rol_id),
  CONSTRAINT uq_usuarios_roles UNIQUE (usuario_id, rol_id),
  CONSTRAINT fk_usuarios_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  CONSTRAINT fk_usuarios_roles_rol FOREIGN KEY (rol_id) REFERENCES roles(id)
);
