package com.comanda.onboarding.seed;

import com.comanda.menu.additionals.AdditionalService;
import com.comanda.menu.additionals.web.AdditionalGroupRequest;
import com.comanda.menu.additionals.web.AdditionalItemRequest;
import com.comanda.menu.categories.CategoryResponse;
import com.comanda.menu.categories.CategoryService;
import com.comanda.menu.categories.web.CategoryRequest;
import com.comanda.menu.products.ProductResponse;
import com.comanda.menu.products.ProductService;
import com.comanda.menu.products.web.ProductRequest;
import com.comanda.onboarding.Segment;
import com.comanda.onboarding.seed.SeedCatalog.SeedAdditionalGroup;
import com.comanda.onboarding.seed.SeedCatalog.SeedCategory;
import com.comanda.onboarding.seed.SeedCatalog.SeedProduct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes {@link SeedCatalog} as real, editable tenant data (design.md Decision 7) by
 * calling {@code menu-management}'s own creation services — the exact same path a dono would hit
 * editing the cardápio by hand, so no cardápio rule is reimplemented here (task 5.2). Because
 * those services already enforce {@code PlanPolicyService} (task 2.1/2.2), a freshly signed-up
 * tenant seeding a catalog well under the Gratuito limits never trips them.
 */
@Service
public class OnboardingSeedService {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final AdditionalService additionalService;

    public OnboardingSeedService(CategoryService categoryService, ProductService productService, AdditionalService additionalService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.additionalService = additionalService;
    }

    @Transactional
    public void seed(Segment segment) {
        for (SeedCategory seedCategory : SeedCatalog.forSegment(segment)) {
            CategoryResponse category = categoryService.create(new CategoryRequest(seedCategory.name(), true));
            for (SeedProduct seedProduct : seedCategory.products()) {
                ProductResponse product = productService.create(new ProductRequest(
                        seedProduct.name(), seedProduct.description(), seedProduct.price(), true, null, category.id(), null));
                for (SeedAdditionalGroup seedGroup : seedProduct.additionalGroups()) {
                    var group = additionalService.createGroup(product.id(), new AdditionalGroupRequest(
                            seedGroup.name(), seedGroup.required(), seedGroup.selectionType(),
                            seedGroup.minSelections(), seedGroup.maxSelections()));
                    for (var seedItem : seedGroup.items()) {
                        additionalService.createItem(group.id(), new AdditionalItemRequest(seedItem.name(), seedItem.additionalPrice(), true));
                    }
                }
            }
        }
    }
}
