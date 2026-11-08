package com.allvote.services.impl;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.orga.*;
import com.allvote.entities.Candidat;
import com.allvote.entities.Categorie;
import com.allvote.entities.Concours;
import com.allvote.entities.Evenement;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.OrgaMapper;
import com.allvote.repositories.CandidatRepository;
import com.allvote.repositories.CategorieRepository;
import com.allvote.repositories.ConcoursRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.OrgaConcoursService;
import com.allvote.utiles.StatutConcours;
import com.allvote.utiles.TypeCandidat;

import lombok.RequiredArgsConstructor;

/**
 * Règle de prudence : dès qu'un paiement existe, on ne SUPPRIME plus (concours, catégorie, candidat)
 * pour garder la traçabilité de l'argent. On désactive / on ferme à la place.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrgaConcoursServiceImpl implements OrgaConcoursService {

    private final AccesOrganisateur acces;
    private final ConcoursRepository concoursRepository;
    private final CategorieRepository categorieRepository;
    private final CandidatRepository candidatRepository;
    private final TransactionVoteRepository transactionRepository;

    // =================================================================
    // Concours
    // =================================================================
    @Override
    @Transactional(readOnly = true)
    public ConcoursOrgaDetailDto detail(Long idOrganisateur, Long idConcours) {
        return vers(acces.concours(idOrganisateur, idConcours));
    }

    @Override
    public ConcoursOrgaDetailDto creer(Long idOrganisateur, Long idEvenement, ConcoursRequest r) {
        Evenement e = acces.evenement(idOrganisateur, idEvenement);
        Concours c = new Concours();
        c.setEvenement(e);
        remplir(c, r);
        e.getConcours().add(c);
        return vers(concoursRepository.save(c));
    }

    @Override
    public ConcoursOrgaDetailDto modifier(Long idOrganisateur, Long idConcours, ConcoursRequest r) {
        Concours c = acces.concours(idOrganisateur, idConcours);
        remplir(c, r);
        return vers(c);
    }

    @Override
    public ConcoursOrgaDetailDto changerStatut(Long idOrganisateur, Long idConcours, StatutConcoursRequest r) {
        Concours c = acces.concours(idOrganisateur, idConcours);
        StatutConcours s = r.statutForce();
        if (s != null && s != StatutConcours.OUVERT && s != StatutConcours.SUSPENDU && s != StatutConcours.FERME) {
            throw ApiException.badRequest("Statut forcé possible : OUVERT, SUSPENDU, FERME (ou null = automatique).");
        }
        c.setStatutForce(s);
        return vers(c);
    }

    @Override
    public void supprimer(Long idOrganisateur, Long idConcours) {
        Concours c = acces.concours(idOrganisateur, idConcours);
        if (transactionRepository.existsByConcours_IdConcours(idConcours)) {
            throw ApiException.conflict("Des paiements existent pour ce concours : fermez-le au lieu de le supprimer.");
        }
        c.getEvenement().getConcours().remove(c);   // orphanRemoval supprime concours, catégories et candidats
    }

    private void remplir(Concours c, ConcoursRequest r) {
        if (!r.dateFermeture().isAfter(r.dateOuverture())) {
            throw ApiException.badRequest("La date de fermeture doit être après la date d'ouverture.");
        }
        c.setTitre(r.titre().trim());
        c.setDescription(OrgaProfilServiceImpl.vide(r.description()));
        c.setAfficheUrl(OrgaProfilServiceImpl.vide(r.afficheUrl()));
        c.setPrixVoix(r.prixVoix());          // le prix payé est figé sur chaque transaction : changer ici ne touche pas l'historique
        c.setScoresPublics(!Boolean.FALSE.equals(r.scoresPublics()));   // absent -> true
        c.setDateOuverture(r.dateOuverture());
        c.setDateFermeture(r.dateFermeture());
        c.setBrouillon(Boolean.TRUE.equals(r.brouillon()));              // absent -> false
    }

    // =================================================================
    // Catégories
    // =================================================================
    @Override
    public ConcoursOrgaDetailDto ajouterCategorie(Long idOrganisateur, Long idConcours, CategorieRequest r) {
        Concours c = acces.concours(idOrganisateur, idConcours);
        if (c.getCategories().size() >= 50) {
            throw ApiException.badRequest("50 catégories maximum par concours.");
        }
        Categorie k = new Categorie();
        k.setConcours(c);
        k.setNom(r.nom().trim());
        k.setOrdre(r.ordre() != null ? r.ordre()
                : c.getCategories().stream().mapToInt(Categorie::getOrdre).max().orElse(0) + 1);
        c.getCategories().add(categorieRepository.save(k));
        return vers(c);
    }

    @Override
    public ConcoursOrgaDetailDto modifierCategorie(Long idOrganisateur, Long idCategorie, CategorieRequest r) {
        Categorie k = acces.categorie(idOrganisateur, idCategorie);
        k.setNom(r.nom().trim());
        if (r.ordre() != null) {
            k.setOrdre(r.ordre());
        }
        return vers(k.getConcours());
    }

    @Override
    public ConcoursOrgaDetailDto ordonnerCategories(Long idOrganisateur, Long idConcours, OrdreRequest r) {
        Concours c = acces.concours(idOrganisateur, idConcours);
        Map<Long, Categorie> parId = c.getCategories().stream()
                .collect(Collectors.toMap(Categorie::getIdCategorie, Function.identity()));
        if (r.ids().size() != parId.size() || !parId.keySet().equals(new HashSet<>(r.ids()))) {
            throw ApiException.badRequest("La liste doit contenir exactement toutes les catégories du concours.");
        }
        List<Long> ids = r.ids();
        for (int i = 0; i < ids.size(); i++) {
            parId.get(ids.get(i)).setOrdre(i + 1);
        }
        return vers(c);
    }

    @Override
    public void supprimerCategorie(Long idOrganisateur, Long idCategorie) {
        Categorie k = acces.categorie(idOrganisateur, idCategorie);
        if (transactionRepository.existsByCandidat_Categorie_IdCategorie(idCategorie)) {
            throw ApiException.conflict("Des paiements existent dans cette catégorie : désactivez ses candidats.");
        }
        k.getConcours().getCategories().remove(k);
    }

    // =================================================================
    // Candidats
    // =================================================================
    @Override
    public CandidatOrgaDto ajouterCandidat(Long idOrganisateur, Long idCategorie, CandidatRequest r) {
        Categorie k = acces.categorie(idOrganisateur, idCategorie);
        if (k.getCandidats().size() >= 200) {
            throw ApiException.badRequest("200 candidats maximum par catégorie.");
        }
        int numero;
        if (r.numero() != null) {
            if (candidatRepository.existsByCategorie_IdCategorieAndNumero(idCategorie, r.numero())) {
                throw ApiException.conflict("Le numéro " + r.numero() + " est déjà pris dans cette catégorie.");
            }
            numero = r.numero();
        } else {
            numero = candidatRepository.numeroMax(idCategorie) + 1;
        }
        Candidat x = new Candidat();
        x.setCategorie(k);
        x.setNumero(numero);
        remplir(x, r);
        x = candidatRepository.save(x);
        k.getCandidats().add(x);
        return OrgaMapper.candidat(x);
    }

    @Override
    public CandidatOrgaDto modifierCandidat(Long idOrganisateur, Long idCandidat, CandidatRequest r) {
        Candidat x = acces.candidat(idOrganisateur, idCandidat);
        if (r.numero() != null && r.numero() != x.getNumero()) {
            if (candidatRepository.existsByCategorie_IdCategorieAndNumeroAndIdCandidatNot(
                    x.getCategorie().getIdCategorie(), r.numero(), idCandidat)) {
                throw ApiException.conflict("Le numéro " + r.numero() + " est déjà pris dans cette catégorie.");
            }
            x.setNumero(r.numero());
        }
        remplir(x, r);
        return OrgaMapper.candidat(x);
    }

    @Override
    public CandidatOrgaDto activerCandidat(Long idOrganisateur, Long idCandidat, ActifRequest r) {
        Candidat x = acces.candidat(idOrganisateur, idCandidat);
        x.setActif(r.actif());
        return OrgaMapper.candidat(x);
    }

    @Override
    public void supprimerCandidat(Long idOrganisateur, Long idCandidat) {
        Candidat x = acces.candidat(idOrganisateur, idCandidat);
        if (x.getVotes() > 0 || transactionRepository.existsByCandidat_IdCandidat(idCandidat)) {
            throw ApiException.conflict("Ce candidat a déjà reçu des votes : désactivez-le au lieu de le supprimer.");
        }
        x.getCategorie().getCandidats().remove(x);
    }

    private void remplir(Candidat x, CandidatRequest r) {
        x.setNom(r.nom().trim());
        x.setSousTitre(OrgaProfilServiceImpl.vide(r.sousTitre()));
        x.setDescription(OrgaProfilServiceImpl.vide(r.description()));
        x.setPhotoUrl(OrgaProfilServiceImpl.vide(r.photoUrl()));
        x.setTypeCandidat(r.typeCandidat() != null ? r.typeCandidat() : TypeCandidat.PERSONNE);
    }

    private ConcoursOrgaDetailDto vers(Concours c) {
        return OrgaMapper.concoursDetail(c, Instant.now());
    }
}
