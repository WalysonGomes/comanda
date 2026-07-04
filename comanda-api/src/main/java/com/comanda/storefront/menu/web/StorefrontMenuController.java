package com.comanda.storefront.menu.web;

import com.comanda.storefront.menu.StorefrontCategoryResponse;
import com.comanda.storefront.menu.StorefrontMenuService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loja")
public class StorefrontMenuController {

    private final StorefrontMenuService storefrontMenuService;

    public StorefrontMenuController(StorefrontMenuService storefrontMenuService) {
        this.storefrontMenuService = storefrontMenuService;
    }

    @GetMapping("/cardapio")
    public List<StorefrontCategoryResponse> getMenu() {
        return storefrontMenuService.getMenu();
    }
}
