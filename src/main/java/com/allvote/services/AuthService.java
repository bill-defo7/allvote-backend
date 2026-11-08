package com.allvote.services;

import com.allvote.dtos.auth.ChangerMotDePasseRequest;
import com.allvote.dtos.auth.LoginRequest;
import com.allvote.dtos.auth.LoginResponse;
import com.allvote.dtos.auth.UtilisateurDto;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UtilisateurDto moi(Long idUtilisateur);

    void changerMotDePasse(Long idUtilisateur, ChangerMotDePasseRequest request);
}
