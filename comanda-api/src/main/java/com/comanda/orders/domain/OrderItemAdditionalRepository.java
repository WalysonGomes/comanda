package com.comanda.orders.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemAdditionalRepository extends JpaRepository<OrderItemAdditional, Long> {

    List<OrderItemAdditional> findAllByOrderItemIdOrderById(Long orderItemId);
}
