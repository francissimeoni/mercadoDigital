package ar.com.mercadodigital.backend.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transportistas")
public class Transportista {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, updatable = false, length = 36)
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "transportista")
    private List<TarifaTransportista> tarifas = new ArrayList<>();
}
