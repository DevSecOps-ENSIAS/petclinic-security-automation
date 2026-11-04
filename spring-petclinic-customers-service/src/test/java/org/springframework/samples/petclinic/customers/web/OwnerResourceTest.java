package org.springframework.samples.petclinic.customers.web;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.samples.petclinic.customers.model.Owner;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerResource.class)
class OwnerResourceTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private OwnerEntityMapper ownerEntityMapper;

    @Test
    void returnsAllOwners() throws Exception {
        Owner owner = new Owner();
        owner.setFirstName("George");
        owner.setLastName("Franklin");
        owner.setCity("Madison");

        given(ownerRepository.findAll()).willReturn(List.of(owner));

        mvc.perform(get("/owners"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].firstName").value("George"))
            .andExpect(jsonPath("$[0].city").value("Madison"));
    }

    @Test
    void returnsSingleOwner() throws Exception {
        Owner owner = new Owner();
        owner.setFirstName("Betty");

        given(ownerRepository.findById(1)).willReturn(Optional.of(owner));

        mvc.perform(get("/owners/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Betty"));
    }

    @Test
    void returnsEmptyBodyForUnknownOwner() throws Exception {
        given(ownerRepository.findById(999)).willReturn(Optional.empty());

        mvc.perform(get("/owners/999"))
            .andExpect(status().isOk());
    }
}
