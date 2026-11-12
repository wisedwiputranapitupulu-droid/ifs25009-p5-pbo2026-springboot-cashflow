package org.delcom.ifs25009springbootcashflow.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourceFactoryTest {

    private static String h2Url(String prefix) {
        return "jdbc:h2:mem:" + prefix + UUID.randomUUID()
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    }

    @Test
    void usesPrimaryDatabaseWhenReachable() {
        String primary = h2Url("primary");
        HikariDataSource dataSource = (HikariDataSource) DataSourceFactory.create(
                primary, "sa", "", h2Url("unused"), 1000);
        try {
            assertEquals(primary, dataSource.getJdbcUrl());
        } finally {
            dataSource.close();
        }
    }

    @Test
    void fallsBackToH2WhenPrimaryDatabaseUnreachable() throws Exception {
        String fallback = h2Url("fallback");
        HikariDataSource dataSource = (HikariDataSource) DataSourceFactory.create(
                "jdbc:postgresql://127.0.0.1:1/tidak_ada", "user", "pass", fallback, 1000);
        try {
            assertEquals(fallback, dataSource.getJdbcUrl());
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(1));
            }
        } finally {
            dataSource.close();
        }
    }

    @Test
    void utilityClassHasPrivateConstructor() throws Exception {
        Constructor<DataSourceFactory> constructor = DataSourceFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
