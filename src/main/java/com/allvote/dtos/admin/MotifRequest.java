package com.allvote.dtos.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MotifRequest(@NotBlank(message = "Motif obligatoire") @Size(max = 250) String motif) {
}
