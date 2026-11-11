package org.springframework.samples.petclinic.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie la chaîne de sécurité servlet réellement produite par le module :
 * 401 sans jeton, 403 sans le bon rôle, 200 avec le bon rôle, santé ouverte.
 */
@SpringJUnitWebConfig({ ServletSecurityConfiguration.class, ServletSecurityConfigurationTest.Support.class })
class ServletSecurityConfigurationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sansJetonLaLectureEstRefusee() throws Exception {
        mvc.perform(get("/owners")).andExpect(status().isUnauthorized());
    }

    @Test
    void leRoleViewerOuvreLaLecture() throws Exception {
        mvc.perform(get("/owners").header("Authorization", "Bearer jeton-viewer"))
            .andExpect(status().isOk())
            .andExpect(content().string("[]"));
    }

    @Test
    void leRoleViewerNouvrePasLEcriture() throws Exception {
        mvc.perform(post("/owners").header("Authorization", "Bearer jeton-viewer"))
            .andExpect(status().isForbidden());
    }

    @Test
    void leRoleStaffOuvreLEcriture() throws Exception {
        mvc.perform(post("/owners").header("Authorization", "Bearer jeton-staff"))
            .andExpect(status().isOk())
            .andExpect(content().string("cree"));
    }

    @Test
    void unJetonSansRoleEstRefuse() throws Exception {
        mvc.perform(get("/owners").header("Authorization", "Bearer jeton-sans-role"))
            .andExpect(status().isForbidden());
    }

    @Test
    void laSondeDeSanteResteOuverte() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().string("ok"));
    }

    @Configuration
    @EnableWebMvc
    static class Support {

        @Bean
        JwtDecoder jwtDecoder() {
            return ServletSecurityConfigurationTest::jetonPour;
        }

        @Bean
        Endpoints endpoints() {
            return new Endpoints();
        }
    }

    @RestController
    static class Endpoints {

        @GetMapping("/owners")
        String lire() {
            return "[]";
        }

        @PostMapping("/owners")
        String creer() {
            return "cree";
        }

        @GetMapping("/actuator/health")
        String sante() {
            return "ok";
        }
    }

    private static Jwt jetonPour(String token) {
        List<String> roles = switch (token) {
            case "jeton-viewer" -> List.of("viewer");
            case "jeton-staff" -> List.of("staff");
            case "jeton-sans-role" -> List.of();
            default -> throw new JwtException("jeton inconnu");
        };
        return Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject("test")
            .claim("realm_access", Map.of("roles", roles))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }
}
