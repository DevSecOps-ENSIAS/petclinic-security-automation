package org.springframework.samples.petclinic.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesTest {

    @Test
    void sansClaimRealmAccessAucuneAutorite() {
        assertThat(KeycloakRoles.authorities(null)).isEmpty();
    }

    @Test
    void realmAccessSansRolesAucuneAutorite() {
        assertThat(KeycloakRoles.authorities(Map.of("autre", "valeur"))).isEmpty();
    }

    @Test
    void listeDeRolesVideAucuneAutorite() {
        assertThat(KeycloakRoles.authorities(Map.of("roles", List.of()))).isEmpty();
    }

    @Test
    void chaqueRoleDevientUneAutoriteROLE() {
        Collection<GrantedAuthority> authorities =
            KeycloakRoles.authorities(Map.of("roles", List.of("viewer", "staff")));

        assertThat(authorities)
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_viewer", "ROLE_staff");
    }
}
