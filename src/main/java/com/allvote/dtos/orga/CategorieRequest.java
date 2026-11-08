package com.allvote.dtos.orga;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** ordre facultatif : à la fin si absent. */
public record CategorieRequest(
        @NotBlank(message = "Nom obligatoire") @Size(max = 80) String nom,
        @Min(1) Integer ordre) {
}
