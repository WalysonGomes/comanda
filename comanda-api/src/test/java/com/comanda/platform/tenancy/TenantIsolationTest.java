package com.comanda.platform.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.comanda.ComandaApiApplication;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PRD Regra 9 / spec `multi-tenancy`: tenant A never reads or modifies a resource of tenant B; a
 * resource of another tenant is treated as inexistent. Exercises the real {@link
 * TenantScopedEntity} filter (D1) against the actual {@code customers} MVP table, enabling the
 * filter exactly as {@link TenantFilterInterceptor} does for a real request.
 */
@Testcontainers
@SpringBootTest(classes = ComandaApiApplication.class)
@Transactional
class TenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestCustomerRepository customerRepository;

    @Autowired
    private EntityManager entityManager;

    private Long tenantAId;
    private Long customerOfBId;

    @BeforeEach
    void seedTwoTenantsAndEnableFilterForTenantA() {
        tenantAId = insertTenant("tenant-a");
        Long tenantBId = insertTenant("tenant-b");

        insertCustomer(tenantAId, "Cliente A");
        customerOfBId = insertCustomer(tenantBId, "Cliente B");

        entityManager.unwrap(Session.class)
                .enableFilter(TenantScopedEntity.FILTER_NAME)
                .setParameter("tenantId", tenantAId);
    }

    @Test
    void tenantACannotReadCustomerOfTenantB() {
        Optional<TestCustomer> found = customerRepository.findById(customerOfBId);

        assertThat(found).isEmpty();
    }

    @Test
    void tenantAOnlySeesItsOwnCustomers() {
        List<TestCustomer> all = customerRepository.findAll();

        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(c -> c.getTenantId().equals(tenantAId));
    }

    private Long insertTenant(String subdomain) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO tenants (subdomain, name) VALUES (?, ?) RETURNING id",
                Long.class, subdomain, subdomain);
    }

    private Long insertCustomer(Long tenantId, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO customers (tenant_id, name, phone) VALUES (?, ?, ?) RETURNING id",
                Long.class, tenantId, name, "85999990000");
    }
}
