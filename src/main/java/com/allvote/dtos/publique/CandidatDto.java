package com.allvote.dtos.publique;

import com.allvote.utiles.TypeCandidat;

/** votes et rang valent null quand l'organisateur a masqué les scores. */
public record CandidatDto(Long idCandidat, int numero, String nom, String sousTitre, String description,
        String photoUrl, TypeCandidat typeCandidat, Long votes, Integer rang) {
}
