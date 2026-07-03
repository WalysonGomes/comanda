package com.comanda.auth.web;

import com.comanda.auth.SignupService;

public record SignupRequest(
        String name,
        String businessName,
        String subdomain,
        String whatsappNumber,
        String email,
        String password) {

    SignupService.SignupCommand toCommand() {
        return new SignupService.SignupCommand(name, businessName, subdomain, whatsappNumber, email, password);
    }
}
