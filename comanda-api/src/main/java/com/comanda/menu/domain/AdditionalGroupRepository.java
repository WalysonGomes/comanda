package com.comanda.menu.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalGroupRepository extends JpaRepository<AdditionalGroup, Long> {

    List<AdditionalGroup> findAllByProductId(Long productId);

    @Query("select g from AdditionalGroup g join g.product p where g.id = :id and p.tenantId = :tenantId")
    Optional<AdditionalGroup> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    void deleteAllByProductId(Long productId);
}
