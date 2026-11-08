package com.allvote.dtos.auth;

import java.time.Instant;
import java.util.List;

/** Profil de l'utilisateur connecté — jamais le hash du mot de passe. */
public record UtilisateurDto(Long idUtilisateur, String nom, String prenom, String email, String telephone,
        List<String> roles, List<String> permissions, OrganisateurCourtDto organisateur, Instant derniereConnexion) {
}
