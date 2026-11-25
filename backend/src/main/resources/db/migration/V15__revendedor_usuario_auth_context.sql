alter table revendedores
    add column usuario_id char(36) null;

alter table revendedores
    add constraint uk_revendedores_usuario_id unique (usuario_id);

alter table revendedores
    add constraint fk_revendedores_usuario
        foreign key (usuario_id) references usuarios(id);
