package com.allvote.entities;

import jakarta.persistence.*;
import lombok.*;

/** Réglage modifiable sans redéployer (ex. taux_commission_defaut = 20). */
@Entity
@Table(name = "parametres_plateforme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParametrePlateforme {

    public static final String TAUX_COMMISSION_DEFAUT = "taux_commission_defaut";

    /** Frais de service en % payés EN PLUS par le votant (couvrent les frais du prestataire de paiement). */
    public static final String FRAIS_VOTANT = "frais_votant_pourcentage";

    @Id
    @Column(length = 60)
    private String cle;

    @Basic(optional = false)
    private String valeur;

    @Basic
    private String description;
}
