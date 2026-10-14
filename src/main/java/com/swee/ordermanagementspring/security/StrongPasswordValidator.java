package com.swee.ordermanagementspring.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Valida {@link StrongPassword}. A lista fica em security/common-passwords.txt (uma senha por linha, # comenta). */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;
    private static final Set<String> COMMON = load();

    private static Set<String> load() {
        try (InputStream in = StrongPasswordValidator.class.getResourceAsStream("/security/common-passwords.txt")) {
            if (in == null) throw new IllegalStateException("security/common-passwords.txt not found");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(line -> line.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Senha aceita pela politica (sem a regra do e-mail). Null fica para @NotNull. */
    public static boolean acceptable(String password) {
        if (password == null) return true;
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) return false;
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_LENGTH) return false;
        if (password.codePoints().distinct().count() == 1) return false;
        return !COMMON.contains(password.toLowerCase(Locale.ROOT));
    }

    /** Senha igual ao e-mail (ignorando caixa e espacos nas pontas). */
    public static boolean sameAsEmail(String password, String email) {
        return password != null && email != null
                && password.trim().equalsIgnoreCase(email.trim());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return acceptable(value);
    }
}
