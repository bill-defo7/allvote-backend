package com.allvote.dtos.orga;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord financier de l'organisateur (FCFA).
 * partOrganisateur = dejaReverse + reversementEnCours + resteAReverser.
 */
public record FinancesOrgaDto(BigDecimal tauxCommissionActuel, long nbTransactions, long voix,
        long encaisse, long commission, long partOrganisateur,
        long dejaReverse, long reversementEnCours, long resteAReverser,
        long transactionsAVerifier, List<FinanceConcoursDto> parConcours) {
}
