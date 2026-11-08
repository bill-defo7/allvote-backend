package com.allvote.utiles;

import com.allvote.exceptions.ApiException;

/** Numéros camerounais : accepte "6XX XX XX XX", "+237 6…", "237 6…", "00237 6…" et renvoie "6XXXXXXXX". */
public final class Telephones {

    private Telephones() {
    }

    public static String normaliser(String brut) {
        if (brut == null || brut.isBlank()) {
            return null;
        }
        String d = brut.replaceAll("[\\s.\\-()]", "");
        if (d.startsWith("+")) {
            d = d.substring(1);
        }
        if (d.startsWith("00237")) {
            d = d.substring(5);
        } else if (d.startsWith("237") && d.length() == 12) {
            d = d.substring(3);
        }
        if (!d.matches("6\\d{8}")) {
            throw ApiException.badRequest("Numéro de téléphone invalide (format attendu : 6XXXXXXXX).");
        }
        return d;
    }

    /** Format attendu par Aangaraa : "2376XXXXXXXX". */
    public static String formatInternational(String brut) {
        String d = normaliser(brut);
        return d == null ? null : "237" + d;
    }

    /** Pour les logs et l'affichage : 2376XXXXXX42 -> 2376••••••42. */
    public static String masquer(String tel) {
        if (tel == null || tel.length() < 6) {
            return "•••";
        }
        return tel.substring(0, 4) + "•".repeat(tel.length() - 6) + tel.substring(tel.length() - 2);
    }
}
