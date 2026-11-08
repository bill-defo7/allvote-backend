package com.allvote.dtos.vote;

import com.allvote.utiles.Operateur;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Demande de vote envoyée par le front.
 * Le PRIX n'est jamais envoyé par le client : il est recalculé côté serveur.
 * "website" est un champ piège (honeypot) : un humain le laisse vide, un robot le remplit.
 */
public record VoteRequest(
        @NotNull(message = "Concours obligatoire") Long idConcours,
        @NotNull(message = "Candidat obligatoire") Long idCandidat,
        @NotNull(message = "Nombre de voix obligatoire") @Min(value = 1, message = "Au moins 1 voix") Integer nbVoix,
        @NotNull(message = "Opérateur obligatoire : MTN ou ORANGE") Operateur operateur,
        @NotBlank(message = "Numéro Mobile Money obligatoire") @Size(max = 20) String telephone,
        @Size(max = 200) String website) {
}
