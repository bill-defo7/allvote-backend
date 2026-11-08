package com.allvote.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.admin.PaiementReversementRequest;
import com.allvote.dtos.admin.ReversementDto;
import com.allvote.dtos.admin.ReversementRequest;
import com.allvote.dtos.admin.SoldeOrganisateurDto;
import com.allvote.dtos.commun.PageDto;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.AdminReversementService;
import com.allvote.utiles.StatutReversement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REVERSEMENT_GERER')")
public class AdminReversementController {

    private final AdminReversementService reversementService;

    /** Ce que AllVote doit à chaque organisateur. */
    @GetMapping("/soldes")
    public List<SoldeOrganisateurDto> soldes() {
        return reversementService.soldes();
    }

    @PostMapping("/organisateurs/{idOrganisateur}/reversements")
    @ResponseStatus(HttpStatus.CREATED)
    public ReversementDto creer(@PathVariable Long idOrganisateur, @Valid @RequestBody(required = false) ReversementRequest request) {
        return reversementService.creer(idOrganisateur, UtilisateurConnecte.idUtilisateur(), request);
    }

    /** ?statut=A_PAYER&idOrganisateur=1 */
    @GetMapping("/reversements")
    public PageDto<ReversementDto> lister(@RequestParam(required = false) StatutReversement statut,
            @RequestParam(required = false) Long idOrganisateur,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int taille) {
        return reversementService.lister(statut, idOrganisateur, page, taille);
    }

    /** {"referencePaiement":"MP2611...","note":"..."} */
    @PostMapping("/reversements/{id}/payer")
    public ReversementDto payer(@PathVariable Long id, @Valid @RequestBody PaiementReversementRequest request) {
        return reversementService.marquerPaye(id, UtilisateurConnecte.idUtilisateur(), request);
    }

    @DeleteMapping("/reversements/{id}")
    public ResponseEntity<Void> annuler(@PathVariable Long id) {
        reversementService.annuler(id);
        return ResponseEntity.noContent().build();
    }
}
