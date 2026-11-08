package com.allvote.dtos.admin;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

/** Taux en % (ex. 15.5). null = revenir au taux par défaut de la plateforme. */
public record CommissionRequest(
        @DecimalMin(value = "0", message = "0 % minimum") @DecimalMax(value = "100", message = "100 % maximum")
        @Digits(integer = 3, fraction = 2, message = "2 décimales maximum") BigDecimal tauxCommission) {
}
