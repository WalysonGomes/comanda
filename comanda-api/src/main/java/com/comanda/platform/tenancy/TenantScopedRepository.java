package com.comanda.platform.tenancy;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * Base repository for {@link TenantScopedEntity} subclasses. Spring Data's default {@code
 * findById} calls {@code EntityManager.find()}, which Hibernate does not run through an enabled
 * {@code @Filter} (a filter only restricts HQL/JPQL/Criteria queries, not primary-key lookups) —
 * so it would leak cross-tenant rows by id despite the filter being enabled. The convention is:
 * tenant-scoped entities extend {@link TenantScopedEntity} and their repositories extend this
 * instead of a plain {@code JpaRepository}, so every lookup goes through a filtered query.
 */
@NoRepositoryBean
public interface TenantScopedRepository<T extends TenantScopedEntity, ID> extends JpaRepository<T, ID> {

    @Override
    @Query("select e from #{#entityName} e where e.id = :id")
    Optional<T> findById(@Param("id") ID id);
}
