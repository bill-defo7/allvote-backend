package com.allvote.dtos.orga;

import java.time.Instant;

import com.allvote.utiles.StatutEvenement;

public record EvenementOrgaDto(Long idEvenement, String titre, String slug, String lieu, Instant dateDebut,
        Instant dateFin, String afficheUrl, StatutEvenement statut, int nbConcours, Instant dateCreation) {
}
