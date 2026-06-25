package de.htwberlin.webtech.database;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(FlywayMigrationTest.FlywayProfile.class)
class FlywayMigrationTest {

    @Inject
    AgroalDataSource dataSource;

    @Test
    void flyway_should_create_core_tables_and_schema_history() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(tableExists(connection, "flyway_schema_history"));
            assertTrue(tableExists(connection, "app_user"));
            assertTrue(tableExists(connection, "recipe"));
            assertTrue(tableExists(connection, "mealplan"));
            assertTrue(tableExists(connection, "pantryitem"));
            assertTrue(tableExists(connection, "shoppinglistitem"));
            assertTrue(tableExists(connection, "user_preferences"));
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (var statement = connection.prepareStatement("""
                select count(*)
                from information_schema.tables
                where table_schema = current_schema()
                  and table_name = ?
                """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public static class FlywayProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    Map.entry("quarkus.datasource.active", "true"),
                    Map.entry("quarkus.hibernate-orm.active", "true"),
                    Map.entry("quarkus.datasource.db-kind", "postgresql"),
                    Map.entry("quarkus.datasource.devservices.enabled", "true"),
                    Map.entry("quarkus.datasource.devservices.db-name", "webtech_flyway_test"),
                    Map.entry("quarkus.datasource.devservices.username", "postgres"),
                    Map.entry("quarkus.datasource.devservices.password", "postgres"),
                    Map.entry("quarkus.flyway.enabled", "true"),
                    Map.entry("quarkus.flyway.migrate-at-start", "true"),
                    Map.entry("quarkus.flyway.clean-disabled", "true"),
                    Map.entry("quarkus.hibernate-orm.schema-management.strategy", "validate")
            );
        }
    }
}
