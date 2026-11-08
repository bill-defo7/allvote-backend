package com.allvote.dtos.publique;

import java.time.Instant;
import java.util.List;

import com.allvote.utiles.StatutConcours;

/** Page de vote d'un concours. totalVotes = null si scores masqués. */
public record ConcoursDetailDto(Long idConcours, String titre, String description, String afficheUrl,
        int prixVoix, boolean votesVisibles, Instant dateOuverture, Instant dateFermeture,
        StatutConcours statut, Instant serverTime, EvenementLienDto evenement,
        List<CategorieDto> categories, Long totalVotes) {
}
