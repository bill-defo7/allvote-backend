package com.allvote.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.allvote.dtos.orga.*;
import com.allvote.entities.*;

/** Entité -> DTO pour l'espace organisateur (vue complète : brouillons, candidats inactifs, voix). */
public final class OrgaMapper {

    private OrgaMapper() {
    }

    public static ProfilOrganisateurDto profil(Organisateur o, BigDecimal tauxApplique) {
        return new ProfilOrganisateurDto(o.getIdOrganisateur(), o.getNom(), o.getSlug(), o.getDescription(),
                o.getLogoUrl(), o.getEmail(), o.getTelephone(), o.getVille(), o.getStatut(), o.getMotifRefus(),
                tauxApplique, o.getNumeroReversement(), o.getOperateurReversement(), o.getDateCreation(),
                o.getDateValidation());
    }

    public static EvenementOrgaDto evenement(Evenement e) {
        return new EvenementOrgaDto(e.getIdEvenement(), e.getTitre(), e.getSlug(), e.getLieu(), e.getDateDebut(),
                e.getDateFin(), e.getAfficheUrl(), e.getStatut(), e.getConcours().size(), e.getDateCreation());
    }

    public static EvenementOrgaDetailDto evenementDetail(Evenement e, Instant now) {
        List<ConcoursOrgaDto> concours = e.getConcours().stream().map(c -> concours(c, now)).toList();
        return new EvenementOrgaDetailDto(e.getIdEvenement(), e.getTitre(), e.getSlug(), e.getDescription(),
                e.getLieu(), e.getDateDebut(), e.getDateFin(), e.getAfficheUrl(), e.getStatut(), e.getDateCreation(),
                concours, now);
    }

    public static ConcoursOrgaDto concours(Concours c, Instant now) {
        int nbCandidats = c.getCategories().stream().mapToInt(k -> k.getCandidats().size()).sum();
        return new ConcoursOrgaDto(c.getIdConcours(), c.getTitre(), c.getPrixVoix(), c.isScoresPublics(),
                c.isBrouillon(), c.getDateOuverture(), c.getDateFermeture(), c.getStatutForce(), c.statutEffectif(now),
                c.getCategories().size(), nbCandidats, totalVotes(c));
    }

    public static ConcoursOrgaDetailDto concoursDetail(Concours c, Instant now) {
        // trié ici : après une modification de l'ordre, la liste en mémoire n'est pas encore re-triée
        List<CategorieOrgaDto> categories = c.getCategories().stream()
                .sorted(Comparator.comparingInt(Categorie::getOrdre).thenComparing(Categorie::getIdCategorie))
                .map(OrgaMapper::categorie).toList();
        return new ConcoursOrgaDetailDto(c.getIdConcours(), c.getEvenement().getIdEvenement(),
                c.getEvenement().getTitre(), c.getTitre(), c.getDescription(), c.getAfficheUrl(), c.getPrixVoix(),
                c.isScoresPublics(), c.isBrouillon(), c.getDateOuverture(), c.getDateFermeture(), c.getStatutForce(),
                c.statutEffectif(now), categories, totalVotes(c), now);
    }

    public static CategorieOrgaDto categorie(Categorie k) {
        return new CategorieOrgaDto(k.getIdCategorie(), k.getNom(), k.getOrdre(),
                k.getCandidats().stream().sorted(Comparator.comparingInt(Candidat::getNumero))
                        .map(OrgaMapper::candidat).toList());
    }

    public static CandidatOrgaDto candidat(Candidat x) {
        return new CandidatOrgaDto(x.getIdCandidat(), x.getNumero(), x.getNom(), x.getSousTitre(), x.getDescription(),
                x.getPhotoUrl(), x.getTypeCandidat(), x.getVotes(), x.isActif(), x.getDateCreation());
    }

    public static MembreDto membre(Utilisateur u) {
        return new MembreDto(u.getIdUtilisateur(), u.getNom(), u.getPrenom(), u.getEmail(), u.getTelephone(),
                u.getRoles().stream().map(Role::getNomRole).sorted().toList(), u.isActif(), u.getDerniereConnexion(),
                u.getDateCreation());
    }

    private static long totalVotes(Concours c) {
        return c.getCategories().stream().flatMap(k -> k.getCandidats().stream()).mapToLong(Candidat::getVotes).sum();
    }
}
