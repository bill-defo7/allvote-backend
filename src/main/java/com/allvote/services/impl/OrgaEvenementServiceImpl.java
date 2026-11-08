package com.allvote.services.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.orga.*;
import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.OrgaMapper;
import com.allvote.repositories.EvenementRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.OrgaEvenementService;
import com.allvote.utiles.Slugs;
import com.allvote.utiles.StatutEvenement;
import com.allvote.utiles.StatutOrganisateur;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrgaEvenementServiceImpl implements OrgaEvenementService {

    private final AccesOrganisateur acces;
    private final EvenementRepository evenementRepository;
    private final TransactionVoteRepository transactionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EvenementOrgaDto> lister(Long idOrganisateur) {
        return evenementRepository.findByOrganisateur_IdOrganisateurOrderByDateCreationDesc(idOrganisateur)
                .stream().map(OrgaMapper::evenement).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvenementOrgaDetailDto detail(Long idOrganisateur, Long idEvenement) {
        return OrgaMapper.evenementDetail(acces.evenement(idOrganisateur, idEvenement), Instant.now());
    }

    @Override
    public EvenementOrgaDetailDto creer(Long idOrganisateur, EvenementRequest r) {
        Organisateur o = acces.organisateur(idOrganisateur);
        Evenement e = new Evenement();
        e.setOrganisateur(o);
        e.setSlug(slugUnique(r.titre()));     // fixé à la création : les liens partagés ne cassent jamais
        e.setStatut(StatutEvenement.BROUILLON);
        remplir(e, r);
        return OrgaMapper.evenementDetail(evenementRepository.save(e), Instant.now());
    }

    @Override
    public EvenementOrgaDetailDto modifier(Long idOrganisateur, Long idEvenement, EvenementRequest r) {
        Evenement e = acces.evenement(idOrganisateur, idEvenement);
        remplir(e, r);
        return OrgaMapper.evenementDetail(e, Instant.now());
    }

    @Override
    public EvenementOrgaDetailDto changerStatut(Long idOrganisateur, Long idEvenement, StatutEvenementRequest r) {
        Evenement e = acces.evenement(idOrganisateur, idEvenement);
        if (r.statut() == StatutEvenement.PUBLIE && e.getOrganisateur().getStatut() != StatutOrganisateur.VALIDE) {
            throw ApiException.forbidden("Votre organisation doit être validée par AllVote avant de publier.");
        }
        e.setStatut(r.statut());
        return OrgaMapper.evenementDetail(e, Instant.now());
    }

    @Override
    public void supprimer(Long idOrganisateur, Long idEvenement) {
        Evenement e = acces.evenement(idOrganisateur, idEvenement);
        if (transactionRepository.existsByConcours_Evenement_IdEvenement(idEvenement)) {
            throw ApiException.conflict("Des paiements existent pour cet événement : archivez-le au lieu de le supprimer.");
        }
        evenementRepository.delete(e);
    }

    // ------------------------------------------------------------------

    private void remplir(Evenement e, EvenementRequest r) {
        if (r.dateDebut() != null && r.dateFin() != null && !r.dateFin().isAfter(r.dateDebut())) {
            throw ApiException.badRequest("La date de fin doit être après la date de début.");
        }
        e.setTitre(r.titre().trim());
        e.setDescription(OrgaProfilServiceImpl.vide(r.description()));
        e.setLieu(OrgaProfilServiceImpl.vide(r.lieu()));
        e.setDateDebut(r.dateDebut());
        e.setDateFin(r.dateFin());
        e.setAfficheUrl(OrgaProfilServiceImpl.vide(r.afficheUrl()));
    }

    private String slugUnique(String titre) {
        String base = Slugs.slugifier(titre);
        if (base.isBlank()) {
            base = "evenement";
        }
        String slug = base;
        int i = 2;
        while (evenementRepository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}
