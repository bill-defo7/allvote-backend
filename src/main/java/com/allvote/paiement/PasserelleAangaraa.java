package com.allvote.paiement;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.allvote.config.AppProperties;
import com.allvote.entities.TransactionVote;
import com.allvote.utiles.Operateur;
import com.allvote.utiles.Telephones;

import lombok.extern.slf4j.Slf4j;

/**
 * Intégration réelle Aangaraa Pay — paiement direct (invite USSD, sans redirection).
 * POST /api/v1/no_redirect/payment puis POST /api/v1/aangaraa_check_status.
 * Sécurité : la clé app_key ne quitte jamais le serveur et n'est jamais écrite dans les logs.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payment.mode", havingValue = "aangaraa")
public class PasserelleAangaraa implements PasserellePaiement {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private final RestClient client;
    private final String appKey;
    private final String notifyUrl;
    private final String returnUrl;

    public PasserelleAangaraa(AppProperties props) {
        AppProperties.Aangaraa cfg = props.payment().aangaraa();
        if (cfg.appKey() == null || cfg.appKey().isBlank()) {
            throw new IllegalStateException("ALLVOTE_PAYMENT_MODE=aangaraa mais ALLVOTE_AANGARAA_APP_KEY est vide.");
        }
        this.appKey = cfg.appKey();
        this.notifyUrl = cfg.notifyUrl();
        this.returnUrl = cfg.returnUrl();

        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofSeconds(5));
        rf.setReadTimeout(Duration.ofSeconds(25));
        this.client = RestClient.builder().baseUrl(cfg.baseUrl()).requestFactory(rf).build();
        log.info("Paiement Aangaraa Pay ACTIVÉ (paiements réels). Webhook : {}", notifyUrl);
    }

    @Override
    public ResultatInitiation initier(TransactionVote tx) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone_number", tx.getTelephone());                 // 2376XXXXXXXX
        body.put("amount", String.valueOf(tx.getMontant()));
        body.put("description", "AllVote - " + tx.getNbVoix() + " voix");
        body.put("app_key", appKey);
        body.put("transaction_id", tx.getReference().toString());
        body.put("return_url", returnUrl);
        body.put("notify_url", notifyUrl);
        body.put("operator", tx.getOperateur() == Operateur.MTN ? "MTN_Cameroon" : "Orange_Cameroon");
        body.put("devise_id", "XAF");
        try {
            Map<String, Object> res = client.post().uri("/api/v1/no_redirect/payment")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(MAP);
            Map<String, Object> data = asMap(res == null ? null : res.get("data"));
            String payToken = str(data.get("payToken"));
            if (payToken == null) {
                payToken = str(data.get("pay_token"));
            }
            if (payToken == null) {
                log.warn("Aangaraa : réponse sans payToken pour {} (message={})",
                        tx.getReference(), res == null ? null : res.get("message"));
                return ResultatInitiation.echec("Le paiement n'a pas pu être initié.");
            }
            log.info("Paiement Aangaraa initié : ref={} tel={} montant={}",
                    tx.getReference(), Telephones.masquer(tx.getTelephone()), tx.getMontant());
            return new ResultatInitiation(true, payToken, str(data.get("transaction_id")), str(res.get("message")));
        } catch (RestClientResponseException e) {
            log.warn("Aangaraa a refusé l'initiation ({}) pour {} : {}", e.getStatusCode().value(),
                    tx.getReference(), tronquer(e.getResponseBodyAsString()));
            return ResultatInitiation.echec("Paiement refusé par le prestataire. Vérifiez le numéro et l'opérateur.");
        } catch (RestClientException e) {
            log.error("Aangaraa injoignable pour {} : {}", tx.getReference(), e.getMessage());
            throw e;
        }
    }

    @Override
    public ResultatVerification verifier(TransactionVote tx) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payToken", tx.getPayToken());
        body.put("app_key", appKey);
        try {
            Map<String, Object> res = client.post().uri("/api/v1/aangaraa_check_status")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(MAP);
            if (res == null) {
                return new ResultatVerification(EtatPaiement.INCONNU, null, "Réponse vide");
            }
            return new ResultatVerification(etat(str(res.get("status"))), montant(res.get("amount")), str(res.get("message")));
        } catch (RestClientResponseException e) {
            log.warn("check_status en erreur ({}) pour {} : {}", e.getStatusCode().value(),
                    tx.getReference(), tronquer(e.getResponseBodyAsString()));
            return new ResultatVerification(EtatPaiement.INCONNU, null, "Vérification impossible");
        }
    }

    // ------------------------------------------------------------------

    private static EtatPaiement etat(String s) {
        if (s == null) {
            return EtatPaiement.INCONNU;
        }
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "SUCCESSFUL", "SUCCESS", "SUCCEEDED" -> EtatPaiement.REUSSI;
            case "PENDING", "INITIATED", "PROCESSING" -> EtatPaiement.EN_COURS;
            case "FAILED", "FAILURE", "CANCELLED", "CANCELED", "REJECTED", "EXPIRED" -> EtatPaiement.ECHOUE;
            default -> EtatPaiement.INCONNU;
        };
    }

    /** Le montant peut arriver en nombre (100, 100.0) ou en texte ("100"). */
    private static Long montant(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.toString().trim()).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static String tronquer(String s) {
        if (s == null) {
            return "";
        }
        String propre = s.replaceAll("(?i)\"app_?key\"\\s*:\\s*\"[^\"]*\"", "\"app_key\":\"***\"");
        return propre.length() > 300 ? propre.substring(0, 300) + "…" : propre;
    }
}
