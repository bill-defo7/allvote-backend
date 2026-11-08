package com.allvote.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.commun.PageDto;
import com.allvote.dtos.organisateur.InscriptionOrganisateurRequest;
import com.allvote.dtos.organisateur.InscriptionResponse;
import com.allvote.dtos.publique.*;
import com.allvote.services.InscriptionService;
import com.allvote.services.ParametreService;
import com.allvote.services.PubliqueService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API ouverte à tous (sans connexion). */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PubliqueController {

    private final PubliqueService publiqueService;
    private final InscriptionService inscriptionService;
    private final ParametreService parametreService;

    /** Accueil / recherche : ?q=miss&ville=ambam&page=0&taille=12 */
    @GetMapping("/evenements")
    public PageDto<EvenementResumeDto> evenements(@RequestParam(required = false) String q,
            @RequestParam(required = false) String ville,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int taille) {
        return publiqueService.evenements(q, ville, page, taille);
    }

    @GetMapping("/evenements/{slug}")
    public EvenementDetailDto evenement(@PathVariable String slug) {
        return publiqueService.evenement(slug);
    }

    @GetMapping("/organisateurs/{slug}")
    public OrganisateurPublicDto organisateur(@PathVariable String slug) {
        return publiqueService.organisateur(slug);
    }

    @GetMapping("/concours/{idConcours}")
    public ConcoursDetailDto concours(@PathVariable Long idConcours) {
        return publiqueService.concours(idConcours);
    }

    @GetMapping("/concours/{idConcours}/candidats/{idCandidat}")
    public CandidatPageDto candidat(@PathVariable Long idConcours, @PathVariable Long idCandidat) {
        return publiqueService.candidat(idConcours, idCandidat);
    }

    /** Frais de service affichés dans la fenêtre de vote avant paiement. */
    @GetMapping("/tarifs")
    public TarifsDto tarifs() {
        return new TarifsDto(parametreService.fraisVotantPourcentage());
    }

    @PostMapping("/organisateurs/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public InscriptionResponse inscription(@Valid @RequestBody InscriptionOrganisateurRequest request) {
        return inscriptionService.inscrire(request);
    }
}
