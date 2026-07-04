package com.comanda.orders.domain;

import com.comanda.platform.tenancy.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

/**
 * Read-only mapping of {@code business_hours} (created by {@code foundations}' V1 migration; no
 * feature owned it yet). {@code order-operation} reads today's row to derive the opening banner
 * (design.md Decision 9): "dentro do horário configurado e negócio fechado" (PRD 5.3 / Seção 13's
 * {@code America/Fortaleza} convention, {@code dayOfWeek} 0=Dom…6=Sáb).
 */
@Entity
@Table(name = "business_hours")
public class BusinessHours extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "is_closed", nullable = false)
    private boolean closed;

    protected BusinessHours() {
    }

    public Long getId() {
        return id;
    }

    public short getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public boolean isClosed() {
        return closed;
    }
}
