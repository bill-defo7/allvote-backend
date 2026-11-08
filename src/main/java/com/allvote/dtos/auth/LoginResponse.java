package com.allvote.dtos.auth;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UtilisateurDto utilisateur) {
}
