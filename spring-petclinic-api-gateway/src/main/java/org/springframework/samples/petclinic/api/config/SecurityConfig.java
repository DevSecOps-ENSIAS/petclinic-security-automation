package org.springframework.samples.petclinic.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.samples.petclinic.security.KeycloakRoles;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(a -> a
                .pathMatchers("/actuator/health/**", "/fallback").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/**").hasAnyRole("viewer", "staff")
                .pathMatchers("/api/**").hasRole("staff")                   // POST, PUT, DELETE…
                .pathMatchers(HttpMethod.GET, "/**").permitAll()            // fichiers statiques du front
                .anyExchange().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(keycloakRoleConverter())))
            .build();
    }

    /** Traduit realm_access.roles (Keycloak) en autorités ROLE_xxx. */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakRoleConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt ->
            new JwtAuthenticationToken(jwt, KeycloakRoles.authorities(jwt.getClaimAsMap("realm_access"))));
    }
}