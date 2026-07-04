package com.comanda.orders.creation;

import com.comanda.orders.domain.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Public creation contract declared by {@code public-storefront} (design.md Decision 8): no
 * {@code tenantId} field at all — the tenant is always the one resolved from the subdomain by
 * {@link com.comanda.platform.tenancy.SubdomainTenantResolver}, so there's nothing here for a
 * malicious client to override.
 */
public record OrderCreationRequest(
        @NotBlank(message = "idempotencyKey é obrigatório.") String idempotencyKey,
        @NotBlank(message = "Nome do cliente é obrigatório.") String customerName,
        @NotBlank(message = "Telefone do cliente é obrigatório.") String customerPhone,
        @NotNull(message = "Tipo de entrega é obrigatório.") DeliveryType deliveryType,
        String address,
        String notes,
        @NotEmpty(message = "Pedido precisa ter ao menos um item.") @Valid List<OrderLineRequest> items) {
}
