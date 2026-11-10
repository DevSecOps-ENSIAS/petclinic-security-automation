/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.discovery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protège l'annuaire Eureka : les clients doivent s'authentifier en HTTP Basic pour lire ou publier
 * le registre. Le jeton CSRF est ignoré sur /eureka/** car les clients Eureka parlent par requêtes
 * REST (verbes POST, PUT, DELETE) sans formulaire, et la chaîne de filtres est sans session : sans
 * cookie de session, il n'existe pas de requête « ambiante » à rejouer depuis un navigateur.
 *
 * <p>La règle Sonar {@code java:S4502} est neutralisée volontairement : elle vise les applications
 * à session et à cookie, ce que cette chaîne de filtres n'est plus.
 */
@Configuration
@EnableWebSecurity
@SuppressWarnings("java:S4502")
public class EurekaSecurityConfig {

	@Bean
	SecurityFilterChain eurekaSecurityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.ignoringRequestMatchers("/eureka/**"))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(requests -> requests
				.requestMatchers("/actuator/health", "/actuator/info")
				.permitAll()
				.anyRequest()
				.authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

}
