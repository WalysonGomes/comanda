package com.comanda.orders.domain;

import com.comanda.platform.tenancy.TenantScopedRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends TenantScopedRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime from);

    @Query("select coalesce(sum(o.total), 0) from Order o where o.createdAt >= :from and o.status <> :excludedStatus")
    BigDecimal revenueSince(@Param("from") OffsetDateTime from, @Param("excludedStatus") OrderStatus excludedStatus);

    @Query("select count(o) from Order o where o.status not in :statuses")
    long countByStatusNotIn(@Param("statuses") Collection<OrderStatus> statuses);
}
