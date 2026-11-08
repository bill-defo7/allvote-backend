package com.allvote.services;

import java.math.BigDecimal;
import java.util.List;

import com.allvote.dtos.admin.ParametreDto;

public interface ParametreService {

    /** Taux de commission par défaut de la plateforme, en % (ex. 20.00). */
    BigDecimal tauxCommissionDefaut();

    /** Frais de service en % payés en plus par le votant (ex. 5.00). */
    BigDecimal fraisVotantPourcentage();

    List<ParametreDto> lister();

    /** Les nouvelles transactions utiliseront ce taux ; l'historique ne change pas (taux figé par transaction). */
    ParametreDto modifierTauxDefaut(BigDecimal taux);

    /** Idem pour les frais de service votant : seuls les nouveaux votes sont concernés. */
    ParametreDto modifierFraisVotant(BigDecimal taux);
}
