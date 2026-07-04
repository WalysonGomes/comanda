package com.comanda.storefront.cart.web;

import java.math.BigDecimal;

/**
 * {@code unitPrice}/{@code lineTotal} are the server's authoritative prices for this instant
 * (design.md Decision 6) — null when {@code available} is false, since there's no price to trust.
 */
public record ValidateCartLineResult(String lineId, boolean available, BigDecimal unitPrice, BigDecimal lineTotal) {
}
