package com.allvote.dtos.orga;

import java.time.Instant;

import com.allvote.utiles.TypeCandidat;

/** Vue organisateur : voit TOUJOURS les voix, et les candidats désactivés. */
public record CandidatOrgaDto(Long idCandidat, int numero, String nom, String sousTitre, String description,
        String photoUrl, TypeCandidat typeCandidat, long votes, boolean actif, Instant dateCreation) {
}
