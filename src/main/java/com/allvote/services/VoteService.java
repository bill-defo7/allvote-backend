package com.allvote.services;

import java.util.UUID;

import com.allvote.dtos.vote.VoteRequest;
import com.allvote.dtos.vote.VoteResponse;
import com.allvote.dtos.vote.VoteStatutDto;

/**
 * Flux d'un vote :
 *  1. creer()    -> transaction EN_ATTENTE (commission figée) + demande de paiement (invite USSD)
 *  2. verifier() -> appelé par le webhook, la tâche planifiée ou le front ;
 *                   demande l'état RÉEL au prestataire et crédite les voix une seule fois.
 */
public interface VoteService {

    VoteResponse creer(VoteRequest request);

    VoteStatutDto statut(UUID reference);

    void verifier(UUID reference);
}
