package com.allvote.dtos.vote;

import java.util.UUID;

import com.allvote.utiles.StatutVote;

/** Interrogé par le front toutes les 3 s pendant que le votant valide sur son téléphone. */
public record VoteStatutDto(UUID reference, StatutVote statut, int nbVoix, int montant, String candidat, String message) {
}
