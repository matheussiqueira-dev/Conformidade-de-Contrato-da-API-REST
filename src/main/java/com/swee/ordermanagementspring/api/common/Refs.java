package com.swee.ordermanagementspring.api.common;

/** Referencias pequenas do contrato v4. */
public final class Refs {
    private Refs() {
    }

    public record UserRef(long id, String name) { }

    public record NamedRef(String name) { }

    public record CategoryRef(long id, String name) { }

    public record ProductRef(long id, String sku, String name) { }

    /** Escapa curinga do LIKE para busca por prefixo do nome. */
    public static String prefix(String q) {
        return q.toLowerCase(java.util.Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    public static int offset(int page, int size) {
        return Math.multiplyExact(page, size);
    }
}
