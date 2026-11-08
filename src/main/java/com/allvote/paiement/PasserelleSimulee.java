package com.allvote.paiement;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.allvote.entities.TransactionVote;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Paiement SIMULÉ — aucun argent ne circule.
 * - Le paiement est « validé » ~8 secondes après le vote (le temps de taper son code).
 * - Un numéro qui se termine par 000 simule un paiement REFUSÉ (pour tester les échecs).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "simulated", matchIfMissing = true)
public class PasserelleSimulee implements PasserellePaiement {

    private static final Duration DELAI_VALIDATION = Duration.ofSeconds(8);

    @PostConstruct
    void avertir() {
        log.warn("MODE PAIEMENT SIMULÉ : aucun paiement réel (ALLVOTE_PAYMENT_MODE=simulated).");
    }

    @Override
    public ResultatInitiation initier(TransactionVote tx) {
        return new ResultatInitiation(true, "SIM-" + UUID.randomUUID(), "SIM", "Paiement simulé initié");
    }

    @Override
    public ResultatVerification verifier(TransactionVote tx) {
        if (Duration.between(tx.getDateCreation(), Instant.now()).compareTo(DELAI_VALIDATION) < 0) {
            return new ResultatVerification(EtatPaiement.EN_COURS, null, "En attente (simulation)");
        }
        if (tx.getTelephone().endsWith("000")) {
            return new ResultatVerification(EtatPaiement.ECHOUE, null, "Paiement refusé (simulation)");
        }
        return new ResultatVerification(EtatPaiement.REUSSI, (long) tx.getMontant(), "Paiement réussi (simulation)");
    }
}
