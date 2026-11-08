package com.allvote.entities;

import java.time.Instant;

import com.allvote.utiles.TypeCandidat;

import jakarta.persistence.*;
import lombok.*;

/** Une personne, une équipe ou un projet pour qui on vote. */
@Entity
@Table(name = "candidats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_categorie", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candidat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCandidat;

    @Basic(optional = false)
    private int numero;

    @Basic(optional = false)
    @Column(length = 120)
    private String nom;

    /** Filière, équipe, club… selon le type. */
    @Basic
    @Column(length = 80)
    private String sousTitre;

    @Basic
    @Column(length = 280)
    private String description;

    @Basic
    private String photoUrl;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TypeCandidat typeCandidat = TypeCandidat.PERSONNE;

    /** Voix CONFIRMÉES par paiement uniquement (modifié seulement par requête atomique). */
    @Basic(optional = false)
    private long votes;

    @Basic(optional = false)
    private boolean actif = true;

    @Basic(optional = false)
    private Instant dateCreation = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categorie")
    private Categorie categorie;
}
