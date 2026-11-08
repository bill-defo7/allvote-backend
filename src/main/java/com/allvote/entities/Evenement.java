package com.allvote.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.allvote.utiles.StatutEvenement;

import jakarta.persistence.*;
import lombok.*;

/** Un événement (soirée, tournoi, salon…) qui regroupe un ou plusieurs concours. */
@Entity
@Table(name = "evenements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEvenement;

    @Basic(optional = false)
    @Column(length = 150)
    private String titre;

    /** Identifiant d'URL : allvote.cm/e/{slug} */
    @Basic(optional = false)
    @Column(unique = true, length = 90)
    private String slug;

    @Basic
    @Column(length = 2000)
    private String description;

    @Basic
    @Column(length = 150)
    private String lieu;

    @Basic
    private Instant dateDebut;

    @Basic
    private Instant dateFin;

    @Basic
    private String afficheUrl;

    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutEvenement statut = StatutEvenement.BROUILLON;

    @Basic(optional = false)
    private Instant dateCreation = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organisateur")
    private Organisateur organisateur;

    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateOuverture ASC")
    private List<Concours> concours = new ArrayList<>();
}
