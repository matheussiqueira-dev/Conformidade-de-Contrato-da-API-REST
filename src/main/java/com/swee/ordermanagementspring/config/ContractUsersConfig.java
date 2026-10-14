package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.entities.auth.*;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Contas sinteticas do runner de contrato. So rodam em banco descartavel: exigem A3_CONTRACT_ISOLATED=1,
 * a propriedade a3.contract.disposable-database=true (passada apenas por scripts/test-contract.ps1 e pela CI)
 * e o PostgreSQL de teste da porta 15432. O banco comeca limpo: as tabelas de negocio sao esvaziadas antes.
 */
@Configuration @Profile("contract")
public class ContractUsersConfig {
    static final String DISPOSABLE_URL = "jdbc:postgresql://localhost:15432/order_management_test";

    @Bean CommandLineRunner contractUsers(AppUserRepository users, PasswordEncoder encoder, Environment env, JdbcTemplate jdbc) {
        return args -> {
            requireDisposableDatabase(env);
            String password = env.getRequiredProperty("A3_TEST_PASSWORD");
            if (password.length() < 12) throw new IllegalStateException("Test password too short");
            BusinessTables.truncate(jdbc);
            users.save(new AppUser("Gerente sintetico", "manager@example.test", encoder.encode(password), UserRole.GERENTE));
            users.save(new AppUser("Vendedor sintetico", "seller@example.test", encoder.encode(password), UserRole.VENDEDOR));
        };
    }

    static void requireDisposableDatabase(Environment env) {
        if (!"1".equals(env.getProperty("A3_CONTRACT_ISOLATED"))
                || !"true".equals(env.getProperty("a3.contract.disposable-database"))
                || !DISPOSABLE_URL.equals(env.getProperty("spring.datasource.url"))) {
            throw new IllegalStateException("Contract fixtures require the disposable database runner");
        }
    }
}
