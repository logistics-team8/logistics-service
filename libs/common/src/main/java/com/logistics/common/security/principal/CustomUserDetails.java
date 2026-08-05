package com.logistics.common.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {
    private final UUID userId;
    private final String username;
    private final UUID hubId;
    private final UUID companyId;
    private Collection<? extends GrantedAuthority> authorities;

    public UUID getId() { return this.userId; }
    @Override public String getUsername() { return username; }
    public UUID getHubId() { return this.hubId; }
    public UUID getCompanyId() { return this.companyId; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public CustomUserDetails(
            UUID userId,
            String username,
            UUID hubId,
            UUID companyId,
            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.hubId = hubId;
        this.companyId = companyId;
        this.authorities = authorities;
    }

    public static CustomUserDetails from(UUID userId, UUID hubId, UUID companyId, String roleStr) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + roleStr)
        );

        return new CustomUserDetails(userId, null, hubId, companyId, authorities);
    }

    @Override public String getPassword() { return null; }
}
