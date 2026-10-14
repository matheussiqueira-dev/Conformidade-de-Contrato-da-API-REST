package com.swee.ordermanagementspring.api.common;

import java.util.List;

/** Pagina do contrato v4: page comeca em 0 (C-18). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements) {

    public PageResponse {
        content = List.copyOf(content);
    }
}
