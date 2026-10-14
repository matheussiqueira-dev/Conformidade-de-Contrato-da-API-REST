package com.swee.ordermanagementspring.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Politica de senha nova (4.5, NIST SP 800-63B): 12 a 72 caracteres e no maximo 72 bytes UTF-8 (limite do
 * BCrypt), sem regra de composicao, fora da lista de senhas comuns e sem um unico caractere repetido.
 * A comparacao com o e-mail fica no DTO ou no servico, que conhecem o e-mail. Violacao: 400 VALIDATION_FAILED.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "must have 12 to 72 characters and must not be a common password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
