/**
 * Plan model (Gratuito/Essencial): a single policy service answers "can this tenant create one
 * more X?" for products/categories/orders (design.md Decision 1), a retention service filters
 * order history reads by plan, and an admin-only surface activates/downgrades Essencial manually
 * in the MVP (PRD Seção 10) — all independent of how payment is processed (PRD Regra 16).
 */
package com.comanda.plans;
