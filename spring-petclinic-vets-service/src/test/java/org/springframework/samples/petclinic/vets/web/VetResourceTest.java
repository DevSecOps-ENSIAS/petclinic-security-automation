package org.springframework.samples.petclinic.vets.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.samples.petclinic.vets.model.Vet;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VetResource.class)
@AutoConfigureMockMvc(addFilters = false)
class VetResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VetRepository vetRepository;

    @Test
    void returnsAllVets() throws Exception {
        Vet vet = new Vet();
        vet.setFirstName("Helen");
        vet.setLastName("Leary");

        given(vetRepository.findAll()).willReturn(List.of(vet));

        mvc.perform(get("/vets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("Helen"))
            .andExpect(jsonPath("$[0].lastName").value("Leary"));
    }

    @Test
    void returnsEmptyArrayWhenNoVetExists() throws Exception {
        given(vetRepository.findAll()).willReturn(List.of());

        mvc.perform(get("/vets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }
}
