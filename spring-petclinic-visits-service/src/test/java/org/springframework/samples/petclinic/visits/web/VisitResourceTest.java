package org.springframework.samples.petclinic.visits.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VisitResource.class)
class VisitResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VisitRepository visitRepository;

    @Test
    void returnsVisitsOfOnePet() throws Exception {
        Visit visit = new Visit();
        visit.setDescription("rabies shot");

        given(visitRepository.findByPetId(1)).willReturn(List.of(visit));

        mvc.perform(get("/owners/1/pets/1/visits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].description").value("rabies shot"));
    }

    @Test
    void returnsVisitsOfSeveralPets() throws Exception {
        given(visitRepository.findByPetIdIn(List.of(1, 2))).willReturn(List.of());

        mvc.perform(get("/pets/visits").param("petId", "1", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items").isEmpty());
    }
}
