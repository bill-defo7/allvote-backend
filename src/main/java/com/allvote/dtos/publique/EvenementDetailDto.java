package com.allvote.dtos.publique;

import java.time.Instant;
import java.util.List;

/** Page d'un événement : infos + ses concours. serverTime sert au compte à rebours. */
public record EvenementDetailDto(Long idEvenement, String titre, String slug, String description, String lieu,
        Instant dateDebut, Instant dateFin, String afficheUrl, OrganisateurResumeDto organisateur,
        List<ConcoursResumeDto> concours, Instant serverTime) {
}
