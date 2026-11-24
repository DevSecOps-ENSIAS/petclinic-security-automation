package org.springframework.samples.petclinic.customers.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.customers.model.OwnerRepository;
import org.springframework.samples.petclinic.customers.web.mapper.OwnerEntityMapper;
import org.springframework.samples.petclinic.security.ServletSecurityConfiguration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OwnerResource.class, properties = "petclinic.security.mtls-required=false")
@Import(ServletSecurityConfiguration.class)
class OwnerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private OwnerEntityMapper ownerEntityMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void sansJetonLectureRefusee() throws Exception {
        mvc.perform(get("/owners")).andExpect(status().isUnauthorized());
    }

    @Test
    void avecRoleViewerLectureAutorisee() throws Exception {
        given(ownerRepository.findAll()).willReturn(List.of());

        mvc.perform(get("/owners").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer"))))
            .andExpect(status().isOk());
    }

    @Test
    void avecRoleViewerEcritureRefusee() throws Exception {
        mvc.perform(post("/owners")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Smoke\",\"lastName\":\"Test\",\"address\":\"1 rue\",\"city\":\"Paris\",\"telephone\":\"0102030405\"}"))
            .andExpect(status().isForbidden());
    }
}
