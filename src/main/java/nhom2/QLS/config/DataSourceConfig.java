package nhom2.QLS.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * DataSource Configuration for Railway MySQL
 * Handles multiple environment variable formats và provides retry logic
 */
@Configuration
@Profile("prod")
@Slf4j
public class DataSourceConfig {

    @Value("${MYSQLHOST:${DB_HOST:localhost}}")
    private String dbHost;

    @Value("${MYSQLPORT:${DB_PORT:3306}}")
    private String dbPort;

    @Value("${MYSQLDATABASE:${DB_NAME:QLS}}")
    private String dbName;

    @Value("${MYSQLUSER:${DB_USERNAME:root}}")
    private String dbUser;

    @Value("${MYSQLPASSWORD:${DB_PASSWORD:}}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String jdbcUrl = String.format(
            "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&autoReconnect=true&failOverReadOnly=false&maxReconnects=10",
            dbHost, dbPort, dbName
        );

        log.info("=".repeat(80));
        log.info("RAILWAY DATABASE CONFIGURATION");
        log.info("=".repeat(80));
        log.info("DB Host: {}", dbHost);
        log.info("DB Port: {}", dbPort);
        log.info("DB Name: {}", dbName);
        log.info("DB User: {}", dbUser);
        log.info("JDBC URL: {}", jdbcUrl);
        log.info("=".repeat(80));

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Connection Pool Settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute

        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // Không fail ngay khi không kết nối được - để Spring Boot có thể start
        config.setInitializationFailTimeout(-1);
        
        // Pool name
        config.setPoolName("QLS-HikariPool-Railway");

        try {
            HikariDataSource dataSource = new HikariDataSource(config);
            log.info("✅ HikariCP DataSource initialized successfully");
            return dataSource;
        } catch (Exception e) {
            log.error("❌ Failed to initialize HikariCP DataSource: {}", e.getMessage());
            log.error("⚠️  Application will start but database operations will fail until connection is established");
            // Vẫn trả về DataSource để app có thể start
            return new HikariDataSource(config);
        }
    }
}
