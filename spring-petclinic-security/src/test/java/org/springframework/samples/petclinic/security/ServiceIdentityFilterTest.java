package org.springframework.samples.petclinic.security;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceIdentityFilterTest {

    private static final String ATTRIBUT_CERTIFICATS = "jakarta.servlet.request.X509Certificate";

    private final ServiceIdentityFilter sansMtls = new ServiceIdentityFilter(Set.of("api-gateway"), false);
    private final ServiceIdentityFilter mtlsObligatoire = new ServiceIdentityFilter(Set.of("api-gateway"), true);

    @Test
    void laSondeDeSanteNestJamaisFiltree() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mtlsObligatoire.doFilter(new MockHttpServletRequest("GET", "/actuator/health/liveness"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void sansCertificateEtSansMtlsLaRequetePasse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        sansMtls.doFilter(new MockHttpServletRequest("GET", "/owners"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void sansCertificateQuandMtlsEstObligatoireRefus401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mtlsObligatoire.doFilter(new MockHttpServletRequest("GET", "/owners"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void identiteDeServiceAutoriseePasse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mtlsObligatoire.doFilter(requeteAvecCertificat("CN=api-gateway,OU=petclinic"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void identiteDeServiceInconnueRefusee403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mtlsObligatoire.doFilter(requeteAvecCertificat("CN=service-intrus,OU=ailleurs"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void certificatSansCnRefuse403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        mtlsObligatoire.doFilter(requeteAvecCertificat("OU=petclinic"), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    private static MockHttpServletRequest requeteAvecCertificat(String sujet) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/owners");
        X509Certificate certificat = mock(X509Certificate.class);
        when(certificat.getSubjectX500Principal()).thenReturn(new X500Principal(sujet));
        request.setAttribute(ATTRIBUT_CERTIFICATS, new X509Certificate[] { certificat });
        return request;
    }
}
