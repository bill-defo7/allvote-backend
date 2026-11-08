package com.allvote.dtos.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** referencePaiement = référence de la transaction Mobile Money envoyée à l'organisateur. */
public record PaiementReversementRequest(
        @NotBlank(message = "Référence du paiement obligatoire") @Size(max = 120) String referencePaiement,
        @Size(max = 250) String note) {
}
