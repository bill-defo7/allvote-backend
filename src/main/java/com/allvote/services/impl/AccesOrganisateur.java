package com.allvote.services.impl;

import org.springframework.stereotype.Component;

import com.allvote.entities.Candidat;
import com.allvote.entities.Categorie;
import com.allvote.entities.Concours;
import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.repositories.CandidatRepository;
import com.allvote.repositories.CategorieRepository;
import com.allvote.repositories.ConcoursRepository;
import com.allvote.repositories.EvenementRepository;
import com.allvote.repositories.OrganisateurRepository;

import lombok.RequiredArgsConstructor;

/**
 * Charge une ressource UNIQUEMENT si elle appartient à l'organisateur du jeton.
 * Si elle appartient à un autre : 404 (on ne confirme même pas qu'elle existe).
 */
@Component
@RequiredArgsConstructor
public class AccesOrganisateur {

    private final OrganisateurRepository organisateurRepository;
    private final EvenementRepository evenementRepository;
    private final ConcoursRepository concoursRepository;
    private final CategorieRepository categorieRepository;
    private final CandidatRepository candidatRepository;

    public Organisateur organisateur(Long idOrganisateur) {
        return organisateurRepository.findById(idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Organisation introuvable."));
    }

    public Evenement evenement(Long idOrganisateur, Long idEvenement) {
        return evenementRepository.findByIdEvenementAndOrganisateur_IdOrganisateur(idEvenement, idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Événement introuvable."));
    }

    public Concours concours(Long idOrganisateur, Long idConcours) {
        return concoursRepository.findByIdConcoursAndEvenement_Organisateur_IdOrganisateur(idConcours, idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Concours introuvable."));
    }

    public Categorie categorie(Long idOrganisateur, Long idCategorie) {
        return categorieRepository.findByIdCategorieAndConcours_Evenement_Organisateur_IdOrganisateur(idCategorie, idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Catégorie introuvable."));
    }

    public Candidat candidat(Long idOrganisateur, Long idCandidat) {
        return candidatRepository.findByIdCandidatAndCategorie_Concours_Evenement_Organisateur_IdOrganisateur(idCandidat, idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Candidat introuvable."));
    }
}
