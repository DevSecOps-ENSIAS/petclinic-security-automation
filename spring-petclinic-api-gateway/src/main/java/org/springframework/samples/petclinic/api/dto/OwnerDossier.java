package org.springframework.samples.petclinic.api.dto;

import java.util.List;

/**
 * Dossier d'un propriétaire : ses informations et ses animaux (service {@code customers}),
 * leurs visites (service {@code visits}) et les vétérinaires (service {@code vets}).
 */
public record OwnerDossier(OwnerDetails owner, List<VetDetails> vets) {
}
