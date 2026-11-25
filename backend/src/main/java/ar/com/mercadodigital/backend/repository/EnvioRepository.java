package ar.com.mercadodigital.backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.Envio;

public interface EnvioRepository extends JpaRepository<Envio, UUID> {
    @EntityGraph(attributePaths = { "pedido", "pedido.items", "pedido.items.producto", "transportista" })
    Optional<Envio> findByPedido_Id(UUID pedidoId);

    @EntityGraph(attributePaths = { "pedido", "pedido.cliente", "pedido.revendedor", "transportista" })
    Optional<Envio> findByPedido_IdAndPedido_Revendedor_Id(UUID pedidoId, UUID revendedorId);


    @EntityGraph(attributePaths={"pedido", "pedido.cliente", "pedido.revendedor", "pedido.revendedor.usuario", "transportista"})
    public Optional<Envio> findByPedido_TrackingToken(String var1);
}
