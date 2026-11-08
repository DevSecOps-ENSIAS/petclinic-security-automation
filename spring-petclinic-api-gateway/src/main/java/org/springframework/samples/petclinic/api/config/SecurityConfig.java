package org.springframework.samples.petclinic.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)   // API sans session : jeton Bearer
            .authorizeExchange(auth -> auth
                .pathMatchers("/actuator/health/**", "/actuator/info", "/actuator/prometheus").permitAll()
                .pathMatchers(HttpMethod.GET, "/**").permitAll()          // site + lectures
                .pathMatchers(HttpMethod.POST, "/api/**").hasRole("staff")
                .pathMatchers(HttpMethod.PUT, "/api/**").hasRole("staff")
                .pathMatchers(HttpMethod.DELETE, "/api/**").hasRole("staff")
                .anyExchange().denyAll())                                 // refus par défaut
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                jwt.jwtAuthenticationConverter(keycloakRoleConverter())))
            .build();
    }

    /** Traduit realm_access.roles (Keycloak) en autorités ROLE_xxx. */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> keycloakRoleConverter() {
        return new ReactiveJwtAuthenticationConverterAdapter(jwt -> {
            Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
            Collection<GrantedAuthority> authorities = List.of();
            if (realm != null && realm.get("roles") instanceof Collection<?> roles) {
                authorities = roles.stream()
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            }
            return new JwtAuthenticationToken(jwt, authorities);
        });
    }
}