package com.allvote.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.allvote.config.AppProperties;
import com.allvote.entities.Permission;
import com.allvote.entities.Role;
import com.allvote.entities.Utilisateur;

import lombok.RequiredArgsConstructor;

/**
 * Émission des jetons d'accès.
 * Contenu du jeton : sub = idUtilisateur, roles, permissions, org = idOrganisateur (absent pour le super-admin).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_ORG = "org";

    private final JwtEncoder encoder;
    private final AppProperties props;

    public record JetonAcces(String token, Instant expiresAt) {
    }

    public JetonAcces generer(Utilisateur u) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().expirationMinutes(), ChronoUnit.MINUTES);

        List<String> roles = u.getRoles().stream().map(Role::getNomRole).sorted().toList();
        TreeSet<String> permissions = new TreeSet<>();
        for (Role r : u.getRoles()) {
            r.getPermissions().stream().map(Permission::getCode).forEach(permissions::add);
        }

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("allvote")
                .subject(String.valueOf(u.getIdUtilisateur()))
                .issuedAt(now)
                .expiresAt(exp)
                .claim("email", u.getEmail())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMISSIONS, List.copyOf(permissions));
        if (u.getOrganisateur() != null) {
            claims.claim(CLAIM_ORG, u.getOrganisateur().getIdOrganisateur());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new JetonAcces(token, exp);
    }
}
