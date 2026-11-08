package com.allvote.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.allvote.entities.Organisateur;
import com.allvote.entities.Utilisateur;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.utiles.StatutOrganisateur;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Après la vérification du jeton : le compte existe-t-il encore, est-il actif,
 * son organisation n'est-elle pas suspendue ou refusée ?
 * Ainsi, suspendre un organisateur coupe l'accès IMMÉDIATEMENT, sans attendre l'expiration du jeton.
 * (Créé dans SecurityConfig, volontairement PAS en @Component pour ne pas s'exécuter deux fois.)
 */
public class CompteActifFilter extends OncePerRequestFilter {

    private final UtilisateurRepository utilisateurRepository;

    public CompteActifFilter(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken t) {
            Long id;
            try {
                id = Long.valueOf(t.getToken().getSubject());
            } catch (NumberFormatException e) {
                refuser(res, "Session invalide. Reconnectez-vous.");
                return;
            }
            Utilisateur u = utilisateurRepository.findAvecOrganisateur(id).orElse(null);
            if (u == null || !u.isActif()) {
                refuser(res, "Compte désactivé ou introuvable.");
                return;
            }
            Organisateur o = u.getOrganisateur();
            if (o != null && (o.getStatut() == StatutOrganisateur.SUSPENDU || o.getStatut() == StatutOrganisateur.REFUSE)) {
                refuser(res, "Votre organisation n'a plus accès à AllVote. Contactez le support.");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void refuser(HttpServletResponse res, String message) throws IOException {
        SecurityContextHolder.clearContext();
        ReponseJson.erreur(res, 401, message);
    }
}
