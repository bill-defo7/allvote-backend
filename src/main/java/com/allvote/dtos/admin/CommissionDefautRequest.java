package com.allvote.dtos.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CommissionDefautRequest(
        @NotNull(message = "Taux obligatoire")
        @DecimalMin(value = "0", message = "0 % minimum") @DecimalMax(value = "100", message = "100 % maximum")
        @Digits(integer = 3, fraction = 2, message = "2 décimales maximum") BigDecimal taux) {
}
