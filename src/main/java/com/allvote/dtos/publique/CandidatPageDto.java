package com.allvote.dtos.publique;

import java.time.Instant;

/** Page partagée d'un candidat (lien direct pour voter pour lui). */
public record CandidatPageDto(CandidatDto candidat, Long idCategorie, String categorieNom,
        ConcoursResumeDto concours, EvenementLienDto evenement, Instant serverTime) {
}
