package org.springframework.samples.petclinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autorisation par identité de service : après le handshake mTLS, lit le CN du certificat client
 * et ne laisse passer que les appelants de la liste blanche (petclinic.security.allowed-callers).
 */
public class ServiceIdentityFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceIdentityFilter.class);
    private static final Pattern CN = Pattern.compile("CN=([^,]+)");

    private final Set<String> allowedCallers;
    private final boolean mtlsRequired;

    public ServiceIdentityFilter(Set<String> allowedCallers, boolean mtlsRequired) {
        this.allowedCallers = allowedCallers;
        this.mtlsRequired = mtlsRequired;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        X509Certificate[] certs = (X509Certificate[]) req.getAttribute("jakarta.servlet.request.X509Certificate");
        if (certs == null || certs.length == 0) {
            if (mtlsRequired) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);   // certificat client requis
                return;
            }
            chain.doFilter(req, res);
            return;
        }
        Matcher m = CN.matcher(certs[0].getSubjectX500Principal().getName());
        String caller = m.find() ? m.group(1) : "";
        if (!allowedCallers.contains(caller)) {
            LOG.warn("Appelant refuse : CN={}", caller);
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);          // identité valide mais non autorisée
            return;
        }
        LOG.debug("Appelant autorise : CN={}", caller);
        chain.doFilter(req, res);
    }
}