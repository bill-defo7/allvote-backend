package com.allvote.entities;

import jakarta.persistence.*;
import lombok.*;

/** Une action autorisée (ex. EVENEMENT_GERER). Attribuée aux rôles. */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPermission;

    @Basic(optional = false)
    @Column(unique = true, length = 60)
    private String code;

    @Basic
    private String description;
}
