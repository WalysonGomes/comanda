package com.comanda.storefront.cart.web;

import com.comanda.storefront.cart.CartValidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loja")
public class CartValidationController {

    private final CartValidationService cartValidationService;

    public CartValidationController(CartValidationService cartValidationService) {
        this.cartValidationService = cartValidationService;
    }

    @PostMapping("/carrinho/validar")
    public ValidateCartResponse validate(@Valid @RequestBody ValidateCartRequest request) {
        return cartValidationService.validate(request);
    }
}
