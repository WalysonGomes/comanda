package com.comanda.orders;

/**
 * A product/additional in the creation request doesn't exist in the tenant, or exists but is
 * unavailable today (design.md Decision 3: creation is the source-of-truth revalidation, even
 * though `public-storefront` already checks availability before handoff).
 */
public class ItemUnavailableException extends RuntimeException {

    private final String itemName;

    public ItemUnavailableException(String itemName) {
        this.itemName = itemName;
    }

    public String getItemName() {
        return itemName;
    }
}
