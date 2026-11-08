package com.allvote.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.admin.MotifRequest;
import com.allvote.dtos.admin.TransactionAdminDto;
import com.allvote.dtos.admin.VerificationPrestataireDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.AdminTransactionService;
import com.allvote.utiles.StatutVote;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('TRANSACTION_VOIR')")
public class AdminTransactionController {

    private final AdminTransactionService transactionService;

    /** ?statut=A_VERIFIER&idOrganisateur=1&idConcours=2&page=0&taille=20 */
    @GetMapping
    public PageDto<TransactionAdminDto> lister(@RequestParam(required = false) StatutVote statut,
            @RequestParam(required = false) Long idOrganisateur, @RequestParam(required = false) Long idConcours,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        return transactionService.lister(statut, idOrganisateur, idConcours, page, taille);
    }

    @GetMapping("/{id}")
    public TransactionAdminDto detail(@PathVariable Long id) {
        return transactionService.detail(id);
    }

    @GetMapping("/{id}/prestataire")
    public VerificationPrestataireDto prestataire(@PathVariable Long id) {
        return transactionService.verifierPrestataire(id);
    }

    @PostMapping("/{id}/confirmer")
    @PreAuthorize("hasAuthority('REVERSEMENT_GERER')")
    public TransactionAdminDto confirmer(@PathVariable Long id, @Valid @RequestBody MotifRequest request) {
        return transactionService.confirmer(id, UtilisateurConnecte.idUtilisateur(), request);
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('REVERSEMENT_GERER')")
    public TransactionAdminDto rejeter(@PathVariable Long id, @Valid @RequestBody MotifRequest request) {
        return transactionService.rejeter(id, UtilisateurConnecte.idUtilisateur(), request);
    }
}
