package com.comanda.platform.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.comanda.ComandaApiApplication;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = ComandaApiApplication.class)
class FlywayMigrationTest {

    private static final Set<String> MVP_TABLES = Set.of(
            "tenants", "users", "business_hours", "categories", "products",
            "additional_groups", "additional_items", "customers", "orders",
            "order_items", "order_item_additionals", "order_status_history", "subscriptions"
    );

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    void migratingAnEmptyDatabaseCreatesExactlyTheMvpTables() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name != 'flyway_schema_history'",
                String.class);

        assertThat(tables).containsExactlyInAnyOrderElementsOf(MVP_TABLES);
    }

    @Test
    void reapplyingMigrationsOnAnAlreadyMigratedDatabaseIsANoop() {
        MigrateResult secondRun = flyway.migrate();

        assertThat(secondRun.migrationsExecuted).isZero();
    }
}
