package org.delcom.ifs25009springbootcashflow.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Membuat DataSource: memakai database utama (PostgreSQL) bila bisa dihubungi,
 * jika tidak otomatis beralih ke database cadangan (H2 file) agar aplikasi tetap hidup.
 */
public final class DataSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DataSourceFactory.class);

    private DataSourceFactory() {
    }

    public static DataSource create(String url, String username, String password,
                                    String fallbackUrl, long connectionTimeoutMs) {
        HikariDataSource primary = build(url, username, password, connectionTimeoutMs);
        try {
            primary.getConnection().close();
            return primary;
        } catch (SQLException | RuntimeException ex) {
            LOG.warn("Database utama tidak dapat dihubungi ({}). Beralih ke database cadangan H2.",
                    ex.getMessage());
            primary.close();
            return build(fallbackUrl, "sa", "", connectionTimeoutMs);
        }
    }

    private static HikariDataSource build(String url, String username, String password, long timeoutMs) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setConnectionTimeout(timeoutMs);
        return dataSource;
    }
}
