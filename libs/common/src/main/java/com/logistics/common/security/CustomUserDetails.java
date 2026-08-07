package com.logistics.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {
    private final UUID userId;
    private final String username;
    private Collection<? extends GrantedAuthority> authorities;

    public UUID getId() { return this.userId; }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return null; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public CustomUserDetails(UUID userId, String username, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(UUID userId, String roleStr) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(toRoleAuthority(roleStr))
        );

        return new CustomUserDetails(userId, null, authorities);
    }

    private static String toRoleAuthority(String roleStr) {
        return roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;
    }

    public Set<String> getRoleNames() {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

}
