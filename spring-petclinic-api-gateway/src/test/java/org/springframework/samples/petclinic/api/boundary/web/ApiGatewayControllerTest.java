package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.config.SecurityConfig;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.VisitDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@WebFluxTest(controllers = ApiGatewayController.class)
@Import({ReactiveResilience4JAutoConfiguration.class, CircuitBreakerConfiguration.class, SecurityConfig.class})
class ApiGatewayControllerTest {

    /** Jeton accepté par le décodeur simulé : rôle staff, donc lecture et écriture autorisées. */
    private static final String JETON = "jeton-test";

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    private CustomersServiceClient customersServiceClient;

    @MockitoBean
    private VisitsServiceClient visitsServiceClient;

    @Autowired
    private WebTestClient client;

    /**
     * Le gateway n'accepte plus aucune requête anonyme : chaque appel porte un jeton,
     * et le décodeur simulé le traduit en jeton Keycloak porteur du rôle attendu.
     */
    @BeforeEach
    void authentifierLeDecodeur() {
        Mockito.when(jwtDecoder.decode(JETON)).thenReturn(Mono.just(
            Jwt.withTokenValue(JETON)
                .header("alg", "none")
                .subject("alice")
                .claim("realm_access", Map.of("roles", List.of("staff")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()));
    }

    private WebTestClient withUser() {
        return client.mutate()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + JETON)
            .build();
    }

    @Test
    void getOwnerDetails_withAvailableVisitsService() {
        PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
            .id(20)
            .name("Garfield")
            .visits(new ArrayList<>())
            .build();
        OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
            .pets(List.of(cat))
            .build();
        Mockito
            .when(customersServiceClient.getOwner(1))
            .thenReturn(Mono.just(owner));

        VisitDetails visit = new VisitDetails(300, cat.id(), null, "First visit");
        Visits visits = new Visits(List.of(visit));
        Mockito
            .when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.id())))
            .thenReturn(Mono.just(visits));

        withUser().get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].name").isEqualTo("Garfield")
            .jsonPath("$.pets[0].visits[0].description").isEqualTo("First visit");
    }

    @Test
    void getOwnerDetails_withNoPets() {
        OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
            .pets(Collections.emptyList())
            .build();
        Mockito
            .when(customersServiceClient.getOwner(1))
            .thenReturn(Mono.just(owner));

        withUser().get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets").isEmpty();

        Mockito.verify(visitsServiceClient, Mockito.never()).getVisitsForPets(Mockito.any());
    }

    /**
     * Test Resilience4j fallback method
     */
    @Test
    void getOwnerDetails_withServiceError() {
        PetDetails cat = PetDetails.PetDetailsBuilder.aPetDetails()
            .id(20)
            .name("Garfield")
            .visits(new ArrayList<>())
            .build();
        OwnerDetails owner = OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
            .pets(List.of(cat))
            .build();
        Mockito
            .when(customersServiceClient.getOwner(1))
            .thenReturn(Mono.just(owner));

        Mockito
            .when(visitsServiceClient.getVisitsForPets(Collections.singletonList(cat.id())))
            .thenReturn(Mono.error(new ConnectException("Simulate error")));

        withUser().get()
            .uri("/api/gateway/owners/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.pets[0].name").isEqualTo("Garfield")
            .jsonPath("$.pets[0].visits").isEmpty();
    }

}
