package com.comanda.platform.web;

import com.comanda.menu.images.LocalDiskImageStorage;
import com.comanda.platform.tenancy.TenantFilterInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantFilterInterceptor tenantFilterInterceptor;
    private final LocalDiskImageStorage imageStorage;

    public WebMvcConfig(TenantFilterInterceptor tenantFilterInterceptor, LocalDiskImageStorage imageStorage) {
        this.tenantFilterInterceptor = tenantFilterInterceptor;
        this.imageStorage = imageStorage;
    }

    /**
     * Ordered last (Spring Boot's OSIV interceptor registers with the default order, 0): the
     * Hibernate filter must be enabled on the EntityManager Open-Session-In-View actually binds
     * to the request, not on a throw-away one obtained before OSIV opens it — enabling it too
     * early silently no-ops the filter and leaks cross-tenant rows by id.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantFilterInterceptor).addPathPatterns("/api/**").order(Ordered.LOWEST_PRECEDENCE);
    }

    /**
     * Serves product photos stored on local disk (`menu-management` PRD 7.3); {@code /media/**}
     * doesn't start with {@code /api/} so {@code TenantResolutionFilter} never touches it.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + imageStorage.getBaseDir() + "/");
    }
}
