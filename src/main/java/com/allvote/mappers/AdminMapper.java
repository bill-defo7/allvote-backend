package com.allvote.mappers;

import java.math.BigDecimal;
import java.time.Instant;

import com.allvote.dtos.admin.*;
import com.allvote.entities.*;
import com.allvote.utiles.Roles;

/** Entité -> DTO pour l'espace super-admin. À appeler dans une transaction. */
public final class AdminMapper {

    private AdminMapper() {
    }

    public static OrganisateurAdminDto organisateur(Organisateur o, BigDecimal tauxDefaut) {
        OrganisateurAdminDto.ResponsableDto resp = o.getUtilisateurs().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> Roles.ORGA_ADMIN.equals(r.getNomRole())))
                .findFirst()
                .map(u -> new OrganisateurAdminDto.ResponsableDto(u.getIdUtilisateur(), u.getNom(), u.getPrenom(),
                        u.getEmail(), u.getTelephone()))
                .orElse(null);
        return new OrganisateurAdminDto(o.getIdOrganisateur(), o.getNom(), o.getSlug(), o.getDescription(),
                o.getEmail(), o.getTelephone(), o.getVille(), o.getStatut(), o.getMotifRefus(),
                o.getTauxCommission(), o.getTauxCommission() != null ? o.getTauxCommission() : tauxDefaut,
                o.getNumeroReversement(), o.getOperateurReversement(), o.getDateCreation(), o.getDateValidation(),
                o.getEvenements().size(), resp);
    }

    public static EvenementAdminDto evenement(Evenement e) {
        return new EvenementAdminDto(e.getIdEvenement(), e.getTitre(), e.getSlug(), e.getStatut(), e.getDateDebut(),
                e.getOrganisateur().getIdOrganisateur(), e.getOrganisateur().getNom(), e.getConcours().size(),
                e.getDateCreation());
    }

    public static TransactionAdminDto transaction(TransactionVote t) {
        return new TransactionAdminDto(t.getIdTransaction(), t.getReference(), t.getDateCreation(), t.getDateConfirmation(),
                t.getOrganisateur().getIdOrganisateur(), t.getOrganisateur().getNom(),
                t.getConcours().getIdConcours(), t.getConcours().getTitre(),
                t.getCandidat().getIdCandidat(), t.getCandidat().getNom(),
                t.getNbVoix(), t.getMontant(), t.getTauxCommission(), t.getMontantCommission(), t.getMontantOrganisateur(),
                t.getOperateur(), t.getTelephone(), t.getStatut(), t.getMessage(), t.getProviderTxnId(),
                t.getResoluPar() != null ? t.getResoluPar().getEmail() : null, t.getDateResolution(),
                t.getReversement() != null ? t.getReversement().getIdReversement() : null,
                t.getFraisService(), t.getTauxFrais());
    }

    public static ReversementDto reversement(Reversement r) {
        Organisateur o = r.getOrganisateur();
        return new ReversementDto(r.getIdReversement(), o.getIdOrganisateur(), o.getNom(), o.getNumeroReversement(),
                o.getOperateurReversement(), r.getMontant(), r.getNbTransactions(), r.getPeriodeDebut(),
                r.getPeriodeFin(), r.getStatut(), r.getReferencePaiement(), r.getDatePaiement(), r.getNote(),
                r.getEffectuePar() != null ? r.getEffectuePar().getEmail() : null);
    }

    public static UtilisateurAdminDto utilisateur(Utilisateur u, Instant now) {
        Organisateur o = u.getOrganisateur();
        boolean verrouille = u.getVerrouilleJusqua() != null && now.isBefore(u.getVerrouilleJusqua());
        return new UtilisateurAdminDto(u.getIdUtilisateur(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getRoles().stream().map(Role::getNomRole).sorted().toList(), u.isActif(), verrouille,
                u.getDerniereConnexion(), u.getDateCreation(),
                o != null ? o.getIdOrganisateur() : null, o != null ? o.getNom() : null);
    }
}
