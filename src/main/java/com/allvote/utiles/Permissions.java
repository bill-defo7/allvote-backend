package com.allvote.utiles;

/** Codes des permissions (lignes de la table "permissions"). */
public final class Permissions {
    private Permissions() {}

    // --- organisateur ---
    public static final String EVENEMENT_GERER = "EVENEMENT_GERER";
    public static final String EVENEMENT_PUBLIER = "EVENEMENT_PUBLIER";
    public static final String CANDIDAT_GERER = "CANDIDAT_GERER";
    public static final String TRANSACTION_VOIR = "TRANSACTION_VOIR";
    public static final String FINANCE_VOIR = "FINANCE_VOIR";
    public static final String MEMBRE_GERER = "MEMBRE_GERER";
    public static final String PROFIL_GERER = "PROFIL_GERER";
    // --- plateforme (super-admin) ---
    public static final String ORGANISATEUR_VALIDER = "ORGANISATEUR_VALIDER";
    public static final String REVERSEMENT_GERER = "REVERSEMENT_GERER";
    public static final String UTILISATEUR_GERER = "UTILISATEUR_GERER";
    public static final String PLATEFORME_PARAMETRER = "PLATEFORME_PARAMETRER";
}
