package com.comanda.storefront.business.web;

import com.comanda.storefront.business.BusinessResponse;
import com.comanda.storefront.business.BusinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loja")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/negocio")
    public BusinessResponse get() {
        return businessService.get();
    }
}
