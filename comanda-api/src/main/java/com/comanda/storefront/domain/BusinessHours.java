package com.comanda.storefront.domain;

import com.comanda.platform.tenancy.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

/**
 * Read-only in this slice: {@code business_hours} rows are configured elsewhere (owner
 * onboarding/settings, a later change); the storefront only reads today's row to compute
 * open/closed (design.md Decision 3). Convention: {@code day_of_week} {@code 0=Dom…6=Sáb}
 * (PRD Seção 8), matching {@link com.comanda.storefront.BusinessClock}.
 */
@Entity
@Table(name = "business_hours")
public class BusinessHours extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

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

    public Short getDayOfWeek() {
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
