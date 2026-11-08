package com.allvote.dtos.admin;

import java.time.Instant;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutReversement;

public record ReversementDto(Long idReversement, Long idOrganisateur, String organisateurNom,
        String numeroReversement, Operateur operateurReversement, long montant, int nbTransactions,
        Instant periodeDebut, Instant periodeFin, StatutReversement statut, String referencePaiement,
        Instant datePaiement, String note, String effectuePar) {
}
