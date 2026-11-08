package com.allvote.dtos.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutVote;

/**
 * Vue complète pour le support (numéro en clair : nécessaire pour traiter une réclamation).
 * montant = total payé ; fraisService inclus ; tauxFrais null pour les transactions antérieures aux frais.
 */
public record TransactionAdminDto(Long idTransaction, UUID reference, Instant dateCreation, Instant dateConfirmation,
        Long idOrganisateur, String organisateurNom, Long idConcours, String concoursTitre,
        Long idCandidat, String candidatNom, int nbVoix, int montant, BigDecimal tauxCommission,
        int montantCommission, int montantOrganisateur, Operateur operateur, String telephone, StatutVote statut,
        String message, String providerTxnId, String resoluPar, Instant dateResolution, Long idReversement,
        int fraisService, BigDecimal tauxFrais) {
}
