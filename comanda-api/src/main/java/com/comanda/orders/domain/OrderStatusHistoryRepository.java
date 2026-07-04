package com.comanda.orders.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insert-only usage enforced by convention (PRD 4.4): no update/delete methods declared. */
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findAllByOrderIdOrderByCreatedAtAsc(Long orderId);
}
