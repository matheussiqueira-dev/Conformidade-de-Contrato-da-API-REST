package com.swee.ordermanagementspring.config;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porta da API: application.properties usa PORT da plataforma (Render) com padrao 8080. As fontes ficam na ordem de
 * precedencia do Spring Boot: linha de comando, variaveis de ambiente, application.properties.
 */
class ServerPortConfigTest {

    private static String port(Map<String, Object> environmentVariables, String... args) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources sources = environment.getPropertySources();
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        sources.addFirst(new ResourcePropertySource("application.properties", new ClassPathResource("application.properties")));
        sources.addFirst(new SystemEnvironmentPropertySource("environment", environmentVariables));
        sources.addFirst(new SimpleCommandLinePropertySource(args));
        return environment.getProperty("server.port");
    }

    @Test
    @DisplayName("sem PORT a API escuta na 8080")
    void defaultsTo8080() throws IOException {
        assertThat(port(Map.of())).isEqualTo("8080");
    }

    @Test
    @DisplayName("PORT da plataforma define a porta")
    void usesPlatformPort() throws IOException {
        assertThat(port(Map.of("PORT", "10000"))).isEqualTo("10000");
    }

    @Test
    @DisplayName("SERVER_PORT explicito vale antes de PORT (Next e API no mesmo ambiente)")
    void explicitServerPortWins() throws IOException {
        assertThat(port(Map.of("PORT", "3000", "SERVER_PORT", "18081"))).isEqualTo("18081");
    }

    @Test
    @DisplayName("--server.port dos scripts de teste vale antes de PORT")
    void commandLineWins() throws IOException {
        assertThat(port(Map.of("PORT", "3000"), "--server.port=18080")).isEqualTo("18080");
    }
}
