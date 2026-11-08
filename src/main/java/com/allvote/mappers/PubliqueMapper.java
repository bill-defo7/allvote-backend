package com.allvote.mappers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.allvote.dtos.publique.*;
import com.allvote.entities.Candidat;
import com.allvote.entities.Categorie;
import com.allvote.entities.Concours;
import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.utiles.StatutConcours;

/**
 * Entité -> DTO pour l'API publique. Filtre ce que le public n'a pas à voir :
 * concours en brouillon, candidats désactivés, voix quand les scores sont masqués.
 */
public final class PubliqueMapper {

    private PubliqueMapper() {
    }

    public static OrganisateurResumeDto organisateurResume(Organisateur o) {
        return new OrganisateurResumeDto(o.getNom(), o.getSlug(), o.getLogoUrl(), o.getVille());
    }

    public static EvenementLienDto evenementLien(Evenement e) {
        return new EvenementLienDto(e.getTitre(), e.getSlug(), e.getOrganisateur().getNom(), e.getOrganisateur().getSlug());
    }

    public static List<Concours> concoursVisibles(Evenement e) {
        return e.getConcours().stream().filter(c -> !c.isBrouillon()).toList();
    }

    public static EvenementResumeDto evenementResume(Evenement e, Instant now) {
        List<Concours> visibles = concoursVisibles(e);
        boolean enCours = visibles.stream().anyMatch(c -> c.statutEffectif(now) == StatutConcours.OUVERT);
        return new EvenementResumeDto(e.getIdEvenement(), e.getTitre(), e.getSlug(), e.getLieu(),
                e.getDateDebut(), e.getDateFin(), e.getAfficheUrl(), organisateurResume(e.getOrganisateur()),
                visibles.size(), enCours);
    }

    public static EvenementDetailDto evenementDetail(Evenement e, Instant now) {
        List<ConcoursResumeDto> concours = concoursVisibles(e).stream().map(c -> concoursResume(c, now)).toList();
        return new EvenementDetailDto(e.getIdEvenement(), e.getTitre(), e.getSlug(), e.getDescription(), e.getLieu(),
                e.getDateDebut(), e.getDateFin(), e.getAfficheUrl(), organisateurResume(e.getOrganisateur()),
                concours, now);
    }

    public static ConcoursResumeDto concoursResume(Concours c, Instant now) {
        int nb = c.getCategories().stream()
                .mapToInt(k -> (int) k.getCandidats().stream().filter(Candidat::isActif).count()).sum();
        return new ConcoursResumeDto(c.getIdConcours(), c.getTitre(), c.getDescription(), c.getAfficheUrl(),
                c.getPrixVoix(), c.getDateOuverture(), c.getDateFermeture(), c.statutEffectif(now), nb);
    }

    public static ConcoursDetailDto concoursDetail(Concours c, Instant now) {
        boolean visibles = c.votesVisibles(now);
        List<CategorieDto> categories = new ArrayList<>();
        long total = 0;
        for (Categorie k : c.getCategories()) {
            List<Candidat> actifs = actifs(k);
            total += actifs.stream().mapToLong(Candidat::getVotes).sum();
            categories.add(new CategorieDto(k.getIdCategorie(), k.getNom(), k.getOrdre(),
                    actifs.stream().map(x -> candidat(x, actifs, visibles)).toList()));
        }
        return new ConcoursDetailDto(c.getIdConcours(), c.getTitre(), c.getDescription(), c.getAfficheUrl(),
                c.getPrixVoix(), visibles, c.getDateOuverture(), c.getDateFermeture(), c.statutEffectif(now), now,
                evenementLien(c.getEvenement()), categories, visibles ? total : null);
    }

    public static CandidatPageDto candidatPage(Candidat x, Instant now) {
        Categorie k = x.getCategorie();
        Concours c = k.getConcours();
        CandidatDto dto = candidat(x, actifs(k), c.votesVisibles(now));
        return new CandidatPageDto(dto, k.getIdCategorie(), k.getNom(), concoursResume(c, now),
                evenementLien(c.getEvenement()), now);
    }

    private static List<Candidat> actifs(Categorie k) {
        return k.getCandidats().stream().filter(Candidat::isActif).toList();
    }

    /** rang = 1 + nombre de candidats de la catégorie ayant strictement plus de voix (ex aequo = même rang). */
    private static CandidatDto candidat(Candidat x, List<Candidat> categorie, boolean visibles) {
        Long votes = null;
        Integer rang = null;
        if (visibles) {
            votes = x.getVotes();
            rang = 1 + (int) categorie.stream().filter(o -> o.getVotes() > x.getVotes()).count();
        }
        return new CandidatDto(x.getIdCandidat(), x.getNumero(), x.getNom(), x.getSousTitre(), x.getDescription(),
                x.getPhotoUrl(), x.getTypeCandidat(), votes, rang);
    }
}
