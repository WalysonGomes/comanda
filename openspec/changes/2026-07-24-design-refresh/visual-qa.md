# Visual QA — design refresh

## Execution

- Date: 2026-08-10 (`America/Fortaleza`).
- Environment: production Vite assets served by the Spring Boot JAR on localhost; PostgreSQL 17 in a disposable Docker container; Chromium through the Codex in-app browser.
- Authentication/data: a temporary owner completed the real onboarding flow. A real `RECEBIDO` order was created through the tenant storefront API and opened in the owner dashboard. The application and database container were stopped and removed after inspection.
- Local, untracked references: `.design/Comanda Landing.dc.html` and `.design/Comanda Painel.dc.html`.
- Versioned reference/acceptance assets: `comanda-client/public/comanda-logo.svg`, PWA icons/manifest, and the accepted images under `comanda-client/public/shots/` (notably `storefront.png`, `pedidos.png`, `cardapio.png`, and `detail.png`). `.design` was read-only and was not copied or added to Git.

## Matrix and results

| Surface/check | Viewport | Result | Evidence/notes |
| --- | ---: | --- | --- |
| Landing | 360 px | Pass | Mobile menu button visible; hero, copy, CTAs, and overlapping product screenshots reflowed without document overflow. |
| Landing | 390 px | Pass | Same mobile composition and destinations; no horizontal document overflow. |
| Landing | 768 px | Pass | Desktop navigation active at the tablet breakpoint; one `h1` and expected `nav`/`main` landmarks. |
| Landing | 1280 px | Pass | Two-column hero and desktop navigation matched the approved warm composition; no horizontal document overflow. |
| Login | Mobile (390 px) | Pass | Form, logo, fields, CTA, and onboarding link fit without overflow; unsupported password-recovery and unqualified real-time copy were absent. |
| Onboarding | Mobile (390 px) | Pass | All five real steps were completed: segment, account, hours, first product, and published-card confirmation. |
| Orders dashboard | Mobile (390 px) | Pass | Real order card displayed `Novo`, customer, item, fulfillment, time, and total. The status-chip row scrolls within its own control while the document remains overflow-free. |
| Real order detail | Mobile (390 px) | Pass | Sheet displayed textual stepper (`Novo`, `Aceito`, `Em preparo`, `Pronto`, `Entregue`), customer, item, note, totals, WhatsApp preview, and actions. Meaning did not depend on color. |
| Menu | Mobile (390 px) | Pass | Seeded categories/products, availability text, edit controls, and bottom navigation reflowed without document overflow. |
| Orders dashboard | Desktop (1280 px) | Pass | Authenticated layout and navigation remained usable with the approved palette and status terminology. |
| Menu | Desktop (1280 px) | Pass | Menu composition remained readable with no document overflow. |
| Navigation | Mobile/desktop | Pass | Collapsible keyboard-operable landing menu at mobile widths; full landing navigation at 768/1280; owner bottom navigation remained visible and labeled. |

## Cross-cutting checks

- Palette/brand: rendered surfaces used the approved brick accent `#b53c25`, cream/card neutrals, and green success treatment. Runtime `theme-color`, manifest, favicon, Apple icon, and PWA/maskable icons were checked against the same accent.
- Status consistency: filters/cards/detail used `Novo`, `Aceito`, `Em preparo`, `Pronto`, `Entregue`, and `Cancelado` consistently; the stepper uses `Em preparo` rather than the earlier shortened label.
- Focus: links, menu control, and CTAs have visible `:focus-visible` treatment; keyboard-operable menu behavior is covered by the landing interaction test.
- Reduced motion: the reduced-motion media query disables non-essential animations without applying the reveal-hidden state; the landing test renders with reduced motion enabled and confirms content/landmarks remain present.
- Responsive composition: checks at 360, 390, 768, and 1280 px found no horizontal document overflow. The orders filter strip intentionally uses contained horizontal scrolling on narrow screens.

## Deviations and limitations

- No new screenshots were committed; the record references the approved assets and the reproduced browser session.
- The authenticated dataset was temporary and was deleted with its disposable database after the checks.
- This QA does not validate checkout reliability or `Dados do negócio`; both are outside this change, and checkout reliability remains a release blocker.

## Conclusion

**Pass for design-refresh visual acceptance.** The required public and authenticated surfaces were reproduced and checked at the listed viewports. No unresolved visual deviation remains within task 4.3. This conclusion does not claim release readiness and does not authorize archive or merge.
