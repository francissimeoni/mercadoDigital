package ar.com.mercadodigital.backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "proveedores")
public class Proveedor extends BaseEntity {

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "despacha_mercaderia", nullable = false)
    private boolean despachaMercaderia = false;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToOne(mappedBy = "proveedor")
    private ProveedorMercadoPagoAccount mercadoPagoAccount;

    @OneToMany(mappedBy = "proveedor")
    private List<Producto> productos = new ArrayList<>();
}
