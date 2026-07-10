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
 * Read here, written by {@code onboarding} (task 5.3/6.4): the storefront reads today's row to
 * compute open/closed (design.md Decision 3); {@code OnboardingBusinessHoursService} is the write
 * path this class's own comment used to call "a later change" — that change is this one.
 * Convention: {@code day_of_week} {@code 0=Dom…6=Sáb} (PRD Seção 8), matching
 * {@link com.comanda.storefront.BusinessClock}.
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

    public BusinessHours(Long tenantId, short dayOfWeek, LocalTime opensAt, LocalTime closesAt, boolean closed) {
        super(tenantId);
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.closed = closed;
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

    public void update(LocalTime opensAt, LocalTime closesAt, boolean closed) {
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.closed = closed;
    }
}
