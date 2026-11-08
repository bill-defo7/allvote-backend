package com.allvote.services;

import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.TransactionAdminDto;
import com.allvote.dtos.admin.VerificationPrestataireDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.utiles.StatutVote;

public interface AdminTransactionService {

    PageDto<TransactionAdminDto> lister(StatutVote statut, Long idOrganisateur, Long idConcours, int page, int taille);

    TransactionAdminDto detail(Long idTransaction);

    /** Interroge le prestataire, sans rien modifier (aide à la décision). */
    VerificationPrestataireDto verifierPrestataire(Long idTransaction);

    /** A_VERIFIER ou EXPIRE -> CONFIRME : crédite les voix une seule fois. */
    TransactionAdminDto confirmer(Long idTransaction, Long idSuperAdmin, MotifRequest request);

    /** A_VERIFIER ou EXPIRE -> ECHOUE. */
    TransactionAdminDto rejeter(Long idTransaction, Long idSuperAdmin, MotifRequest request);
}
