package org.springframework.samples.petclinic.visits.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void returnsACompleteVisit() throws Exception {
        Visit visit = Visit.VisitBuilder.aVisit()
            .id(5)
            .description("vaccination")
            .petId(2)
            .build();

        given(visitRepository.findByPetId(2)).willReturn(List.of(visit));

        mvc.perform(get("/owners/1/pets/2/visits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(5))
            .andExpect(jsonPath("$[0].description").value("vaccination"))
            .andExpect(jsonPath("$[0].petId").value(2));
    }

    @Test
    void createsAVisitForThePetInThePath() throws Exception {
        given(visitRepository.save(any(Visit.class))).willAnswer(invocation -> {
            Visit saved = invocation.getArgument(0);
            saved.setId(99);
            return saved;
        });

        mvc.perform(post("/owners/1/pets/3/visits")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"checkup","date":"2025-01-15"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(99))
            .andExpect(jsonPath("$.petId").value(3))
            .andExpect(jsonPath("$.description").value("checkup"));
    }
}
