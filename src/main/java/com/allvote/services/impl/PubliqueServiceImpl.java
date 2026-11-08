package com.allvote.services.impl;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.publique.*;
import com.allvote.entities.Candidat;
import com.allvote.entities.Concours;
import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.PubliqueMapper;
import com.allvote.repositories.CandidatRepository;
import com.allvote.repositories.ConcoursRepository;
import com.allvote.repositories.EvenementRepository;
import com.allvote.repositories.OrganisateurRepository;
import com.allvote.services.PubliqueService;
import com.allvote.utiles.StatutEvenement;
import com.allvote.utiles.StatutOrganisateur;

import lombok.RequiredArgsConstructor;

/**
 * Lecture publique. Règle de visibilité unique :
 * événement PUBLIÉ + organisateur VALIDÉ + concours hors brouillon + candidat actif.
 * Tout le reste répond 404 (on ne confirme pas qu'un brouillon existe).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PubliqueServiceImpl implements PubliqueService {

    private final EvenementRepository evenementRepository;
    private final OrganisateurRepository organisateurRepository;
    private final ConcoursRepository concoursRepository;
    private final CandidatRepository candidatRepository;

    @Override
    public PageDto<EvenementResumeDto> evenements(String recherche, String ville, int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 50),
                Sort.by(Sort.Direction.DESC, "dateDebut"));
        Page<Evenement> p = evenementRepository.rechercherPublics(StatutEvenement.PUBLIE, StatutOrganisateur.VALIDE,
                motif(recherche), motif(ville), pr);
        Instant now = Instant.now();
        return PageDto.de(p, p.getContent().stream().map(e -> PubliqueMapper.evenementResume(e, now)).toList());
    }

    @Override
    public EvenementDetailDto evenement(String slug) {
        Evenement e = evenementRepository.findBySlug(slug).filter(this::visible)
                .orElseThrow(() -> ApiException.notFound("Événement introuvable."));
        return PubliqueMapper.evenementDetail(e, Instant.now());
    }

    @Override
    public OrganisateurPublicDto organisateur(String slug) {
        Organisateur o = organisateurRepository.findBySlug(slug)
                .filter(x -> x.getStatut() == StatutOrganisateur.VALIDE)
                .orElseThrow(() -> ApiException.notFound("Organisateur introuvable."));
        Instant now = Instant.now();
        var evenements = evenementRepository.findByOrganisateurAndStatutOrderByDateDebutDesc(o, StatutEvenement.PUBLIE)
                .stream().map(e -> PubliqueMapper.evenementResume(e, now)).toList();
        return new OrganisateurPublicDto(o.getNom(), o.getSlug(), o.getDescription(), o.getLogoUrl(), o.getVille(),
                evenements);
    }

    @Override
    public ConcoursDetailDto concours(Long idConcours) {
        return PubliqueMapper.concoursDetail(concoursVisible(idConcours), Instant.now());
    }

    @Override
    public CandidatPageDto candidat(Long idConcours, Long idCandidat) {
        Candidat x = candidatRepository.findById(idCandidat)
                .filter(Candidat::isActif)
                .filter(c -> c.getCategorie().getConcours().getIdConcours().equals(idConcours))
                .orElseThrow(() -> ApiException.notFound("Candidat introuvable."));
        concoursVisible(idConcours);
        return PubliqueMapper.candidatPage(x, Instant.now());
    }

    // ------------------------------------------------------------------

    private Concours concoursVisible(Long idConcours) {
        return concoursRepository.findById(idConcours)
                .filter(c -> !c.isBrouillon() && visible(c.getEvenement()))
                .orElseThrow(() -> ApiException.notFound("Concours introuvable."));
    }

    private boolean visible(Evenement e) {
        return e.getStatut() == StatutEvenement.PUBLIE && e.getOrganisateur().getStatut() == StatutOrganisateur.VALIDE;
    }

    /** "  Miss " -> "%miss%" ; vide -> "%" (pas de filtre). Les jokers SQL saisis sont neutralisés. */
    private static String motif(String texte) {
        if (texte == null || texte.isBlank()) {
            return "%";
        }
        String t = texte.trim().toLowerCase(Locale.ROOT).replace("%", "").replace("_", "");
        return "%" + (t.length() > 80 ? t.substring(0, 80) : t) + "%";
    }
}
