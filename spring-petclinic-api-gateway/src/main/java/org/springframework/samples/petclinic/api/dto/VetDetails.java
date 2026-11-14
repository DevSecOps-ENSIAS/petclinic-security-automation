package org.springframework.samples.petclinic.api.dto;

import java.util.List;

public record VetDetails(
    int id,
    String firstName,
    String lastName,
    int nrOfSpecialties,
    List<Specialty> specialties) {

    public VetDetails {
        if (specialties == null) {
            specialties = List.of();
        }
    }

    public record Specialty(int id, String name) {
    }
}
