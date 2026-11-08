package com.allvote.dtos.orga;

/** Paiements CONFIRMÉS d'un concours. Montants en FCFA. */
public record FinanceConcoursDto(Long idConcours, String titre, long nbTransactions, long voix,
        long encaisse, long commission, long partOrganisateur) {
}
