package com.allvote.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.allvote.utiles.Operateur;
import com.allvote.utiles.StatutOrganisateur;

import jakarta.persistence.*;
import lombok.*;

/** Une organisation qui crée des événements : département, club, association… */
@Entity
@Table(name = "organisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idOrganisateur;

    @Basic(optional = false)
    @Column(length = 120)
    private String nom;

    /** Identifiant d'URL : allvote.cm/o/{slug} */
    @Basic(optional = false)
    @Column(unique = true, length = 90)
    private String slug;

    @Basic
    @Column(length = 2000)
    private String description;

    @Basic
    private String logoUrl;

    @Basic(optional = false)
    @Column(length = 120)
    private String email;

    @Basic
    @Column(length = 20)
    private String telephone;

    @Basic
    @Column(length = 80)
    private String ville;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutOrganisateur statut = StatutOrganisateur.EN_ATTENTE;

    /** Taux spécifique en % (ex. 10.00). null = taux par défaut de la plateforme. */
    @Basic
    @Column(precision = 5, scale = 2)
    private BigDecimal tauxCommission;

    /** Numéro Mobile Money sur lequel AllVote reverse l'argent. */
    @Basic
    @Column(length = 20)
    private String numeroReversement;

    @Basic
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Operateur operateurReversement;

    @Basic
    private String motifRefus;

    @Basic(optional = false)
    private Instant dateCreation = Instant.now();

    @Basic
    private Instant dateValidation;

    @OneToMany(mappedBy = "organisateur")
    private List<Utilisateur> utilisateurs = new ArrayList<>();

    @OneToMany(mappedBy = "organisateur")
    private List<Evenement> evenements = new ArrayList<>();

    @OneToMany(mappedBy = "organisateur")
    private List<Reversement> reversements = new ArrayList<>();
}
