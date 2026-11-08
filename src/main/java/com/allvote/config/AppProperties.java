package com.allvote.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration applicative (préfixe "app" dans application.yaml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontUrl,
        String publicUrl,
        boolean donneesDemo,
        Cors cors,
        Jwt jwt,
        SuperAdmin superAdmin,
        Payment payment,
        Vote vote,
        Storage storage,
        Security security) {

    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(String secret, long expirationMinutes) {}

    public record SuperAdmin(String email, String password) {}

    public record Payment(String mode, Aangaraa aangaraa) {}

    public record Aangaraa(String baseUrl, String appKey, String notifyUrl, String returnUrl) {}

    public record Vote(int maxVoixParTransaction, int expirationMinutes, long reconciliationDelayMs) {}

    public record Storage(String dir) {}

    public record Security(boolean trustForwardedHeaders) {}

    /** URL du front sans "/" final. */
    public String frontBase() {
        return frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    /** URL publique de l'API sans "/" final. */
    public String publicBase() {
        return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }
}
