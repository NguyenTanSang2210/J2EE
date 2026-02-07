package nhom2.NguyenTanSang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình SePay Payment Gateway
 * Load từ application.properties với prefix "sepay"
 */
@Configuration
@ConfigurationProperties(prefix = "sepay")
@Data
public class SePayConfig {
    private Api api = new Api();
    private Account account = new Account();
    private Bank bank = new Bank();

    @Data
    public static class Api {
        private String url;
        private String token;
    }

    @Data
    public static class Account {
        private String number;
        private String name;
    }

    @Data
    public static class Bank {
        private String code;
    }
}
