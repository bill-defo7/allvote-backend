package com.allvote.dtos.admin;

import java.math.BigDecimal;

/** Vue d'ensemble de la plateforme (montants en FCFA, paiements CONFIRMÉS uniquement). */
public record TableauBordDto(
        long organisateursEnAttente, long organisateursValides, long organisateursSuspendus,
        long evenementsPublies,
        long transactionsConfirmees, long encaisse, long commissionAllVote, long partOrganisateurs,
        long fraisService, long revenuAllVote,
        long resteAReverser, long reversementsAPayer, long dejaReverse,
        long transactionsAVerifier, long transactionsEnAttente,
        BigDecimal tauxCommissionDefaut, BigDecimal fraisVotantPourcentage) {
}
