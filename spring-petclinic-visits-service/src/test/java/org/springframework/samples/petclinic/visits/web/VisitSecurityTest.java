package org.springframework.samples.petclinic.visits.web;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.security.ServletSecurityConfiguration;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filtres ACTIFS : la chaîne de sécurité est réellement exercée sur les visites.
 */
@WebMvcTest(controllers = VisitResource.class, properties = "petclinic.security.mtls-required=false")
@Import(ServletSecurityConfiguration.class)
class VisitSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private VisitRepository visitRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void sansJetonLectureRefusee() throws Exception {
        mvc.perform(get("/owners/1/pets/1/visits")).andExpect(status().isUnauthorized());
    }

    @Test
    void avecRoleViewerLectureAutorisee() throws Exception {
        given(visitRepository.findByPetId(1)).willReturn(List.of());

        mvc.perform(get("/owners/1/pets/1/visits").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer"))))
            .andExpect(status().isOk());
    }

    @Test
    void avecRoleViewerEcritureRefusee() throws Exception {
        mvc.perform(post("/owners/1/pets/1/visits")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_viewer")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Controle de securite\"}"))
            .andExpect(status().isForbidden());
    }
}
