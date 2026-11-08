package com.allvote.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.orga.FinancesOrgaDto;
import com.allvote.dtos.orga.TransactionOrgaDto;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.OrgaFinanceService;
import com.allvote.utiles.StatutVote;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orga")
@RequiredArgsConstructor
public class OrgaFinanceController {

    private final OrgaFinanceService financeService;

    /** ?statut=CONFIRME&idConcours=4&page=0&taille=20 */
    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('TRANSACTION_VOIR')")
    public PageDto<TransactionOrgaDto> transactions(@RequestParam(required = false) StatutVote statut,
            @RequestParam(required = false) Long idConcours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int taille) {
        return financeService.transactions(UtilisateurConnecte.idOrganisateur(), statut, idConcours, page, taille);
    }

    @GetMapping("/finances")
    @PreAuthorize("hasAuthority('FINANCE_VOIR')")
    public FinancesOrgaDto finances() {
        return financeService.finances(UtilisateurConnecte.idOrganisateur());
    }
}
