package com.allvote.dtos.orga;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Liste des ids dans le nouvel ordre d'affichage. */
public record OrdreRequest(@NotEmpty @Size(max = 200) List<Long> ids) {
}
