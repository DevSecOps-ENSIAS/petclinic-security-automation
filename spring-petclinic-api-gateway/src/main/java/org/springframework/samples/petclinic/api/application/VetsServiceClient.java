package org.springframework.samples.petclinic.api.application;

import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class VetsServiceClient {

    private final WebClient.Builder webClientBuilder;

    public VetsServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<List<VetDetails>> getVets() {
        return webClientBuilder.build()
            .get()
            .uri("http://vets-service/vets")
            .retrieve()
            .bodyToFlux(VetDetails.class)
            .collectList();
    }
}