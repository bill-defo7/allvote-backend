package com.allvote.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.allvote.exceptions.ApiException;

/**
 * Qui fait la requête ? Lu UNIQUEMENT depuis le jeton signé.
 * Règle d'or : l'idOrganisateur ne vient JAMAIS de l'URL ni du corps de la requête,
 * sinon un organisateur pourrait lire les données d'un autre.
 */
public final class UtilisateurConnecte {

    private UtilisateurConnecte() {
    }

    private static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken t) {
            return t.getToken();
        }
        throw ApiException.unauthorized("Connexion requise.");
    }

    public static Long idUtilisateur() {
        return Long.valueOf(jwt().getSubject());
    }

    /** Organisation de l'utilisateur connecté ; erreur 403 s'il n'en a pas (super-admin). */
    public static Long idOrganisateur() {
        Object org = jwt().getClaim(JwtService.CLAIM_ORG);
        if (org instanceof Number n) {
            return n.longValue();
        }
        throw ApiException.forbidden("Aucune organisation rattachée à ce compte.");
    }
}
