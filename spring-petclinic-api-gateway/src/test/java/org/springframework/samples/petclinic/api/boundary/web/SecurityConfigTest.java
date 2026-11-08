package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.samples.petclinic.api.config.SecurityConfig;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> converter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod("keycloakRoleConverter");
        method.setAccessible(true);
        this.converter = (Converter<Jwt, Mono<AbstractAuthenticationToken>>) method.invoke(securityConfig);
    }

    private Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(
            "mock-token-value",
            Instant.now(),
            Instant.now().plusSeconds(300),
            Map.of("alg", "none"),
            claims
        );
    }

    @Test
    void shouldExtractStaffRoleWhenPresent() {
        Jwt jwt = createJwt(Map.of(
            "sub", "alice",
            "realm_access", Map.of("roles", List.of("staff", "user"))
        ));

        AbstractAuthenticationToken auth = converter.convert(jwt).block();
        assertThat(auth).isNotNull();

        Collection<String> authorities = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        assertThat(authorities).contains("ROLE_staff", "ROLE_user");
    }

    @Test
    void shouldNotContainStaffRoleWhenAbsent() {
        Jwt jwt = createJwt(Map.of(
            "sub", "bob",
            "realm_access", Map.of("roles", List.of("user", "guest"))
        ));

        AbstractAuthenticationToken auth = converter.convert(jwt).block();
        assertThat(auth).isNotNull();

        Collection<String> authorities = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        assertThat(authorities)
            .doesNotContain("ROLE_staff")
            .containsExactlyInAnyOrder("ROLE_user", "ROLE_guest");
    }

    @Test
    void shouldHandleNullRealmAccessGracefully() {
        Jwt jwt = createJwt(Map.of("sub", "anonymous"));

        AbstractAuthenticationToken auth = converter.convert(jwt).block();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void shouldHandleNonCollectionRolesGracefully() {
        Jwt jwt = createJwt(Map.of(
            "sub", "invalid",
            "realm_access", Map.of("roles", "not-a-list")
        ));

        AbstractAuthenticationToken auth = converter.convert(jwt).block();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }
}