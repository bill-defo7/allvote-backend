package com.allvote.dtos.publique;

import java.time.Instant;

/** Carte d'un événement sur la page d'accueil / la recherche. */
public record EvenementResumeDto(Long idEvenement, String titre, String slug, String lieu,
        Instant dateDebut, Instant dateFin, String afficheUrl, OrganisateurResumeDto organisateur,
        int nbConcours, boolean voteEnCours) {
}
