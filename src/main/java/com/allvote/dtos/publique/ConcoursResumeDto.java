package com.allvote.dtos.publique;

import java.time.Instant;

import com.allvote.utiles.StatutConcours;

public record ConcoursResumeDto(Long idConcours, String titre, String description, String afficheUrl,
        int prixVoix, Instant dateOuverture, Instant dateFermeture, StatutConcours statut, int nbCandidats) {
}
