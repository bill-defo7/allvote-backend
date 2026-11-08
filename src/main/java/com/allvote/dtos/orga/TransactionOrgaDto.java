package com.allvote.dtos.orga;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutVote;

/**
 * Un paiement de vote vu par l'organisateur. Le numéro du votant est masqué (vie privée).
 * montant = valeur des voix (hors frais de service payés par le votant, qui reviennent à AllVote).
 */
public record TransactionOrgaDto(UUID reference, Instant dateCreation, Instant dateConfirmation,
        Long idConcours, String concoursTitre, Long idCandidat, String candidatNom,
        int nbVoix, int montant, BigDecimal tauxCommission, int montantCommission, int montantOrganisateur,
        Operateur operateur, String telephone, StatutVote statut, boolean reverse) {
}
