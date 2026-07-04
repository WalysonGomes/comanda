package com.comanda.storefront.cart;

import com.comanda.menu.domain.AdditionalItem;
import com.comanda.menu.domain.AdditionalItemRepository;
import com.comanda.menu.domain.Product;
import com.comanda.menu.domain.ProductRepository;
import com.comanda.platform.tenancy.TenantContext;
import com.comanda.storefront.BusinessClock;
import com.comanda.storefront.DeliveryType;
import com.comanda.storefront.TenantNotFoundException;
import com.comanda.storefront.cart.web.ValidateCartLineRequest;
import com.comanda.storefront.cart.web.ValidateCartLineResult;
import com.comanda.storefront.cart.web.ValidateCartRequest;
import com.comanda.storefront.cart.web.ValidateCartResponse;
import com.comanda.storefront.domain.StorefrontTenantRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Checkout-time source of truth (design.md Decision 6): re-checks every line against the tenant
 * resolved from the subdomain — never the client's — and recomputes prices server-side so a
 * price/availability change mid-session can never reach the WhatsApp handoff unnoticed. A product
 * or additional id from another tenant is indistinguishable from one that doesn't exist.
 */
@Service
public class CartValidationService {

    private final ProductRepository productRepository;
    private final AdditionalItemRepository itemRepository;
    private final StorefrontTenantRepository tenantRepository;
    private final BusinessClock clock;

    public CartValidationService(
            ProductRepository productRepository,
            AdditionalItemRepository itemRepository,
            StorefrontTenantRepository tenantRepository,
            BusinessClock clock) {
        this.productRepository = productRepository;
        this.itemRepository = itemRepository;
        this.tenantRepository = tenantRepository;
        this.clock = clock;
    }

    public ValidateCartResponse validate(ValidateCartRequest request) {
        Long tenantId = TenantContext.get();
        var tenant = tenantRepository.findById(tenantId).orElseThrow(TenantNotFoundException::new);
        int today = clock.dayOfWeek();

        List<ValidateCartLineResult> results = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        boolean allValid = true;

        for (ValidateCartLineRequest line : request.lines()) {
            Optional<BigDecimal> unitPrice = resolveUnitPrice(line, today);
            if (unitPrice.isEmpty()) {
                allValid = false;
                results.add(new ValidateCartLineResult(line.lineId(), false, null, null));
                continue;
            }
            BigDecimal lineTotal = unitPrice.get().multiply(BigDecimal.valueOf(line.quantity()));
            subtotal = subtotal.add(lineTotal);
            results.add(new ValidateCartLineResult(line.lineId(), true, unitPrice.get(), lineTotal));
        }

        BigDecimal deliveryFee = request.deliveryType() == DeliveryType.ENTREGA ? tenant.getDeliveryFee() : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(deliveryFee);
        return new ValidateCartResponse(allValid, results, subtotal, deliveryFee, total);
    }

    private Optional<BigDecimal> resolveUnitPrice(ValidateCartLineRequest line, int today) {
        Optional<Product> productOpt = productRepository.findById(line.productId()).filter(p -> isAvailableToday(p, today));
        if (productOpt.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal unitPrice = productOpt.get().getPrice();
        for (Long itemId : line.additionalItemIds()) {
            Optional<AdditionalItem> itemOpt = itemRepository.findByIdAndTenantId(itemId, TenantContext.get());
            boolean belongsToProduct = itemOpt.isPresent()
                    && itemOpt.get().getAdditionalGroup().getProduct().getId().equals(line.productId());
            if (!belongsToProduct || !itemOpt.get().isAvailable()) {
                return Optional.empty();
            }
            unitPrice = unitPrice.add(itemOpt.get().getAdditionalPrice());
        }
        return Optional.of(unitPrice);
    }

    private boolean isAvailableToday(Product product, int today) {
        if (!product.isAvailable()) {
            return false;
        }
        Integer[] days = product.getAvailableDays();
        return days == null || days.length == 0 || Arrays.asList(days).contains(today);
    }
}
