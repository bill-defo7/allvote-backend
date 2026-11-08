package com.allvote.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

/** Compte de connexion : super-admin, ou membre d'un organisateur. */
@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUtilisateur;

    @Basic(optional = false)
    @Column(length = 80)
    private String nom;

    @Basic(optional = false)
    @Column(length = 80)
    private String prenom;

    @Basic(optional = false)
    @Column(unique = true, length = 120)
    private String email;

    @Basic
    @Column(length = 20)
    private String telephone;

    /** Hash BCrypt — jamais le mot de passe en clair. */
    @Basic(optional = false)
    @Column(length = 100)
    private String motDePasse;

    @Basic(optional = false)
    private boolean actif = true;

    @Basic(optional = false)
    private int tentativesEchouees;

    @Basic
    private Instant verrouilleJusqua;

    @Basic
    private Instant derniereConnexion;

    @Basic(optional = false)
    private Instant dateCreation = Instant.now();

    /** Organisation de rattachement (null pour le super-admin). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organisateur")
    private Organisateur organisateur;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "utilisateur_role",
            joinColumns = @JoinColumn(name = "id_utilisateur"),
            inverseJoinColumns = @JoinColumn(name = "id_role"))
    private List<Role> roles = new ArrayList<>();
}
