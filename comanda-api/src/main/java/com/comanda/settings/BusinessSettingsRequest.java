package com.comanda.settings;

import com.comanda.onboarding.web.BusinessHoursRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record BusinessSettingsRequest(
        @NotBlank(message = "Nome do negócio é obrigatório.") @Size(max = 255) String businessName,
        @NotBlank(message = "WhatsApp é obrigatório.")
        @Pattern(regexp = "^[+() 0-9-]{8,20}$", message = "WhatsApp inválido.") String whatsappNumber,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal deliveryFee,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 8, fraction = 2) BigDecimal minOrderValue,
        @NotBlank String subdomain,
        @NotNull @Valid @Size(min = 7, max = 7, message = "Informe os sete dias da semana.")
        List<BusinessHoursRequest.Row> businessHours) {}
