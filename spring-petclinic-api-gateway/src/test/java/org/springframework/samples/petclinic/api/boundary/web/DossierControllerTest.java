package org.springframework.samples.petclinic.api.boundary.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.samples.petclinic.api.application.VetsServiceClient;
import org.springframework.samples.petclinic.api.config.SecurityConfig;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@WebFluxTest(controllers = DossierController.class)
@Import(SecurityConfig.class)
class DossierControllerTest {

    /** Jeton accepté par le décodeur simulé : rôle staff, donc lecture autorisée. */
    private static final String JETON = "jeton-test";

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    private ApiGatewayController ownersController;

    @MockitoBean
    private VetsServiceClient vetsServiceClient;

    @Autowired
    private WebTestClient client;

    @BeforeEach
    void preparerLesClients() {
        Mockito.when(jwtDecoder.decode(JETON)).thenReturn(Mono.just(
            Jwt.withTokenValue(JETON)
                .header("alg", "none")
                .subject("alice")
                .claim("realm_access", Map.of("roles", List.of("staff")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build()));

        Mockito.when(ownersController.getOwnerDetails(1))
            .thenReturn(Mono.just(OwnerDetails.OwnerDetailsBuilder.anOwnerDetails()
                .id(1)
                .firstName("George")
                .lastName("Franklin")
                .pets(List.of())
                .build()));

        Mockito.when(vetsServiceClient.getVets())
            .thenReturn(Mono.just(List.of(new VetDetails(1, "James", "Carter", 1,
                List.of(new VetDetails.Specialty(1, "radiology"))))));
    }

    private WebTestClient withUser() {
        return client.mutate()
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + JETON)
            .build();
    }

    @Test
    void leDossierAssembleOwnerEtVets() {
        withUser().get()
            .uri("/api/gateway/owners/1/dossier")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.owner.firstName").isEqualTo("George")
            .jsonPath("$.vets[0].lastName").isEqualTo("Carter")
            .jsonPath("$.vets[0].specialties[0].name").isEqualTo("radiology");
    }

    @Test
    void sansJetonLeDossierEstRefuse() {
        client.get()
            .uri("/api/gateway/owners/1/dossier")
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
