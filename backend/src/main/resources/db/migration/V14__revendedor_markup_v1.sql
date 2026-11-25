alter table revendedores
    add column markup_porcentaje_default decimal(5,2) not null default 0.00,
    add column markup_fijo_default decimal(19,2) not null default 0.00,
    add column comision_asesor_porcentaje_default decimal(5,2) not null default 0.00,
    add column comision_asesor_fija_default decimal(19,2) not null default 0.00;

alter table revendedor_markups
    add column comision_asesor_porcentaje decimal(5,2) null,
    add column comision_asesor_fija decimal(19,2) null;
