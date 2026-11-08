package com.allvote.services;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.FinancesOrgaDto;
import com.allvote.dtos.orga.TransactionOrgaDto;
import com.allvote.utiles.StatutVote;

public interface OrgaFinanceService {

    PageDto<TransactionOrgaDto> transactions(Long idOrganisateur, StatutVote statut, Long idConcours, int page, int taille);

    FinancesOrgaDto finances(Long idOrganisateur);
}
