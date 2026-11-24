package org.springframework.samples.petclinic.api.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

/** Client HTTP presentant le certificat de la passerelle (mTLS). Sans configuration : client standard. */
public final class MtlsHttpClientFactory {

    private MtlsHttpClientFactory() { }

    public static HttpClient create(String keyStore, String trustStore, String password) {
        if (keyStore == null || keyStore.isBlank()) {
            return HttpClient.create();
        }
        try {
            char[] pw = password.toCharArray();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(load(keyStore, pw), pw);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(load(trustStore, pw));
            SslContext ssl = SslContextBuilder.forClient().keyManager(kmf).trustManager(tmf).build();
            return HttpClient.create().secure(spec -> spec.sslContext(ssl));
        } catch (Exception e) {
            throw new IllegalStateException("Configuration mTLS du gateway impossible", e);
        }
    }

    private static KeyStore load(String path, char[] pw) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(Path.of(path))) {
            store.load(in, pw);
        }
        return store;
    }
}