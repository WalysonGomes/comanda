package com.comanda.menu;

/** Design.md Decision 6: removing a category with products is blocked, never cascaded. */
public class CategoryNotEmptyException extends RuntimeException {

    private final long productCount;

    public CategoryNotEmptyException(long productCount) {
        this.productCount = productCount;
    }

    public long getProductCount() {
        return productCount;
    }
}
