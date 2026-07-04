/**
 * Order persistence and the owner's operation panel: idempotent public creation, linear status
 * machine with idempotent advance, append-only history, cancellation, and the authenticated
 * panel endpoints (list/detail/advance/cancel/open-close). Sub-organized by concern ({@code
 * domain}, {@code creation}, {@code panel}, {@code web}), following {@code menu-management}'s
 * pattern from {@code owner-auth}.
 */
package com.comanda.orders;
