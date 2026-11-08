package com.allvote.dtos.orga;

import jakarta.validation.constraints.NotNull;

public record ActifRequest(@NotNull(message = "Champ actif obligatoire (true ou false)") Boolean actif) {
}
