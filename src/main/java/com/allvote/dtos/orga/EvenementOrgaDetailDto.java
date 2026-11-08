package com.allvote.dtos.orga;

import java.time.Instant;
import java.util.List;

import com.allvote.utiles.StatutEvenement;

public record EvenementOrgaDetailDto(Long idEvenement, String titre, String slug, String description, String lieu,
        Instant dateDebut, Instant dateFin, String afficheUrl, StatutEvenement statut, Instant dateCreation,
        List<ConcoursOrgaDto> concours, Instant serverTime) {
}
