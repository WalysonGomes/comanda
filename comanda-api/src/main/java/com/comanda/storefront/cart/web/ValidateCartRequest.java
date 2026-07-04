package com.comanda.storefront.cart.web;

import com.comanda.storefront.DeliveryType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ValidateCartRequest(
        @NotEmpty(message = "Carrinho vazio.") List<@Valid ValidateCartLineRequest> lines,
        @NotNull(message = "Tipo de entrega obrigatório.") DeliveryType deliveryType) {
}
