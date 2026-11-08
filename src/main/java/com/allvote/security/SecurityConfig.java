package com.allvote.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.allvote.config.AppProperties;
import com.allvote.repositories.UtilisateurRepository;
import com.allvote.utiles.Roles;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Sécurité de l'API AllVote :
 * - sans session (stateless) : jeton JWT signé HMAC-SHA256 dans l'en-tête Authorization ;
 * - tout est INTERDIT par défaut, seules les routes listées sont ouvertes ;
 * - /api/orga/**  : membres d'une organisation ; /api/admin/** : super-admin uniquement ;
 * - les permissions fines (FINANCE_VOIR…) se contrôlent avec @PreAuthorize sur les contrôleurs.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter,
            UtilisateurRepository utilisateurRepository) throws Exception {
        AuthenticationEntryPoint nonConnecte = (req, res, ex) ->
                ReponseJson.erreur(res, 401, "Connexion requise ou session expirée.");
        AccessDeniedHandler interdit = (req, res, ex) ->
                ReponseJson.erreur(res, 403, "Vous n'avez pas les droits pour cette action.");

        http
            // CSRF inutile : pas de cookie de session, le jeton est envoyé dans l'en-tête Authorization
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                // --- public (visiteurs, votants) ---
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/votes").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/public/organisateurs/inscription").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/aangaraa/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**", "/share/**").permitAll()
                // --- connecté ---
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/orga/**").hasAnyRole(Roles.ORGA_ADMIN, Roles.ORGA_MEMBRE)
                .requestMatchers("/api/admin/**").hasRole(Roles.SUPER_ADMIN)
                // --- tout le reste est refusé ---
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o
                .jwt(j -> j.jwtAuthenticationConverter(jwtConverter))
                .authenticationEntryPoint(nonConnecte)
                .accessDeniedHandler(interdit))
            .exceptionHandling(e -> e.authenticationEntryPoint(nonConnecte).accessDeniedHandler(interdit))
            .addFilterAfter(new CompteActifFilter(utilisateurRepository), BearerTokenAuthenticationFilter.class)
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; img-src 'self'; frame-ancestors 'none'"))
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origines = props.cors().allowedOrigins().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        cfg.setAllowedOrigins(origines);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // ------------------------- JWT -------------------------

    @Bean
    SecretKey jwtSecretKey(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "ALLVOTE_JWT_SECRET manquant ou trop court (32 caractères minimum). Générez-le avec : openssl rand -base64 48");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    /** Claim "roles" -> ROLE_SUPER_ADMIN… ; claim "permissions" -> FINANCE_VOIR… (sans préfixe). */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> autorites = new ArrayList<>();
            List<String> roles = jwt.getClaimAsStringList(JwtService.CLAIM_ROLES);
            if (roles != null) {
                roles.forEach(r -> autorites.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }
            List<String> perms = jwt.getClaimAsStringList(JwtService.CLAIM_PERMISSIONS);
            if (perms != null) {
                perms.forEach(p -> autorites.add(new SimpleGrantedAuthority(p)));
            }
            return autorites;
        });
        return conv;
    }
}
