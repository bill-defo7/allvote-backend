package com.allvote.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.orga.ActifRequest;
import com.allvote.dtos.orga.MembreDto;
import com.allvote.dtos.orga.MembreRequest;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.OrgaMembreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orga/membres")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MEMBRE_GERER')")
public class OrgaMembreController {

    private final OrgaMembreService membreService;

    @GetMapping
    public List<MembreDto> lister() {
        return membreService.lister(UtilisateurConnecte.idOrganisateur());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembreDto ajouter(@Valid @RequestBody MembreRequest request) {
        return membreService.ajouter(UtilisateurConnecte.idOrganisateur(), request);
    }

    /** {"actif":false} */
    @PatchMapping("/{idMembre}/actif")
    public MembreDto activer(@PathVariable Long idMembre, @Valid @RequestBody ActifRequest request) {
        return membreService.activer(UtilisateurConnecte.idOrganisateur(), UtilisateurConnecte.idUtilisateur(),
                idMembre, request);
    }

    @DeleteMapping("/{idMembre}")
    public ResponseEntity<Void> supprimer(@PathVariable Long idMembre) {
        membreService.supprimer(UtilisateurConnecte.idOrganisateur(), UtilisateurConnecte.idUtilisateur(), idMembre);
        return ResponseEntity.noContent().build();
    }
}
