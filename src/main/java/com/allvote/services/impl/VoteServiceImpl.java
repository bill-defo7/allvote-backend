package com.allvote.services.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.allvote.config.AppProperties;
import com.allvote.dtos.vote.VoteRequest;
import com.allvote.dtos.vote.VoteResponse;
import com.allvote.dtos.vote.VoteStatutDto;
import com.allvote.entities.TransactionVote;
import com.allvote.exceptions.ApiException;
import com.allvote.paiement.PasserellePaiement;
import com.allvote.paiement.PasserellePaiement.ResultatInitiation;
import com.allvote.paiement.PasserellePaiement.ResultatVerification;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.VoteService;
import com.allvote.utiles.StatutVote;
import com.allvote.utiles.Telephones;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteServiceImpl implements VoteService {

    /** Intervalle minimal entre deux vérifications d'une même transaction. */
    private static final Duration ANTI_REBOND = Duration.ofSeconds(5);

    private final TransactionVoteRepository transactionRepository;
    private final VoteEcritureService ecritures;
    private final PasserellePaiement passerelle;
    private final AppProperties props;

    @Override
    public VoteResponse creer(VoteRequest r) {
        if (r.website() != null && !r.website().isBlank()) {
            throw ApiException.badRequest("Requête invalide.");   // robot détecté (honeypot)
        }
        String tel = Telephones.formatInternational(r.telephone());

        TransactionVote tx = ecritures.creer(r, tel);

        ResultatInitiation init;
        try {
            init = passerelle.initier(tx);
        } catch (RuntimeException e) {
            ecritures.changer(tx.getIdTransaction(), StatutVote.ECHOUE, "Prestataire de paiement injoignable");
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Le service de paiement est momentanément indisponible. Réessayez dans un instant.");
        }
        if (!init.ok()) {
            ecritures.changer(tx.getIdTransaction(), StatutVote.ECHOUE, init.message());
            throw new ApiException(HttpStatus.BAD_GATEWAY, init.message());
        }
        ecritures.enregistrerPayToken(tx.getIdTransaction(), init.payToken(), init.providerTxnId());

        return new VoteResponse(tx.getReference(), StatutVote.EN_ATTENTE, tx.getNbVoix(), tx.getMontant(),
                "Validez le paiement de " + tx.getMontant() + " FCFA sur votre téléphone.",
                tx.getMontant() - tx.getFraisService(), tx.getFraisService());
    }

    @Override
    public VoteStatutDto statut(UUID reference) {
        TransactionVote tx = transactionRepository.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("Vote introuvable."));
        if (tx.getStatut() == StatutVote.EN_ATTENTE && peutVerifier(tx)) {
            verifier(reference);
            tx = transactionRepository.findByReference(reference).orElse(tx);
        }
        return new VoteStatutDto(tx.getReference(), tx.getStatut(), tx.getNbVoix(), tx.getMontant(),
                tx.getCandidat().getNom(), tx.getMessage());
    }

    /** Ne fait JAMAIS confiance au contenu d'un webhook : seule la réponse de check_status compte. */
    @Override
    public void verifier(UUID reference) {
        TransactionVote tx = transactionRepository.findByReference(reference).orElse(null);
        if (tx == null || tx.getStatut() != StatutVote.EN_ATTENTE) {
            return;
        }
        boolean expire = Instant.now().isAfter(
                tx.getDateCreation().plus(Duration.ofMinutes(props.vote().expirationMinutes())));

        if (tx.getPayToken() == null) {
            if (expire) {
                ecritures.changer(tx.getIdTransaction(), StatutVote.EXPIRE, "Paiement non initié");
            }
            return;
        }

        ecritures.marquerVerifie(tx.getIdTransaction());
        ResultatVerification r;
        try {
            r = passerelle.verifier(tx);
        } catch (RuntimeException e) {
            log.warn("Vérification impossible pour {} : {}", reference, e.getMessage());
            return;   // on réessaiera au prochain passage
        }

        switch (r.etat()) {
            case REUSSI -> {
                if (r.montant() != null && r.montant() != tx.getMontant()) {
                    log.error("Montant incohérent pour {} : reçu {} / attendu {}", reference, r.montant(), tx.getMontant());
                    ecritures.changer(tx.getIdTransaction(), StatutVote.A_VERIFIER,
                            "Montant reçu " + r.montant() + " différent de l'attendu " + tx.getMontant());
                } else {
                    ecritures.crediter(tx.getIdTransaction(), tx.getCandidat().getIdCandidat(), tx.getNbVoix());
                }
            }
            case ECHOUE -> ecritures.changer(tx.getIdTransaction(), StatutVote.ECHOUE,
                    r.message() != null ? r.message() : "Paiement refusé ou annulé");
            default -> {
                if (expire) {
                    ecritures.changer(tx.getIdTransaction(), StatutVote.EXPIRE, "Aucune validation dans le délai");
                }
            }
        }
    }

    private boolean peutVerifier(TransactionVote tx) {
        Instant now = Instant.now();
        boolean assezAncien = tx.getDateCreation().plus(ANTI_REBOND).isBefore(now);
        boolean pasRecent = tx.getDerniereVerification() == null
                || tx.getDerniereVerification().plus(ANTI_REBOND).isBefore(now);
        return assezAncien && pasRecent;
    }
}
