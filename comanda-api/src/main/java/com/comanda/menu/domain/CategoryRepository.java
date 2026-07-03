package com.comanda.menu.domain;

import com.comanda.platform.tenancy.TenantScopedRepository;
import java.util.List;

public interface CategoryRepository extends TenantScopedRepository<Category, Long> {

    List<Category> findAllByOrderByPositionAsc();
}
