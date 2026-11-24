package org.springframework.samples.petclinic.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.samples.petclinic.api.ApiGatewayApplication;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MtlsHttpClientFactoryTest {

    private static final String TEST_PASSWORD = "test-password";

    @TempDir
    Path tempDir;

    @Test
    void createsStandardClientWhenKeyStoreIsNull() {
        assertNotNull(MtlsHttpClientFactory.create(null, "", ""));
    }

    @Test
    void buildsGatewayWebClientWhenKeyStoreIsBlank() {
        var builder = new ApiGatewayApplication()
            .loadBalancedWebClientBuilder(" ", "", "");

        assertNotNull(builder.build());
    }

    @Test
    void createsClientWithConfiguredPkcs12Stores() throws Exception {
        Path keyStore = writePkcs12("gateway.p12");
        Path trustStore = writePkcs12("truststore.p12");

        assertNotNull(MtlsHttpClientFactory.create(
            keyStore.toString(), trustStore.toString(), TEST_PASSWORD));
    }

    @Test
    void wrapsIoErrorWhenTrustStoreIsMissing() throws Exception {
        Path keyStore = writePkcs12("gateway.p12");
        Path missingTrustStore = tempDir.resolve("missing.p12");

        var error = assertThrows(IllegalStateException.class,
            () -> MtlsHttpClientFactory.create(
                keyStore.toString(), missingTrustStore.toString(), TEST_PASSWORD));

        assertInstanceOf(IOException.class, error.getCause());
    }

    private Path writePkcs12(String fileName) throws Exception {
        char[] password = TEST_PASSWORD.toCharArray();
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, password);

        Path path = tempDir.resolve(fileName);
        try (OutputStream output = Files.newOutputStream(path)) {
            store.store(output, password);
        }
        return path;
    }
}