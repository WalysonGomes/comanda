package com.comanda.menu.categories;

import com.comanda.menu.CategoryNotEmptyException;
import com.comanda.menu.CategoryNotFoundException;
import com.comanda.menu.categories.web.CategoryRequest;
import com.comanda.menu.domain.Category;
import com.comanda.menu.domain.CategoryRepository;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.plans.PlanLimitExceededException;
import com.comanda.plans.PlanPolicyService;
import com.comanda.platform.tenancy.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD isolated by tenant (design.md Decision 4). {@code findById} goes through
 * {@link com.comanda.platform.tenancy.TenantScopedRepository}'s filtered override, so a category
 * from another tenant surfaces as {@link CategoryNotFoundException} (404), never leaking existence.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PlanPolicyService planPolicyService;

    public CategoryService(
            CategoryRepository categoryRepository, ProductRepository productRepository, PlanPolicyService planPolicyService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.planPolicyService = planPolicyService;
    }

    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByPositionAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (!planPolicyService.canCreateCategory()) {
            throw PlanLimitExceededException.categories();
        }
        Long tenantId = TenantContext.get();
        int position = (int) categoryRepository.count();
        Category category = new Category(tenantId, request.name().trim(), position);
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new);
        category.setName(request.name().trim());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        return toResponse(category);
    }

    @Transactional
    public void reorder(List<Long> orderedIds) {
        List<Category> categories = categoryRepository.findAllById(orderedIds);
        if (categories.size() != orderedIds.size()) {
            throw new CategoryNotFoundException();
        }
        Map<Long, Category> byId = new LinkedHashMap<>();
        categories.forEach(c -> byId.put(c.getId(), c));
        for (int i = 0; i < orderedIds.size(); i++) {
            byId.get(orderedIds.get(i)).setPosition(i);
        }
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(CategoryNotFoundException::new);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new CategoryNotEmptyException(productCount);
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getPosition(), category.isActive());
    }
}
