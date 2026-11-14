package org.springframework.samples.petclinic.api.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class VetsServiceClientTest {

    private static final String REPONSE = """
        [{"id":1,"firstName":"James","lastName":"Carter","nrOfSpecialties":1,
          "specialties":[{"id":1,"name":"radiology"}]}]
        """;

    @Test
    void leClientAppelleLeServiceVetsEtDecodeLaReponse() {
        AtomicReference<String> urlAppelee = new AtomicReference<>();

        WebClient.Builder builder = WebClient.builder()
            .exchangeFunction(request -> {
                urlAppelee.set(request.url().toString());
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(REPONSE)
                    .build());
            });

        List<VetDetails> vets = new VetsServiceClient(builder).getVets().block();

        assertThat(urlAppelee.get()).isEqualTo("http://vets-service/vets");
        assertThat(vets).singleElement().satisfies(vet -> {
            assertThat(vet.lastName()).isEqualTo("Carter");
            assertThat(vet.specialties())
                .extracting(VetDetails.Specialty::name)
                .containsExactly("radiology");
        });
    }
}
