package org.springframework.samples.petclinic.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServletSecurityConfiguration {

    @Bean
    SecurityFilterChain petclinicSecurityFilterChain(
            HttpSecurity http,
            @Value("${petclinic.security.allowed-callers:api-gateway}") List<String> allowedCallers,
            @Value("${petclinic.security.mtls-required:false}") boolean mtlsRequired) throws Exception {

        // CSRF désactivé volontairement : API stateless authentifiée par jeton Bearer, sans session
        // ni cookie que le navigateur renverrait automatiquement, donc aucune requête forgée possible.
        // Sonar S4502 est un faux positif documenté (docs/preuves/p05-jwt-rbac/).
        http
            .csrf(csrf -> csrf.disable())                                   // API sans session : jeton Bearer
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new ServiceIdentityFilter(Set.copyOf(allowedCallers), mtlsRequired),
                BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").hasAnyRole("viewer", "staff")
                .requestMatchers(HttpMethod.POST, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.PUT, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.PATCH, "/**").hasRole("staff")
                .requestMatchers(HttpMethod.DELETE, "/**").hasRole("staff")
                .anyRequest().denyAll())                                    // refus par défaut
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter())));
        return http.build();
    }

    private static JwtAuthenticationConverter converter() {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(jwt -> KeycloakRoles.authorities(jwt.getClaimAsMap("realm_access")));
        return c;
    }
}