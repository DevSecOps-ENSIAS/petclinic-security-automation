package org.springframework.samples.petclinic.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VetDetailsTest {

    @Test
    void lesSpecialitesAbsentesDeviennentUneListeVide() {
        VetDetails vet = new VetDetails(1, "James", "Carter", 0, null);

        assertThat(vet.specialties()).isEmpty();
    }
}
