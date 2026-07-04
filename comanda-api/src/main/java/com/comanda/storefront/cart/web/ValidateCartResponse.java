package com.comanda.storefront.cart.web;

import java.math.BigDecimal;
import java.util.List;

public record ValidateCartResponse(
        boolean valid, List<ValidateCartLineResult> lines, BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal total) {
}
