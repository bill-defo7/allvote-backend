package com.allvote.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.allvote.dtos.auth.ChangerMotDePasseRequest;
import com.allvote.dtos.auth.LoginRequest;
import com.allvote.dtos.auth.LoginResponse;
import com.allvote.dtos.auth.UtilisateurDto;
import com.allvote.security.UtilisateurConnecte;
import com.allvote.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Connexion commune : super-admin, responsables et membres d'organisation. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/moi")
    public UtilisateurDto moi() {
        return authService.moi(UtilisateurConnecte.idUtilisateur());
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangerMotDePasseRequest request) {
        authService.changerMotDePasse(UtilisateurConnecte.idUtilisateur(), request);
        return ResponseEntity.noContent().build();
    }
}
