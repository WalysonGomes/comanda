package com.comanda.storefront.menu;

import com.comanda.menu.additionals.AdditionalGroupResponse;
import java.math.BigDecimal;
import java.util.List;

public record StorefrontProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        List<AdditionalGroupResponse> additionalGroups) {
}
