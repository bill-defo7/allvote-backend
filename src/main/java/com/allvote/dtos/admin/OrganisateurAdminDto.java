package com.allvote.dtos.admin;

import java.math.BigDecimal;
import java.time.Instant;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutOrganisateur;

/** tauxCommission = taux propre (null = taux par défaut) ; tauxApplique = celui réellement utilisé. */
public record OrganisateurAdminDto(Long idOrganisateur, String nom, String slug, String description, String email,
        String telephone, String ville, StatutOrganisateur statut, String motif,
        BigDecimal tauxCommission, BigDecimal tauxApplique, String numeroReversement, Operateur operateurReversement,
        Instant dateCreation, Instant dateValidation, int nbEvenements, ResponsableDto responsable) {

    public record ResponsableDto(Long idUtilisateur, String nom, String prenom, String email, String telephone) {
    }
}
