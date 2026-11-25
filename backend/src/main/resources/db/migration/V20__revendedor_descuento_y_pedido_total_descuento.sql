alter table revendedores
    add column codigo_descuento varchar(60),
    add column descuento_fijo numeric(19,2) not null default 0;

alter table pedidos
    add column total_descuento numeric(19,2) not null default 0,
    add column codigo_descuento_aplicado varchar(60);
