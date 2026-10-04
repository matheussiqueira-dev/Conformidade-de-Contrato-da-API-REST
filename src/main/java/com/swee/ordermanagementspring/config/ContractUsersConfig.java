package com.swee.ordermanagementspring.config;

import com.swee.ordermanagementspring.entities.auth.*;
import com.swee.ordermanagementspring.repositories.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration @Profile("contract")
public class ContractUsersConfig {
    @Bean CommandLineRunner contractUsers(AppUserRepository users, PasswordEncoder encoder, Environment env) {
        return args -> {
            if (!"1".equals(env.getProperty("A3_CONTRACT_ISOLATED"))
                    || !"create-drop".equals(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                    || !env.getRequiredProperty("spring.datasource.url").equals("jdbc:postgresql://localhost:15432/order_management_test")) {
                throw new IllegalStateException("Contract fixtures require the disposable database runner");
            }
            String password = env.getRequiredProperty("A3_TEST_PASSWORD");
            if (password.length() < 12) throw new IllegalStateException("Test password too short");
            users.save(new AppUser("Gerente sintetico", "manager@example.test", encoder.encode(password), UserRole.GERENTE));
            users.save(new AppUser("Vendedor sintetico", "seller@example.test", encoder.encode(password), UserRole.VENDEDOR));
        };
    }
}
