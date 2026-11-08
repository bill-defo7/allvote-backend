package com.allvote.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.allvote.config.AppProperties;
import com.allvote.dtos.publique.CandidatPageDto;
import com.allvote.dtos.publique.EvenementDetailDto;
import com.allvote.services.PubliqueService;

import lombok.RequiredArgsConstructor;

/**
 * Liens de partage (WhatsApp, Facebook, X) : petite page HTML avec les balises Open Graph
 * (titre + image) que lisent les réseaux sociaux, puis redirection de l'humain vers le front.
 *  - /share/concours/{id}/candidat/{idCandidat} -> page du candidat, fenêtre de vote ouverte (?vote=1)
 *  - /share/evenements/{slug}                   -> page de l'événement
 * Tout ce qui est affiché est échappé (anti-injection HTML).
 */
@RestController
@RequiredArgsConstructor
public class PartageController {

    private final PubliqueService publiqueService;
    private final AppProperties props;

    @GetMapping(value = "/share/concours/{idConcours}/candidat/{idCandidat}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> candidat(@PathVariable Long idConcours, @PathVariable Long idCandidat) {
        CandidatPageDto p = publiqueService.candidat(idConcours, idCandidat);
        String cible = props.frontBase() + "/concours/" + idConcours + "/candidat/" + idCandidat + "?vote=1";
        String titre = "Vote pour " + p.candidat().nom() + " — " + p.categorieNom();
        String desc = p.candidat().description() != null ? p.candidat().description()
                : "Soutiens " + p.candidat().nom() + " au concours « " + p.concours().titre() + " » sur AllVote !";
        return page(titre, desc, image(p.candidat().photoUrl()), cible);
    }

    @GetMapping(value = "/share/evenements/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> evenement(@PathVariable String slug) {
        EvenementDetailDto e = publiqueService.evenement(slug);
        String cible = props.frontBase() + "/e/" + e.slug();
        String desc = e.description() != null ? e.description()
                : "Votez pour vos candidats préférés — " + e.organisateur().nom();
        return page(e.titre() + " — AllVote", desc, image(e.afficheUrl()), cible);
    }

    private String image(String url) {
        return url != null ? props.publicBase() + url : props.frontBase() + "/og-image.jpg";
    }

    private ResponseEntity<String> page(String titre, String desc, String image, String cible) {
        if (desc.length() > 200) {
            desc = desc.substring(0, 197) + "…";
        }
        String html = """
                <!doctype html>
                <html lang="fr"><head><meta charset="utf-8">
                <title>%1$s</title>
                <meta name="description" content="%2$s">
                <meta property="og:type" content="website">
                <meta property="og:site_name" content="AllVote">
                <meta property="og:title" content="%1$s">
                <meta property="og:description" content="%2$s">
                <meta property="og:image" content="%3$s">
                <meta property="og:url" content="%4$s">
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:title" content="%1$s">
                <meta name="twitter:description" content="%2$s">
                <meta name="twitter:image" content="%3$s">
                <meta http-equiv="refresh" content="0;url=%4$s">
                </head><body>
                <p>Redirection vers <a href="%4$s">la page de vote</a>…</p>
                </body></html>
                """.formatted(esc(titre), esc(desc), esc(image), esc(cible));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private static String esc(String s) {
        return HtmlUtils.htmlEscape(s, "UTF-8");
    }
}
