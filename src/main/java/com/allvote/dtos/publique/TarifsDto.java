package com.allvote.dtos.publique;

import java.math.BigDecimal;

/** Frais de service en % ajoutés au prix des voix (arrondi au franc supérieur). Le serveur recalcule toujours. */
public record TarifsDto(BigDecimal fraisVotantPourcentage) {
}
