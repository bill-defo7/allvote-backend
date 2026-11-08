package com.allvote.utiles;

import java.text.Normalizer;
import java.util.Locale;

/** Transforme un titre en identifiant d'URL : "Soirée de Parrainage GI 2026" → "soiree-de-parrainage-gi-2026". */
public final class Slugs {
    private Slugs() {}

    public static String slugifier(String texte) {
        if (texte == null) {
            return "";
        }
        String s = Normalizer.normalize(texte, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")              // retire les accents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")          // tout le reste devient "-"
                .replaceAll("(^-+|-+$)", "");
        return s.length() > 80 ? s.substring(0, 80).replaceAll("-+$", "") : s;
    }
}
