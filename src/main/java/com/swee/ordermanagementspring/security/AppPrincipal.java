package com.swee.ordermanagementspring.security;

import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import org.springframework.security.core.userdetails.User;

public final class AppPrincipal extends User {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String name;
    private final UserRole role;

    public AppPrincipal(AppUser user) {
        super(user.getEmail(), user.getPasswordHash(),
                User.withUsername(user.getEmail()).password(user.getPasswordHash())
                        .roles(user.getRole().name()).build().getAuthorities());
        this.id = user.getId();
        this.name = user.getName();
        this.role = user.getRole();
    }
    public Long id() { return id; }
    public String name() { return name; }
    public UserRole role() { return role; }
}
