package com.comanda.storefront;

/**
 * How the customer wants to receive the order (PRD Seção 3.1). Part of the storefront ↔
 * order-operation contract (proposal.md Decision 7): {@code ENTREGA} charges the tenant's
 * delivery fee and requires an address; {@code RETIRADA} does neither.
 */
public enum DeliveryType {
    ENTREGA,
    RETIRADA
}
