package org.springframework.samples.petclinic.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Traduit le claim Keycloak realm_access.roles en autorités Spring ROLE_xxx. */
public final class KeycloakRoles {

    private KeycloakRoles() { }

    public static Collection<GrantedAuthority> authorities(Map<String, Object> realmAccess) {
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
            return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        }
        return List.of();
    }
}