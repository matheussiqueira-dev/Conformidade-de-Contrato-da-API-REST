package com.swee.ordermanagementspring.security;

import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Areas de tela liberadas pelo servidor a partir das autoridades alcancaveis na hierarquia (RF-17). */
@Component
public class AreaResolver {
    static final List<String> SELLER_AREAS = List.of("VENDER", "VENDAS", "CLIENTES", "MEU_DESEMPENHO");
    static final List<String> MANAGER_AREAS = List.of("GESTAO", "PRODUTOS", "ESTOQUE", "PROMOCOES", "ANALISES");

    private final RoleHierarchy hierarchy;

    public AreaResolver(RoleHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }

    public List<String> areas(Collection<? extends GrantedAuthority> authorities) {
        Set<String> reachable = hierarchy.getReachableGrantedAuthorities(authorities).stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        List<String> areas = new ArrayList<>();
        if (reachable.contains("ROLE_VENDEDOR")) areas.addAll(SELLER_AREAS);
        if (reachable.contains("ROLE_GERENTE")) areas.addAll(MANAGER_AREAS);
        return List.copyOf(areas);
    }
}
