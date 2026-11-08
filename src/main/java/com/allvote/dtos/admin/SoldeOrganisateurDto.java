package com.allvote.dtos.admin;

import com.allvote.utiles.Operateur;

/** Ce que AllVote doit à un organisateur (paiements confirmés pas encore inclus dans un reversement). */
public record SoldeOrganisateurDto(Long idOrganisateur, String nom, String slug, String numeroReversement,
        Operateur operateurReversement, long nbTransactions, long montant) {
}
