package com.allvote.dtos.orga;

import java.math.BigDecimal;
import java.time.Instant;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutOrganisateur;

/** Profil complet de SON organisation (vu par ses membres). tauxCommission = taux réellement appliqué. */
public record ProfilOrganisateurDto(Long idOrganisateur, String nom, String slug, String description, String logoUrl,
        String email, String telephone, String ville, StatutOrganisateur statut, String motifRefus,
        BigDecimal tauxCommission, String numeroReversement, Operateur operateurReversement,
        Instant dateCreation, Instant dateValidation) {
}
