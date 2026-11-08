package com.allvote.services.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.admin.PaiementReversementRequest;
import com.allvote.dtos.admin.ReversementDto;
import com.allvote.dtos.admin.ReversementRequest;
import com.allvote.dtos.admin.SoldeOrganisateurDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.entities.Organisateur;
import com.allvote.entities.Reversement;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.AdminMapper;
import com.allvote.repositories.OrganisateurRepository;
import com.allvote.repositories.ReversementRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.services.AdminReversementService;
import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutReversement;
import com.allvote.utiles.StatutVote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 1. creer()       : toutes les transactions CONFIRMÉES non encore reversées de l'organisateur sont
 *                    rattachées au reversement (A_PAYER) en UNE requête -> impossible de payer deux fois.
 * 2. marquerPaye() : après l'envoi Mobile Money, on note la référence -> PAYE.
 *    annuler()      : (A_PAYER uniquement) libère les transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminReversementServiceImpl implements AdminReversementService {

    private final ReversementRepository reversementRepository;
    private final TransactionVoteRepository transactionRepository;
    private final OrganisateurRepository organisateurRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SoldeOrganisateurDto> soldes() {
        return transactionRepository.soldes(StatutVote.CONFIRME).stream()
                .map(l -> new SoldeOrganisateurDto(((Number) l[0]).longValue(), (String) l[1], (String) l[2],
                        (String) l[3], (Operateur) l[4], ((Number) l[5]).longValue(), ((Number) l[6]).longValue()))
                .toList();
    }

    @Override
    public ReversementDto creer(Long idOrganisateur, Long idSuperAdmin, ReversementRequest r) {
        Organisateur o = organisateurRepository.findById(idOrganisateur)
                .orElseThrow(() -> ApiException.notFound("Organisation introuvable."));
        if (o.getNumeroReversement() == null) {
            throw ApiException.badRequest("L'organisateur n'a pas renseigné son numéro Mobile Money de reversement.");
        }
        if (reversementRepository.existsByOrganisateur_IdOrganisateurAndStatut(idOrganisateur, StatutReversement.A_PAYER)) {
            throw ApiException.conflict("Un reversement est déjà en attente de paiement pour cette organisation.");
        }
        Reversement rv = new Reversement();
        rv.setOrganisateur(o);
        rv.setStatut(StatutReversement.A_PAYER);
        rv.setNote(vide(r == null ? null : r.note()));
        rv.setEffectuePar(utilisateurRepository.getReferenceById(idSuperAdmin));
        rv = reversementRepository.saveAndFlush(rv);

        int n = transactionRepository.attacher(rv, idOrganisateur, StatutVote.CONFIRME);
        if (n == 0) {
            throw ApiException.badRequest("Rien à reverser : aucun paiement confirmé en attente pour cette organisation.");
        }
        Object[] t = transactionRepository.totauxReversement(rv).get(0);
        rv.setNbTransactions(((Number) t[0]).intValue());
        rv.setMontant(((Number) t[1]).longValue());
        rv.setPeriodeDebut((Instant) t[2]);
        rv.setPeriodeFin((Instant) t[3]);
        log.info("Reversement {} créé pour {} : {} FCFA ({} transactions)", rv.getIdReversement(), o.getSlug(),
                rv.getMontant(), rv.getNbTransactions());
        return AdminMapper.reversement(rv);
    }

    @Override
    public ReversementDto marquerPaye(Long idReversement, Long idSuperAdmin, PaiementReversementRequest r) {
        Reversement rv = charger(idReversement);
        if (rv.getStatut() != StatutReversement.A_PAYER) {
            throw ApiException.conflict("Ce reversement est déjà payé.");
        }
        rv.setStatut(StatutReversement.PAYE);
        rv.setReferencePaiement(r.referencePaiement().trim());
        rv.setDatePaiement(Instant.now());
        rv.setEffectuePar(utilisateurRepository.getReferenceById(idSuperAdmin));
        if (r.note() != null && !r.note().isBlank()) {
            rv.setNote(r.note().trim());
        }
        log.info("Reversement {} PAYÉ ({} FCFA, réf. {})", idReversement, rv.getMontant(), rv.getReferencePaiement());
        return AdminMapper.reversement(rv);
    }

    @Override
    public void annuler(Long idReversement) {
        Reversement rv = charger(idReversement);
        if (rv.getStatut() != StatutReversement.A_PAYER) {
            throw ApiException.conflict("Un reversement payé ne peut pas être annulé.");
        }
        transactionRepository.detacher(rv);
        reversementRepository.delete(rv);
        log.info("Reversement {} annulé", idReversement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<ReversementDto> lister(StatutReversement statut, Long idOrganisateur, int page, int taille) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "idReversement"));
        Page<Reversement> p;
        if (statut != null && idOrganisateur != null) {
            p = reversementRepository.findByOrganisateur_IdOrganisateurAndStatut(idOrganisateur, statut, pr);
        } else if (statut != null) {
            p = reversementRepository.findByStatut(statut, pr);
        } else if (idOrganisateur != null) {
            p = reversementRepository.findByOrganisateur_IdOrganisateur(idOrganisateur, pr);
        } else {
            p = reversementRepository.findAllBy(pr);
        }
        return PageDto.de(p, p.getContent().stream().map(AdminMapper::reversement).toList());
    }

    private Reversement charger(Long id) {
        return reversementRepository.findById(id).orElseThrow(() -> ApiException.notFound("Reversement introuvable."));
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
