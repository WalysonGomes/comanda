package com.comanda.platform.tenancy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enables the {@code tenantFilter} Hibernate filter (see {@link TenantScopedEntity}) on the
 * request's persistence context once {@link TenantContext} is populated, so every query against
 * a tenant-scoped entity is restricted transparently for the rest of the request.
 */
@Component
public class TenantFilterInterceptor implements HandlerInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.isResolved()) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter(TenantScopedEntity.FILTER_NAME).setParameter("tenantId", TenantContext.get());
        }
        return true;
    }
}
