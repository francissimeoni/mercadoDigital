package ar.com.mercadodigital.backend.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.com.mercadodigital.backend.entity.SplitPedidoItem;

public interface SplitPedidoItemRepository extends JpaRepository<SplitPedidoItem, UUID> {
    List<SplitPedidoItem> findByPedidoItem_Pedido_Id(UUID pedidoId);
}
