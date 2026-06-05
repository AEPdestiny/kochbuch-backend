package de.htwberlin.webtech.recipe;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class PostgresDevServicesTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.datasource.active", "true",
                "quarkus.hibernate-orm.active", "true",
                "quarkus.datasource.db-kind", "postgresql",
                "quarkus.datasource.devservices.enabled", "true",
                "quarkus.datasource.devservices.db-name", "webtech_test",
                "quarkus.datasource.devservices.username", "postgres",
                "quarkus.datasource.devservices.password", "postgres",
                "quarkus.hibernate-orm.schema-management.strategy", "drop-and-create"
        );
    }
}
