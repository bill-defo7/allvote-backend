package com.allvote.paiement;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.allvote.entities.TransactionVote;
import com.allvote.repositories.TransactionVoteRepository;
import com.allvote.services.VoteService;
import com.allvote.utiles.StatutVote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filet de sécurité : vérifie régulièrement les paiements encore EN_ATTENTE.
 * Fonctionne même si le webhook n'arrive jamais (ex. en local, sans URL publique).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationPaiements {

    private final TransactionVoteRepository transactionRepository;
    private final VoteService voteService;

    @Scheduled(initialDelay = 20_000, fixedDelayString = "${app.vote.reconciliation-delay-ms:30000}")
    public void reconcilier() {
        List<TransactionVote> enAttente = transactionRepository.findTop100ByStatutOrderByDateCreationAsc(StatutVote.EN_ATTENTE);
        if (enAttente.isEmpty()) {
            return;
        }
        Instant seuil = Instant.now().minusSeconds(10);
        int n = 0;
        for (TransactionVote tx : enAttente) {
            if (tx.getDerniereVerification() != null && tx.getDerniereVerification().isAfter(seuil)) {
                continue; // vérifiée il y a moins de 10 s
            }
            try {
                voteService.verifier(tx.getReference());
                n++;
            } catch (RuntimeException e) {
                log.warn("Réconciliation échouée pour {} : {}", tx.getReference(), e.getMessage());
            }
        }
        if (n > 0) {
            log.debug("Réconciliation : {} paiement(s) vérifié(s)", n);
        }
    }
}
