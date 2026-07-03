package com.comanda.menu.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalItemRepository extends JpaRepository<AdditionalItem, Long> {

    List<AdditionalItem> findAllByAdditionalGroupId(Long groupId);

    @Query("select i from AdditionalItem i join i.additionalGroup g join g.product p "
            + "where i.id = :id and p.tenantId = :tenantId")
    Optional<AdditionalItem> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    void deleteAllByAdditionalGroupId(Long groupId);

    void deleteAllByAdditionalGroupProductId(Long productId);
}
