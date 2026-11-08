package com.allvote.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.allvote.services.VoteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Notification Aangaraa (notify_url).
 * Le webhook n'étant pas signé, son contenu (status, amount…) est IGNORÉ : on n'en retient que
 * notre transaction_id, puis on interroge nous-mêmes Aangaraa (check_status avec notre app_key).
 * Un faux webhook ne peut donc jamais créditer de voix.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final VoteService voteService;

    @PostMapping("/api/payments/aangaraa/webhook")
    public ResponseEntity<Map<String, Object>> notification(@RequestBody(required = false) Map<String, Object> payload) {
        Object tid = payload == null ? null : payload.get("transaction_id");
        UUID reference;
        try {
            reference = UUID.fromString(String.valueOf(tid));
        } catch (IllegalArgumentException e) {
            log.warn("Webhook ignoré : transaction_id absent ou invalide");
            return ResponseEntity.ok(Map.of("received", true));
        }
        log.info("Webhook Aangaraa reçu pour {} -> re-vérification serveur", reference);
        try {
            voteService.verifier(reference);
        } catch (RuntimeException e) {
            log.error("Erreur lors du traitement du webhook {} : {}", reference, e.getMessage());
        }
        return ResponseEntity.ok(Map.of("received", true));
    }
}
