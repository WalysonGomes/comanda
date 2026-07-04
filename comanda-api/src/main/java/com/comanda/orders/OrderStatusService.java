package com.comanda.orders;

import com.comanda.orders.domain.Order;
import com.comanda.orders.domain.OrderStatus;
import com.comanda.orders.domain.OrderStatusHistory;
import com.comanda.orders.domain.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place every order status change goes through — creation, advance, cancellation
 * (design.md Decision 5) — so {@code order_status_history} is always consistent and append-only
 * (PRD 4.4): this method only ever inserts, never updates or deletes a history row.
 */
@Service
public class OrderStatusService {

    private final OrderStatusHistoryRepository historyRepository;

    public OrderStatusService(OrderStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void transition(Order order, OrderStatus from, OrderStatus to, Long changedByUserId) {
        order.setStatus(to);
        historyRepository.save(new OrderStatusHistory(order, from, to, changedByUserId, null));
    }
}
