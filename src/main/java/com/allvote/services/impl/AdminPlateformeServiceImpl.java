package com.allvote.services.impl;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.admin.EvenementAdminDto;
import com.allvote.dtos.admin.TableauBordDto;
import com.allvote.dtos.admin.UtilisateurAdminDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.StatutEvenementRequest;
import com.allvote.entities.Evenement;
import com.allvote.entities.Utilisateur;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.AdminMapper;
import com.allvote.repositories.EvenementRepository;
import com.allvote.repositories.OrganisateurRepository;
import com.allvote.repositories.ReversementRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.services.AdminPlateformeService;
import com.allvote.services.ParametreService;
import com.allvote.utiles.Roles;
import com.allvote.utiles.StatutEvenement;
import com.allvote.utiles.StatutOrganisateur;
import com.allvote.utiles.StatutReversement;
import com.allvote.utiles.StatutVote;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPlateformeServiceImpl implements AdminPlateformeService {

    private final OrganisateurRepository organisateurRepository;
    private final EvenementRepository evenementRepository;
    private final TransactionVoteRepository transactionRepository;
    private final ReversementRepository reversementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreService parametreService;

    @Override
    @Transactional(readOnly = true)
    public TableauBordDto tableauBord() {
        Object[] t = transactionRepository.totaux(StatutVote.CONFIRME).get(0);
        return new TableauBordDto(
                organisateurRepository.countByStatut(StatutOrganisateur.EN_ATTENTE),
                organisateurRepository.countByStatut(StatutOrganisateur.VALIDE),
                organisateurRepository.countByStatut(StatutOrganisateur.SUSPENDU),
                evenementRepository.countByStatut(StatutEvenement.PUBLIE),
                n(t[0]), n(t[1]), n(t[2]), n(t[3]),
                n(t[4]), n(t[2]) + n(t[4]),
                transactionRepository.nonReverseGlobal(StatutVote.CONFIRME),
                reversementRepository.sommeGlobale(StatutReversement.A_PAYER),
                reversementRepository.sommeGlobale(StatutReversement.PAYE),
                transactionRepository.countByStatut(StatutVote.A_VERIFIER),
                transactionRepository.countByStatut(StatutVote.EN_ATTENTE),
                parametreService.tauxCommissionDefaut(),
                parametreService.fraisVotantPourcentage());
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<EvenementAdminDto> evenements(StatutEvenement statut, Long idOrganisateur, int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Page<Evenement> p;
        if (idOrganisateur != null && statut != null) {
            p = evenementRepository.findByOrganisateur_IdOrganisateurAndStatut(idOrganisateur, statut, pr);
        } else if (idOrganisateur != null) {
            p = evenementRepository.findByOrganisateur_IdOrganisateur(idOrganisateur, pr);
        } else if (statut != null) {
            p = evenementRepository.findByStatut(statut, pr);
        } else {
            p = evenementRepository.findAllBy(pr);
        }
        return PageDto.de(p, p.getContent().stream().map(AdminMapper::evenement).toList());
    }

    /** Modération : le super-admin peut retirer (BROUILLON) ou archiver un événement, pas le publier à la place de l'organisateur. */
    @Override
    public EvenementAdminDto changerStatutEvenement(Long idEvenement, StatutEvenementRequest r) {
        if (r.statut() == StatutEvenement.PUBLIE) {
            throw ApiException.badRequest("La publication appartient à l'organisateur.");
        }
        Evenement e = evenementRepository.findById(idEvenement)
                .orElseThrow(() -> ApiException.notFound("Événement introuvable."));
        e.setStatut(r.statut());
        return AdminMapper.evenement(e);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<UtilisateurAdminDto> utilisateurs(String recherche, int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Page<Utilisateur> p = recherche == null || recherche.isBlank()
                ? utilisateurRepository.findAllBy(pr)
                : utilisateurRepository.findByEmailContainingIgnoreCaseOrNomContainingIgnoreCase(recherche.trim(), recherche.trim(), pr);
        Instant now = Instant.now();
        return PageDto.de(p, p.getContent().stream().map(u -> AdminMapper.utilisateur(u, now)).toList());
    }

    @Override
    public UtilisateurAdminDto activerUtilisateur(Long idUtilisateur, Long idSuperAdmin, ActifRequest r) {
        if (idUtilisateur.equals(idSuperAdmin)) {
            throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte.");
        }
        Utilisateur u = charger(idUtilisateur);
        if (u.getRoles().stream().anyMatch(x -> Roles.SUPER_ADMIN.equals(x.getNomRole()))) {
            throw ApiException.forbidden("Un compte super-admin ne peut pas être désactivé ici.");
        }
        u.setActif(r.actif());
        return AdminMapper.utilisateur(u, Instant.now());
    }

    @Override
    public UtilisateurAdminDto deverrouiller(Long idUtilisateur) {
        Utilisateur u = charger(idUtilisateur);
        u.setTentativesEchouees(0);
        u.setVerrouilleJusqua(null);
        return AdminMapper.utilisateur(u, Instant.now());
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable."));
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}
