package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.samples.petclinic.api.application.VetsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDossier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/gateway")
public class DossierController {

    private final ApiGatewayController owners;

    private final VetsServiceClient vets;

    public DossierController(ApiGatewayController owners, VetsServiceClient vets) {
        this.owners = owners;
        this.vets = vets;
    }

    @GetMapping("owners/{ownerId}/dossier")
    public Mono<OwnerDossier> dossier(@PathVariable int ownerId) {
        return Mono.zip(owners.getOwnerDetails(ownerId), vets.getVets(), OwnerDossier::new);
    }
}
