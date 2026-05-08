package net.minecraft.server.jsonrpc.security;

import com.mojang.logging.LogUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;

public class JsonRpcSslContextProvider {
    private static final String PASSWORD_ENV_VARIABLE_KEY = "MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD";
    private static final String PASSWORD_SYSTEM_PROPERTY_KEY = "management.tls.keystore.password";
    private static final Logger log = LogUtils.getLogger();

    public static SslContext createFrom(String keystore, String keystorePassword) throws Exception {
        if (keystore.isEmpty()) {
            throw new IllegalArgumentException("TLS is enabled but keystore is not configured");
        } else {
            File file1 = new File(keystore);
            if (file1.exists() && file1.isFile()) {
                String s = getKeystorePassword(keystorePassword);
                return loadKeystoreFromPath(file1, s);
            } else {
                throw new IllegalArgumentException("Supplied keystore is not a file or does not exist: '" + keystore + "'");
            }
        }
    }

    private static String getKeystorePassword(String password) {
        String s = System.getenv().get("MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD");
        if (s != null) {
            return s;
        } else {
            String s1 = System.getProperty("management.tls.keystore.password", null);
            return s1 != null ? s1 : password;
        }
    }

    private static SslContext loadKeystoreFromPath(File file, String password) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");

        try (InputStream inputstream = new FileInputStream(file)) {
            keystore.load(inputstream, password.toCharArray());
        }

        KeyManagerFactory keymanagerfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keymanagerfactory.init(keystore, password.toCharArray());
        TrustManagerFactory trustmanagerfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustmanagerfactory.init(keystore);
        return SslContextBuilder.forServer(keymanagerfactory).trustManager(trustmanagerfactory).build();
    }

    public static void printInstructions() {
        log.info("To use TLS for the management server, please follow these steps:");
        log.info("1. Set the server property 'management-server-tls-enabled' to 'true' to enable TLS");
        log.info("2. Create a keystore file of type PKCS12 containing your server certificate and private key");
        log.info("3. Set the server property 'management-server-tls-keystore' to the path of your keystore file");
        log.info(
            "4. Set the keystore password via the environment variable 'MINECRAFT_MANAGEMENT_TLS_KEYSTORE_PASSWORD', or system property 'management.tls.keystore.password', or server property 'management-server-tls-keystore-password'"
        );
        log.info("5. Restart the server to apply the changes.");
    }
}
