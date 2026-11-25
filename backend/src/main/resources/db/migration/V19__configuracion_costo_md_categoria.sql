create table configuracion_costo_md_categoria (
    id char(36) not null primary key,
    categoria_paquete varchar(30) not null unique,
    monto numeric(19,2) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);
