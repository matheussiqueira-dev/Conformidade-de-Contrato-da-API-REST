package com.swee.ordermanagementspring.config;

import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;

@Configuration
public class OpenApiConfig {

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
