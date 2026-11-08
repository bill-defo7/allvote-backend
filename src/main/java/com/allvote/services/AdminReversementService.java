package com.allvote.services;

import java.util.List;

import com.allvote.dtos.admin.PaiementReversementRequest;
import com.allvote.dtos.admin.ReversementDto;
import com.allvote.dtos.admin.ReversementRequest;
import com.allvote.dtos.admin.SoldeOrganisateurDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.utiles.StatutReversement;

/** Reversement = regrouper la part due à un organisateur (A_PAYER), puis noter le paiement Mobile Money (PAYE). */
public interface AdminReversementService {

    List<SoldeOrganisateurDto> soldes();

    ReversementDto creer(Long idOrganisateur, Long idSuperAdmin, ReversementRequest request);

    ReversementDto marquerPaye(Long idReversement, Long idSuperAdmin, PaiementReversementRequest request);

    void annuler(Long idReversement);

    PageDto<ReversementDto> lister(StatutReversement statut, Long idOrganisateur, int page, int taille);
}
