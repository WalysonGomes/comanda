package com.comanda.storefront.menu;

import java.util.List;

public record StorefrontCategoryResponse(Long id, String name, List<StorefrontProductResponse> products) {
}
