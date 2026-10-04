package com.swee.ordermanagementspring.config;

import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.Components;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer sessionSecurityDocumentation() {
        return openApi -> {
            if (openApi.getComponents() == null) openApi.setComponents(new Components());
            openApi.getComponents().addSecuritySchemes("sessionAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("JSESSIONID"));
            // Logout is handled by Spring Security rather than an MVC controller.
            var logout = new Operation().operationId("logout").summary("Invalida sessao e CSRF")
                    .responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                            .addApiResponse("204", new ApiResponse().description("Sessao encerrada")));
            openApi.getPaths().addPathItem("/auth/logout", new PathItem().post(logout));
            openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
                boolean publicAuth = List.of("/auth/login", "/auth/csrf", "/auth/logout").contains(path);
                operation.setSecurity(publicAuth ? List.of() : List.of(new SecurityRequirement().addList("sessionAuth")));
                if (List.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT, PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE).contains(method)) {
                    operation.addParametersItem(new HeaderParameter().name("X-CSRF-TOKEN").required(true)
                            .description("Token retornado por GET /auth/csrf")
                            .schema(new StringSchema()));
                    operation.getResponses().addApiResponse("403", new ApiResponse().description("Perfil sem permissao ou token CSRF invalido"));
                }
                if (!publicAuth) operation.getResponses().addApiResponse("401", new ApiResponse().description("Sessao ausente ou expirada"));
            }));
        };
    }

    @Bean
    public OpenApiCustomizer errorDetailsSchema() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
                return;
            }
            var errorSchema = openApi.getComponents().getSchemas().get("ApiErrorResponse");
            if (errorSchema != null && errorSchema.getProperties() != null) {
                // The array annotation resolver drops the null type in this Springdoc version.
                // Express the actual nullable response using an OpenAPI 3.1 JSON Schema.
                var details = new JsonSchema();
                details.setTypes(new LinkedHashSet<>(List.of("array", "null")));
                details.setItems(new StringSchema());
                errorSchema.addProperty("details", details);
            }
        };
    }
}
