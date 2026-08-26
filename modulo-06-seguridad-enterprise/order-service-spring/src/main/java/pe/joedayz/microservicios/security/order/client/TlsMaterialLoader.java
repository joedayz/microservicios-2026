package pe.joedayz.microservicios.security.order.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.stereotype.Component;

@Component
public class TlsMaterialLoader {

    // Esta pieza existe para mostrar, de forma explícita, cómo un servicio arma su identidad TLS.
    public SSLContext createSslContext(String keyStorePath,
                                       String keyStorePassword,
                                       String trustStorePath,
                                       String trustStorePassword) {
        try {
            KeyStore keyStore = loadStore(keyStorePath, keyStorePassword);
            KeyStore trustStore = loadStore(trustStorePath, trustStorePassword);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cargar el material TLS del modulo 6", ex);
        }
    }

    private KeyStore loadStore(String storePath, String storePassword) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = Files.newInputStream(Path.of(storePath))) {
            keyStore.load(inputStream, storePassword.toCharArray());
            return keyStore;
        }
    }
}
