package com.allvote.dtos.orga;

import java.time.Instant;
import java.util.List;

import com.allvote.utiles.StatutConcours;

public record ConcoursOrgaDetailDto(Long idConcours, Long idEvenement, String evenementTitre, String titre,
        String description, String afficheUrl, int prixVoix, boolean scoresPublics, boolean brouillon,
        Instant dateOuverture, Instant dateFermeture, StatutConcours statutForce, StatutConcours statut,
        List<CategorieOrgaDto> categories, long totalVotes, Instant serverTime) {
}
