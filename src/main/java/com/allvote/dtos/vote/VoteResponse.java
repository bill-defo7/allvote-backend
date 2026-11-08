package com.allvote.dtos.vote;

import java.util.UUID;

import com.allvote.utiles.StatutVote;

/** montant = total à payer (valeur des voix + fraisService). */
public record VoteResponse(UUID reference, StatutVote statut, int nbVoix, int montant, String message,
        int valeurVoix, int fraisService) {
}
