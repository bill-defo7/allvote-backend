package com.allvote.services.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.allvote.config.AppProperties;
import com.allvote.dtos.vote.VoteRequest;
import com.allvote.entities.Candidat;
import com.allvote.entities.Concours;
import com.allvote.entities.Evenement;
import com.allvote.entities.Organisateur;
import com.allvote.entities.TransactionVote;
import com.allvote.exceptions.ApiException;
import com.allvote.repositories.CandidatRepository;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.ParametreService;
import com.allvote.utiles.StatutConcours;
import com.allvote.utiles.StatutEvenement;
import com.allvote.utiles.StatutOrganisateur;
import com.allvote.utiles.StatutVote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Toutes les écritures en base liées aux votes, chacune dans sa propre transaction SQL courte.
 * (Séparé de VoteServiceImpl pour que les appels HTTP au prestataire ne se fassent JAMAIS
 * à l'intérieur d'une transaction SQL.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteEcritureService {

    private static final BigDecimal CENT = new BigDecimal("100");

    private final CandidatRepository candidatRepository;
    private final TransactionVoteRepository transactionRepository;
    private final ParametreService parametreService;
    private final AppProperties props;

    @Transactional
    public TransactionVote creer(VoteRequest r, String telephone) {
        Candidat x = candidatRepository.findPourVote(r.idCandidat())
                .orElseThrow(() -> ApiException.notFound("Candidat introuvable."));
        Concours c = x.getCategorie().getConcours();
        Evenement e = c.getEvenement();
        Organisateur o = e.getOrganisateur();

        if (!c.getIdConcours().equals(r.idConcours())) {
            throw ApiException.badRequest("Ce candidat n'appartient pas à ce concours.");
        }
        if (c.isBrouillon() || e.getStatut() != StatutEvenement.PUBLIE || o.getStatut() != StatutOrganisateur.VALIDE) {
            throw ApiException.notFound("Concours introuvable.");
        }
        if (c.statutEffectif(Instant.now()) != StatutConcours.OUVERT) {
            throw ApiException.conflict("Le vote n'est pas ouvert pour ce concours.");
        }
        if (!x.isActif()) {
            throw ApiException.conflict("Ce candidat ne participe plus au vote.");
        }
        int max = props.vote().maxVoixParTransaction();
        if (r.nbVoix() > max) {
            throw ApiException.badRequest("Maximum " + max + " voix par paiement.");
        }

        // Tous les montants sont calculés par le SERVEUR (le client n'envoie jamais de prix).
        // Taux FIGÉS maintenant : un changement de paramètre plus tard ne touche pas cette transaction.
        BigDecimal tauxFrais = parametreService.fraisVotantPourcentage();
        int valeurVoix;
        int frais;
        int montant;
        try {
            valeurVoix = Math.multiplyExact(r.nbVoix(), c.getPrixVoix());
            frais = calculerFrais(valeurVoix, tauxFrais);
            montant = Math.addExact(valeurVoix, frais);                     // ce que paie le votant
        } catch (ArithmeticException ex) {
            throw ApiException.badRequest("Montant invalide.");
        }

        // Commission AllVote calculée sur la VALEUR DES VOIX (les frais de service n'entrent pas dans le partage).
        BigDecimal taux = o.getTauxCommission() != null ? o.getTauxCommission() : parametreService.tauxCommissionDefaut();
        int commission = BigDecimal.valueOf(valeurVoix).multiply(taux).divide(CENT, 0, RoundingMode.HALF_UP).intValueExact();

        TransactionVote tx = new TransactionVote();
        tx.setReference(UUID.randomUUID());
        tx.setCandidat(x);
        tx.setConcours(c);
        tx.setOrganisateur(o);
        tx.setNbVoix(r.nbVoix());
        tx.setPrixUnitaire(c.getPrixVoix());
        tx.setMontant(montant);
        tx.setFraisService(frais);
        tx.setTauxFrais(tauxFrais);
        tx.setTauxCommission(taux);
        tx.setMontantCommission(commission);
        tx.setMontantOrganisateur(valeurVoix - commission);
        tx.setOperateur(r.operateur());
        tx.setTelephone(telephone);
        tx.setStatut(StatutVote.EN_ATTENTE);
        return transactionRepository.save(tx);
    }

    /** Frais = taux % de la valeur des voix, arrondi au franc SUPÉRIEUR (ex. 5 % de 150 = 7,5 -> 8). */
    public static int calculerFrais(int valeurVoix, BigDecimal tauxPourcent) {
        if (tauxPourcent == null || tauxPourcent.signum() <= 0) {
            return 0;
        }
        return BigDecimal.valueOf(valeurVoix).multiply(tauxPourcent).divide(CENT, 0, RoundingMode.CEILING).intValueExact();
    }

    @Transactional
    public void enregistrerPayToken(Long id, String payToken, String providerTxnId) {
        transactionRepository.enregistrerPayToken(id, payToken, providerTxnId, Instant.now());
    }

    @Transactional
    public void marquerVerifie(Long id) {
        transactionRepository.marquerVerifie(id, Instant.now());
    }

    /** EN_ATTENTE -> nouveau statut (sans effet si déjà traité). */
    @Transactional
    public boolean changer(Long id, StatutVote nouveau, String message) {
        return transactionRepository.changerStatut(id, StatutVote.EN_ATTENTE, nouveau, tronquer(message),
                Instant.now(), null) == 1;
    }

    /**
     * Confirme le paiement ET crédite les voix dans la MÊME transaction SQL.
     * La mise à jour conditionnelle garantit un crédit unique, même en cas d'appels simultanés.
     */
    @Transactional
    public boolean crediter(Long id, Long idCandidat, int nbVoix) {
        Instant now = Instant.now();
        int n = transactionRepository.changerStatut(id, StatutVote.EN_ATTENTE, StatutVote.CONFIRME,
                "Paiement confirmé", now, now);
        if (n == 1) {
            candidatRepository.ajouterVoix(idCandidat, nbVoix);
            log.info("Vote confirmé : tx={} candidat={} +{} voix", id, idCandidat, nbVoix);
            return true;
        }
        return false;
    }

    private static String tronquer(String s) {
        return s == null ? null : (s.length() > 250 ? s.substring(0, 250) : s);
    }
}
