package ar.com.mercadodigital.backend.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", nullable = false, length = 40)
    private TipoUsuario tipoUsuario = TipoUsuario.CLIENTE;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "telefono_whatsapp", length = 80)
    private String telefonoWhatsapp;

    @Column(length = 120)
    private String provincia;

    @Column(length = 160)
    private String localidad;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(length = 180)
    private String calle;

    @Column(length = 30)
    private String numero;

    @Column(name = "piso_depto", length = 80)
    private String pisoDepto;

    @Column(name = "observaciones_envio", columnDefinition = "TEXT")
    private String observacionesEnvio;

    @ManyToMany
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    @OneToMany(mappedBy = "cliente")
    private Set<Pedido> pedidosCliente = new HashSet<>();

    @OneToMany(mappedBy = "creadoPor")
    private Set<RMA> rmasCreados = new HashSet<>();

    @OneToMany(mappedBy = "usuario")
    private Set<ClienteRevendedor> clientesRevendedor = new HashSet<>();
}
