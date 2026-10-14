package com.swee.ordermanagementspring.api.sales;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regra 8: request_hash = SHA-256 do JSON canonico do corpo (chaves ordenadas em todos os niveis, sem espacos;
 * a ordem das listas e preservada). Mesma chave e corpo diferente -> 409 IDEMPOTENCY_CONFLICT.
 */
@Component
public class RequestHash {
    private final ObjectMapper mapper;

    public RequestHash(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String of(Object request) {
        Object tree = mapper.convertValue(request, Object.class);
        return sha256(mapper.writeValueAsString(canonical(tree)));
    }

    /** Mesmo hash para o mesmo JSON escrito com as chaves em outra ordem (CT-SALE-102). */
    public String ofJson(String json) {
        return sha256(mapper.writeValueAsString(canonical(mapper.readValue(json, Object.class))));
    }

    private static Object canonical(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, value) -> sorted.put(String.valueOf(key), canonical(value)));
            return sorted;
        }
        if (node instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            list.forEach(value -> copy.add(canonical(value)));
            return copy;
        }
        return node;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
