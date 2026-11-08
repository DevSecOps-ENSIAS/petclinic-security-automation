package org.springframework.samples.petclinic.visits.web;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Size;

/**
 * DTO d'entrée pour la création d'une visite.
 *
 * <p>Seuls les champs que le client a le droit de fournir sont exposés : l'identifiant technique
 * et le {@code petId} restent contrôlés par le serveur. Cela corrige la vulnérabilité de
 * <em>mass assignment</em> détectée par SonarQube (règle {@code java:S4684}) lorsque l'entité JPA
 * {@link org.springframework.samples.petclinic.visits.model.Visit} était liée directement au corps
 * de la requête.</p>
 */
public record VisitRequest(
    @Size(max = 8192) String description,
    @JsonFormat(pattern = "yyyy-MM-dd") Date date
) {
}
