package com.allvote.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.allvote.config.AppProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Limitation de débit par adresse IP (fenêtre fixe d'une minute), en mémoire.
 * Protège contre le spam de votes / d'inscriptions, le brute-force du login et l'abus du webhook.
 * (Suffisant pour une seule instance ; avec plusieurs instances, passer sur Redis.)
 * La première règle qui correspond s'applique : les plus précises sont en haut.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private record Regle(String nom, String methode, String prefixe, int maxParMinute) {
        boolean correspond(HttpServletRequest r) {
            return methode.equals(r.getMethod()) && r.getRequestURI().startsWith(prefixe);
        }
    }

    private static final List<Regle> REGLES = List.of(
            new Regle("vote", "POST", "/api/public/votes", 10),
            new Regle("statut-vote", "GET", "/api/public/votes/", 90),
            new Regle("login", "POST", "/api/auth/login", 10),
            new Regle("mot-de-passe", "PUT", "/api/auth/mot-de-passe", 10),
            new Regle("inscription", "POST", "/api/public/organisateurs/inscription", 5),
            new Regle("webhook", "POST", "/api/payments/", 120),
            new Regle("upload", "POST", "/api/orga/uploads", 30),
            new Regle("lecture", "GET", "/api/public", 240));

    private static final class Fenetre {
        final long debut;
        final AtomicInteger compteur = new AtomicInteger();

        Fenetre(long debut) {
            this.debut = debut;
        }
    }

    private final Map<String, Fenetre> fenetres = new ConcurrentHashMap<>();
    private final AppProperties props;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Regle regle = REGLES.stream().filter(r -> r.correspond(req)).findFirst().orElse(null);
        if (regle == null) {
            chain.doFilter(req, res);
            return;
        }
        long minute = System.currentTimeMillis() / 60_000;
        String cle = regle.nom() + "|" + ClientIp.de(req, props.security().trustForwardedHeaders());
        Fenetre f = fenetres.compute(cle, (k, v) -> (v == null || v.debut != minute) ? new Fenetre(minute) : v);

        if (f.compteur.incrementAndGet() > regle.maxParMinute()) {
            res.setHeader("Retry-After", "60");
            ReponseJson.erreur(res, 429, "Trop de requêtes. Patientez une minute puis réessayez.");
            return;
        }
        if (fenetres.size() > 50_000) {
            fenetres.entrySet().removeIf(e -> e.getValue().debut != minute); // nettoyage
        }
        chain.doFilter(req, res);
    }
}
