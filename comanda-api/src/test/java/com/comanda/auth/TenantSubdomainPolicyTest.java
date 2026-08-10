package com.comanda.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TenantSubdomainPolicyTest {
    private final TenantSubdomainPolicy policy = new TenantSubdomainPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"www", "app", "api", "docs", "status", "admin", "demo", "signal",
            " WWW ", "Api", " SIGNAL "})
    void rejectsEveryReservedLabelAfterNormalization(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> policy.validateAndNormalize(value))
                .withMessage("Este subdomínio é reservado.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"acme", "acme-food", "loja123", " A-C-M-E "})
    void acceptsAndNormalizesValidTenantLabels(String value) {
        assertThat(policy.validateAndNormalize(value)).matches("[a-z0-9-]+");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "-", "---"})
    void rejectsMalformedLabels(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> policy.validateAndNormalize(value));
    }
}
