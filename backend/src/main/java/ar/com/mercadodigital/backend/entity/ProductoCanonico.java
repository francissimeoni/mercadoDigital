package ar.com.mercadodigital.backend.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "productos_canonicos")
public class ProductoCanonico extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String marca;

    @Column(nullable = false, length = 120)
    private String modelo;

    @Column(name = "nombre_publico", nullable = false, length = 200)
    private String nombrePublico;

    @Column(name = "atributos_json", columnDefinition = "json")
    private String atributosJson;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "productoCanonico")
    private List<Producto> productos = new ArrayList<>();
}
