package com.comanda.menu.domain;

import com.comanda.platform.tenancy.TenantScopedRepository;
import java.util.List;

public interface ProductRepository extends TenantScopedRepository<Product, Long> {

    List<Product> findAllByOrderByPositionAsc();

    List<Product> findAllByCategoryIdOrderByPositionAsc(Long categoryId);

    long countByCategoryId(Long categoryId);
}
