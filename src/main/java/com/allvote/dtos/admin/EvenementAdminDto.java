package com.allvote.dtos.admin;

import java.time.Instant;

import com.allvote.utiles.StatutEvenement;

public record EvenementAdminDto(Long idEvenement, String titre, String slug, StatutEvenement statut,
        Instant dateDebut, Long idOrganisateur, String organisateurNom, int nbConcours, Instant dateCreation) {
}
