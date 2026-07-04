package com.comanda.storefront.menu;

import com.comanda.menu.additionals.AdditionalGroupResponse;
import com.comanda.menu.additionals.AdditionalItemResponse;
import com.comanda.menu.domain.AdditionalGroup;
import com.comanda.menu.domain.AdditionalGroupRepository;
import com.comanda.menu.domain.AdditionalItem;
import com.comanda.menu.domain.AdditionalItemRepository;
import com.comanda.menu.domain.Category;
import com.comanda.menu.domain.CategoryRepository;
import com.comanda.menu.domain.Product;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.storefront.BusinessClock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Read-only view of {@code menu-management}'s catalog, filtered to what's actually orderable right
 * now (design.md Decision 2): unavailable products are omitted entirely, never sent as
 * "disabled" — and categories left with nothing to show disappear too (PRD Seção 3.1).
 */
@Service
public class StorefrontMenuService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final AdditionalGroupRepository groupRepository;
    private final AdditionalItemRepository itemRepository;
    private final BusinessClock clock;

    public StorefrontMenuService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            AdditionalGroupRepository groupRepository,
            AdditionalItemRepository itemRepository,
            BusinessClock clock) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.groupRepository = groupRepository;
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    public List<StorefrontCategoryResponse> getMenu() {
        int today = clock.dayOfWeek();

        Map<Long, List<Product>> productsByCategory = productRepository.findAllByOrderByPositionAsc().stream()
                .filter(p -> isAvailableToday(p, today))
                .collect(Collectors.groupingBy(p -> p.getCategory().getId(), LinkedHashMap::new, Collectors.toList()));

        return categoryRepository.findAllByOrderByPositionAsc().stream()
                .filter(Category::isActive)
                .map(category -> new StorefrontCategoryResponse(
                        category.getId(),
                        category.getName(),
                        productsByCategory.getOrDefault(category.getId(), List.of()).stream()
                                .map(this::toProductResponse)
                                .toList()))
                .filter(category -> !category.products().isEmpty())
                .toList();
    }

    private boolean isAvailableToday(Product product, int today) {
        if (!product.isAvailable()) {
            return false;
        }
        Integer[] days = product.getAvailableDays();
        return days == null || days.length == 0 || Arrays.asList(days).contains(today);
    }

    private StorefrontProductResponse toProductResponse(Product product) {
        List<AdditionalGroupResponse> groups = groupRepository.findAllByProductId(product.getId()).stream()
                .map(this::toGroupResponse)
                .toList();
        return new StorefrontProductResponse(
                product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getImageUrl(), groups);
    }

    private AdditionalGroupResponse toGroupResponse(AdditionalGroup group) {
        List<AdditionalItemResponse> items = itemRepository.findAllByAdditionalGroupId(group.getId()).stream()
                .filter(AdditionalItem::isAvailable)
                .map(this::toItemResponse)
                .toList();
        return new AdditionalGroupResponse(
                group.getId(), group.getProduct().getId(), group.getName(), group.isRequired(),
                group.getSelectionType(), group.getMinSelections(), group.getMaxSelections(), items);
    }

    private AdditionalItemResponse toItemResponse(AdditionalItem item) {
        return new AdditionalItemResponse(
                item.getId(), item.getAdditionalGroup().getId(), item.getName(), item.getAdditionalPrice(), item.isAvailable());
    }
}
