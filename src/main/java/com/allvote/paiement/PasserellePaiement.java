package com.allvote.paiement;

import com.allvote.entities.TransactionVote;

/**
 * Abstraction du prestataire de paiement. Deux implémentations :
 * PasserelleSimulee (tests, aucun argent) et PasserelleAangaraa (réel).
 * Choix via ALLVOTE_PAYMENT_MODE = simulated | aangaraa.
 */
public interface PasserellePaiement {

    /** Lance le paiement (invite USSD envoyée au téléphone du votant). */
    ResultatInitiation initier(TransactionVote tx);

    /** Interroge le prestataire sur l'état réel du paiement (source de vérité). */
    ResultatVerification verifier(TransactionVote tx);

    enum EtatPaiement { REUSSI, EN_COURS, ECHOUE, INCONNU }

    record ResultatInitiation(boolean ok, String payToken, String providerTxnId, String message) {
        public static ResultatInitiation echec(String message) {
            return new ResultatInitiation(false, null, null, message);
        }
    }

    /** montant = montant réellement payé selon le prestataire (null si inconnu). */
    record ResultatVerification(EtatPaiement etat, Long montant, String message) {
    }
}
