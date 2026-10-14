package com.swee.ordermanagementspring.api.customers;

/**
 * C-12: o GERENTE recebe o documento formatado completo; o VENDEDOR, mascarado (***.456.789-**, **.345.678/0001-**).
 * O banco guarda so digitos (D3); valor fora do tamanho esperado nunca e exibido.
 */
public final class DocumentFormat {
    private DocumentFormat() {
    }

    public static String of(String cpf, String cnpj, boolean full) {
        if (cpf != null && cpf.length() == 11) {
            return (full ? cpf.substring(0, 3) : "***") + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9)
                    + "-" + (full ? cpf.substring(9) : "**");
        }
        if (cnpj != null && cnpj.length() == 14) {
            return (full ? cnpj.substring(0, 2) : "**") + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8)
                    + "/" + cnpj.substring(8, 12) + "-" + (full ? cnpj.substring(12) : "**");
        }
        throw new IllegalStateException("Customer document is not normalized");
    }

    /** C-11: busca remove pontuacao e espacos; sobrando so digitos, a busca e por documento exato. */
    public static String searchDigits(String q) {
        String stripped = q.replaceAll("[\\s.\\-/()]", "");
        return !stripped.isEmpty() && stripped.chars().allMatch(Character::isDigit) ? stripped : null;
    }
}
