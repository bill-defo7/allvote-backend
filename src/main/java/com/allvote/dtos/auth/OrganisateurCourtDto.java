package com.allvote.dtos.auth;

import com.allvote.utiles.StatutOrganisateur;

/** L'organisation de l'utilisateur connecté (le front adapte l'espace selon le statut). */
public record OrganisateurCourtDto(Long idOrganisateur, String nom, String slug, String logoUrl,
        StatutOrganisateur statut, String motifRefus) {
}
