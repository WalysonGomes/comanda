package com.comanda.orders.domain;

import java.util.List;
import java.util.Optional;

/**
 * Linear state machine (design.md Decision 4): {@code CANCELADO} sits outside the sequence and is
 * reachable from any non-terminal status (see {@code OrderService#cancel}), never via {@link
 * #next()}.
 */
public enum OrderStatus {
    RECEBIDO,
    ACEITO,
    EM_PREPARO,
    PRONTO,
    ENTREGUE,
    CANCELADO;

    private static final List<OrderStatus> SEQUENCE = List.of(RECEBIDO, ACEITO, EM_PREPARO, PRONTO, ENTREGUE);

    public Optional<OrderStatus> next() {
        int index = SEQUENCE.indexOf(this);
        if (index < 0 || index == SEQUENCE.size() - 1) {
            return Optional.empty();
        }
        return Optional.of(SEQUENCE.get(index + 1));
    }

    public boolean isTerminal() {
        return this == ENTREGUE || this == CANCELADO;
    }

    /** Position in the linear sequence, or -1 for {@code CANCELADO} (outside the line). */
    public static int sequenceIndex(OrderStatus status) {
        return SEQUENCE.indexOf(status);
    }
}
