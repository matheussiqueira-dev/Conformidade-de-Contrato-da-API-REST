package com.swee.ordermanagementspring.entities.customer;

import com.swee.ordermanagementspring.exceptions.CustomerException;

/**
 * N-06: CPF e CNPJ sao gravados so com digitos (ck_customer_cpf, ck_customer_cnpj da V5).
 * Aceita a entrada com ou sem mascara; o digito verificador e conferido nos DTOs novos (@CPF/@CNPJ).
 */
public final class Documents {

    private Documents() {
    }

    /** Remove a mascara; usado pelas entidades. */
    public static String digitsOnly(String raw) {
        return raw == null ? null : raw.replaceAll("[^0-9]", "");
    }

    /** Valida o tamanho do CPF recebido pela API e devolve so os digitos. */
    public static String cpf(String raw) {
        return digits(raw, 11, "CPF");
    }

    /** Valida o tamanho do CNPJ recebido pela API e devolve so os digitos. */
    public static String cnpj(String raw) {
        return digits(raw, 14, "CNPJ");
    }

    private static String digits(String raw, int length, String label) {
        String digits = digitsOnly(raw);
        if (digits == null) {
            return null;
        }
        if (digits.length() != length) {
            throw new CustomerException(label + " must have " + length + " digits");
        }
        return digits;
    }
}
