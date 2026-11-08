package com.allvote.utiles;

/** Expressions réutilisées dans les annotations @Pattern des DTOs. */
public final class Validations {

    private Validations() {
    }

    /** Seules les images téléversées sur AllVote sont acceptées (pas de lien externe). */
    public static final String URL_IMAGE = "^/uploads/[a-f0-9-]{36}\\.jpg$";

    public static final String MOT_DE_PASSE = "^(?=.*[A-Za-z])(?=.*\\d).{10,128}$";

    public static final String MESSAGE_MOT_DE_PASSE = "10 caractères minimum, avec au moins une lettre et un chiffre";
}
