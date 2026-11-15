package org.springframework.samples.petclinic.vets.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.security.ServletSecurityConfiguration;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtres ACTIFS : la chaîne de sécurité est réellement exercée sur /vets.
 */
@WebMvcTest(controllers = VetResource.class, properties = "petclinic.security.mtls-required=false")
@Import(ServletSecurityConfiguration.class)
class VetSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VetRepository vetRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void sansJetonLectureRefusee() throws Exception {
        mvc.perform(get("/vets")).andExpect(status().isUnauthorized());
    }

    @Test
    void avecRoleViewerLectureAutorisee() throws Exception {
        given(vetRepository.findAll()).willReturn(List.of());

        mvc.perform(get("/vets").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer"))))
            .andExpect(status().isOk());
    }
}
