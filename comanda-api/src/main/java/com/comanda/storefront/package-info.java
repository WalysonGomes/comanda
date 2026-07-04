/**
 * Public storefront: unauthenticated customer-facing surface (`nomedonegocio.$APP_DOMAIN`).
 * Resolves the tenant by subdomain (no JWT), reads the menu already built by {@code menu}
 * (categories/products/additionals) filtered by today's availability, computes open/closed from
 * {@code tenants.is_open} + {@code business_hours}, and validates a cart's availability/prices at
 * checkout time. Sub-organized by resource ({@code business}, {@code menu}, {@code cart}), with
 * shared tenant-read models and the {@link com.comanda.storefront.BusinessClock} under
 * {@code domain} / the slice root, following the {@code menu} slice's pattern. Never persists an
 * order — that boundary belongs to {@code order-operation} (see proposal.md Decision 7).
 */
package com.comanda.storefront;
