package com.allvote.dtos.orga;

import java.time.Instant;

import com.allvote.utiles.StatutConcours;

public record ConcoursOrgaDto(Long idConcours, String titre, int prixVoix, boolean scoresPublics, boolean brouillon,
        Instant dateOuverture, Instant dateFermeture, StatutConcours statutForce, StatutConcours statut,
        int nbCategories, int nbCandidats, long totalVotes) {
}
