package com.allvote.security;

import jakarta.servlet.http.HttpServletRequest;

/** Adresse IP du client. X-Forwarded-For n'est lu QUE derrière un proxy de confiance (sinon falsifiable). */
public final class ClientIp {

    private ClientIp() {
    }

    public static String de(HttpServletRequest req, boolean faireConfianceAuProxy) {
        if (faireConfianceAuProxy) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }
}
