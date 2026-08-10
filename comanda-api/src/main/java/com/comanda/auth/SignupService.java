package com.comanda.auth;

import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.auth.domain.User;
import com.comanda.auth.domain.UserRepository;
import com.comanda.auth.security.PasswordHasher;
import com.comanda.auth.security.RefreshTokenService;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signup creates tenant + OWNER user + minimal business data in one transaction (D1). The
 * pre-checks give good UX; the DB unique constraints are the final guard against a race between
 * two concurrent signups for the same e-mail/subdomain (D2), translated back to the same errors
 * the pre-check would have raised.
 */
@Service
public class SignupService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenService refreshTokenService;
    private final TenantSubdomainPolicy subdomainPolicy;

    public SignupService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RefreshTokenService refreshTokenService,
            TenantSubdomainPolicy subdomainPolicy) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenService = refreshTokenService;
        this.subdomainPolicy = subdomainPolicy;
    }

    public record SignupCommand(
            String ownerName,
            String businessName,
            String subdomain,
            String whatsappNumber,
            String email,
            String password) {
    }

    @Transactional
    public RefreshTokenService.Session signup(SignupCommand command) {
        validate(command);
        String subdomain = subdomainPolicy.validateAndNormalize(command.subdomain());
        String email = command.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (tenantRepository.existsBySubdomain(subdomain)) {
            throw new SubdomainAlreadyInUseException();
        }

        try {
            Tenant tenant = tenantRepository.saveAndFlush(
                    new Tenant(subdomain, command.businessName().trim(), command.whatsappNumber().trim()));
            User user = userRepository.saveAndFlush(
                    new User(tenant.getId(), command.ownerName().trim(), email, passwordHasher.hash(command.password())));
            return refreshTokenService.issueNewSession(user);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    private RuntimeException translate(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage());
        if (message.contains("uq_users_email")) {
            return new EmailAlreadyExistsException();
        }
        if (message.contains("uq_tenants_subdomain")) {
            return new SubdomainAlreadyInUseException();
        }
        return e;
    }

    private void validate(SignupCommand command) {
        requireNonBlank(command.ownerName(), "Nome");
        requireNonBlank(command.businessName(), "Nome do negócio");
        requireNonBlank(command.subdomain(), "Subdomínio");
        requireNonBlank(command.whatsappNumber(), "WhatsApp");
        requireNonBlank(command.email(), "E-mail");
        if (!command.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("E-mail inválido.");
        }
        if (command.password() == null || command.password().length() < 6) {
            throw new IllegalArgumentException("Senha deve ter ao menos 6 caracteres.");
        }
    }

    private void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório.");
        }
    }
}
