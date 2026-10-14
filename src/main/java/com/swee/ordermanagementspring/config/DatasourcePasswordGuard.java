package com.swee.ordermanagementspring.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * D021: sem senha do banco a aplicacao para antes de criar qualquer bean, com mensagem clara.
 * O binder do Spring Boot mantem um placeholder nao resolvido como texto literal, o que antes
 * aparecia so como falha de autenticacao no PostgreSQL.
 */
@Component
public class DatasourcePasswordGuard implements BeanFactoryPostProcessor, EnvironmentAware {

    static final String PROPERTY = "spring.datasource.password";

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        check(environment);
    }

    static void check(Environment environment) {
        String password;
        try {
            password = environment.getProperty(PROPERTY);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            password = null;
        }
        if (password == null || password.isBlank() || password.contains("${")) {
            throw new IllegalStateException("Senha do banco ausente: defina SPRING_DATASOURCE_PASSWORD (banco de desenvolvimento) "
                    + "ou TEST_DB_PASSWORD (perfil integration). Os runners scripts/test.ps1 e scripts/test-contract.ps1 geram a de teste.");
        }
    }
}