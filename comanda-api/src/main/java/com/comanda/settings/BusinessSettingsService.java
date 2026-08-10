package com.comanda.settings;

import com.comanda.auth.SubdomainAlreadyInUseException;
import com.comanda.auth.TenantSubdomainPolicy;
import com.comanda.auth.domain.Tenant;
import com.comanda.auth.domain.TenantRepository;
import com.comanda.menu.images.ImageStorage;
import com.comanda.menu.images.ImageType;
import com.comanda.onboarding.hours.OnboardingBusinessHoursService;
import com.comanda.onboarding.hours.OnboardingBusinessHoursService.DayHours;
import com.comanda.platform.tenancy.TenantContext;
import com.comanda.storefront.domain.BusinessHoursRepository;
import java.util.HashSet;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessSettingsService {
    private final TenantRepository tenants;
    private final BusinessHoursRepository hours;
    private final OnboardingBusinessHoursService hoursWriter;
    private final TenantSubdomainPolicy subdomains;
    private final ImageStorage images;

    public BusinessSettingsService(TenantRepository tenants, BusinessHoursRepository hours,
            OnboardingBusinessHoursService hoursWriter, TenantSubdomainPolicy subdomains, ImageStorage images) {
        this.tenants = tenants; this.hours = hours; this.hoursWriter = hoursWriter;
        this.subdomains = subdomains; this.images = images;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsResponse get() {
        Tenant tenant = currentTenant();
        var rows = hours.findAll().stream().sorted(java.util.Comparator.comparing(h -> h.getDayOfWeek()))
                .map(h -> new BusinessSettingsResponse.DayHours(h.getDayOfWeek(), h.getOpensAt(), h.getClosesAt(), h.isClosed()))
                .toList();
        return new BusinessSettingsResponse(tenant.getName(), tenant.getLogoUrl(), tenant.getWhatsappNumber(),
                tenant.getDeliveryFee(), tenant.getMinOrderValue(), tenant.getSubdomain(), rows);
    }

    @Transactional
    public BusinessSettingsResponse update(BusinessSettingsRequest request) {
        validateHours(request);
        Tenant tenant = currentTenant();
        String slug = subdomains.validateAndNormalize(request.subdomain());
        if (!slug.equals(tenant.getSubdomain()) && tenants.existsBySubdomain(slug)) throw new SubdomainAlreadyInUseException();
        tenant.updateBusinessSettings(request.businessName().trim(), request.whatsappNumber().trim(),
                request.deliveryFee(), request.minOrderValue(), slug);
        try { tenants.saveAndFlush(tenant); } catch (DataIntegrityViolationException e) { throw new SubdomainAlreadyInUseException(); }
        hoursWriter.save(request.businessHours().stream()
                .map(h -> new DayHours(h.dayOfWeek(), h.opensAt(), h.closesAt(), h.closed())).toList());
        return get();
    }

    @Transactional
    public BusinessSettingsResponse updateLogo(byte[] content, ImageType type) {
        Tenant tenant = currentTenant();
        String old = tenant.getLogoUrl();
        var stored = images.store(content, type, tenant.getId());
        tenant.setLogoUrl(stored.imageUrl());
        tenants.save(tenant);
        images.delete(old);
        return get();
    }

    private Tenant currentTenant() { return tenants.findById(TenantContext.get()).orElseThrow(); }
    private void validateHours(BusinessSettingsRequest request) {
        var days = new HashSet<Short>();
        request.businessHours().forEach(row -> {
            if (!days.add(row.dayOfWeek())) throw new IllegalArgumentException("Dias da semana duplicados.");
            if (!row.closed() && (row.opensAt() == null || row.closesAt() == null || !row.opensAt().isBefore(row.closesAt())))
                throw new IllegalArgumentException("Horário de funcionamento inválido.");
        });
    }
}
