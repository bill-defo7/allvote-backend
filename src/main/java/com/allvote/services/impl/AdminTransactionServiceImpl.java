package com.allvote.services.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.TransactionAdminDto;
import com.allvote.dtos.admin.VerificationPrestataireDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.entities.TransactionVote;
import com.allvote.exceptions.ApiException;
import com.allvote.mappers.AdminMapper;
import com.allvote.paiement.PasserellePaiement;
import com.allvote.repositories.CandidatRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.services.AdminTransactionService;
import com.allvote.utiles.StatutVote;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Traitement manuel des cas litigieux :
 *  - A_VERIFIER : le prestataire a confirmé un montant différent de l'attendu ;
 *  - EXPIRE     : aucune confirmation dans le délai, mais le votant affirme avoir payé.
 * Le super-admin vérifie (bouton « interroger le prestataire »), puis confirme ou rejette, avec un motif.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTransactionServiceImpl implements AdminTransactionService {

    private final TransactionVoteRepository transactionRepository;
    private final CandidatRepository candidatRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasserellePaiement passerelle;

    @Override
    @Transactional(readOnly = true)
    public PageDto<TransactionAdminDto> lister(StatutVote statut, Long idOrganisateur, Long idConcours, int page, int taille) {
        Specification<TransactionVote> filtre = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (statut != null) {
                p.add(cb.equal(root.get("statut"), statut));
            }
            if (idOrganisateur != null) {
                p.add(cb.equal(root.get("organisateur").get("idOrganisateur"), idOrganisateur));
            }
            if (idConcours != null) {
                p.add(cb.equal(root.get("concours").get("idConcours"), idConcours));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(taille, 1), 100),
                Sort.by(Sort.Direction.DESC, "dateCreation"));
        Page<TransactionVote> res = transactionRepository.findAll(filtre, pr);
        return PageDto.de(res, res.getContent().stream().map(AdminMapper::transaction).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionAdminDto detail(Long id) {
        return AdminMapper.transaction(charger(id));
    }

    /** Volontairement HORS transaction SQL : appel HTTP au prestataire. */
    @Override
    public VerificationPrestataireDto verifierPrestataire(Long id) {
        TransactionVote tx = transactionRepository.findWithCandidatByIdTransaction(id)
                .orElseThrow(() -> ApiException.notFound("Transaction introuvable."));
        if (tx.getPayToken() == null) {
            throw ApiException.badRequest("Ce paiement n'a jamais été initié chez le prestataire.");
        }
        PasserellePaiement.ResultatVerification r = passerelle.verifier(tx);
        return new VerificationPrestataireDto(r.etat().name(), r.montant(), tx.getMontant(), r.message());
    }

    @Override
    @Transactional
    public TransactionAdminDto confirmer(Long id, Long idSuperAdmin, MotifRequest r) {
        TransactionVote tx = resoluble(id);
        Long idCandidat = tx.getCandidat().getIdCandidat();
        int nbVoix = tx.getNbVoix();
        Instant now = Instant.now();
        int n = transactionRepository.resoudre(id, tx.getStatut(), StatutVote.CONFIRME,
                tronquer("Confirmé manuellement : " + r.motif().trim()), now, now,
                utilisateurRepository.getReferenceById(idSuperAdmin));
        if (n != 1) {
            throw ApiException.conflict("La transaction a changé de statut entre-temps. Rechargez la page.");
        }
        candidatRepository.ajouterVoix(idCandidat, nbVoix);
        log.warn("Transaction {} CONFIRMÉE manuellement par l'utilisateur {} (+{} voix au candidat {})",
                id, idSuperAdmin, nbVoix, idCandidat);
        return AdminMapper.transaction(charger(id));
    }

    @Override
    @Transactional
    public TransactionAdminDto rejeter(Long id, Long idSuperAdmin, MotifRequest r) {
        TransactionVote tx = resoluble(id);
        int n = transactionRepository.resoudre(id, tx.getStatut(), StatutVote.ECHOUE,
                tronquer("Rejeté manuellement : " + r.motif().trim()), Instant.now(), null,
                utilisateurRepository.getReferenceById(idSuperAdmin));
        if (n != 1) {
            throw ApiException.conflict("La transaction a changé de statut entre-temps. Rechargez la page.");
        }
        log.warn("Transaction {} REJETÉE manuellement par l'utilisateur {}", id, idSuperAdmin);
        return AdminMapper.transaction(charger(id));
    }

    private TransactionVote resoluble(Long id) {
        TransactionVote tx = charger(id);
        if (tx.getStatut() != StatutVote.A_VERIFIER && tx.getStatut() != StatutVote.EXPIRE) {
            throw ApiException.conflict("Seule une transaction A_VERIFIER ou EXPIRE peut être traitée manuellement "
                    + "(statut actuel : " + tx.getStatut() + ").");
        }
        return tx;
    }

    private TransactionVote charger(Long id) {
        return transactionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Transaction introuvable."));
    }

    private static String tronquer(String s) {
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}
