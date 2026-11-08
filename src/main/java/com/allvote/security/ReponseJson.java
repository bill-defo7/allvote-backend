package com.allvote.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

/** Écrit une erreur JSON {status, message} directement depuis un filtre (hors contrôleur). */
public final class ReponseJson {

    private ReponseJson() {
    }

    public static void erreur(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        String m = message.replace("\\", "\\\\").replace("\"", "\\\"");
        res.getWriter().write("{\"status\":" + status + ",\"message\":\"" + m + "\"}");
    }
}
