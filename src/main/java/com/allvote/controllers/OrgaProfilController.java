package com.allvote.controllers;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.allvote.dtos.orga.ProfilOrganisateurDto;
import com.allvote.dtos.orga.ProfilOrganisateurRequest;
import com.allvote.dtos.orga.UploadResponse;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.OrgaProfilService;
import com.allvote.services.StockageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Profil de l'organisation + téléversement d'images. L'organisation vient TOUJOURS du jeton. */
@RestController
@RequestMapping("/api/orga")
@RequiredArgsConstructor
public class OrgaProfilController {

    private final OrgaProfilService profilService;
    private final StockageService stockageService;

    @GetMapping("/profil")
    public ProfilOrganisateurDto profil() {
        return profilService.profil(UtilisateurConnecte.idOrganisateur());
    }

    @PutMapping("/profil")
    @PreAuthorize("hasAuthority('PROFIL_GERER')")
    public ProfilOrganisateurDto modifier(@Valid @RequestBody ProfilOrganisateurRequest request) {
        return profilService.modifier(UtilisateurConnecte.idOrganisateur(), request);
    }

    /** Logo, affiche ou photo. Renvoie {"url":"/uploads/xxx.jpg"} à mettre dans logoUrl / afficheUrl / photoUrl. */
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("fichier") MultipartFile fichier) {
        UtilisateurConnecte.idOrganisateur();   // refuse un compte sans organisation
        return new UploadResponse(stockageService.stockerImage(fichier));
    }
}
