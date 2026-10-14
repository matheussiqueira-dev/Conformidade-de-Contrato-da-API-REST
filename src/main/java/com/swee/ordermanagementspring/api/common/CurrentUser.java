package com.swee.ordermanagementspring.api.common;

import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;

/** Usuario da sessao visto pelos casos de uso: id e se e GERENTE (que herda tudo do VENDEDOR). */
public record CurrentUser(long id, String name, boolean manager) {

    public static CurrentUser of(AppPrincipal principal) {
        return new CurrentUser(principal.id(), principal.name(), principal.role() == UserRole.GERENTE);
    }

    /** Terceira barreira: o VENDEDOR so ve o proprio objeto; o GERENTE ve todos. */
    public boolean owns(Long sellerId) {
        return manager || (sellerId != null && sellerId == id);
    }
}
