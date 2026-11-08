package com.allvote.dtos.publique;

import java.util.List;

public record OrganisateurPublicDto(String nom, String slug, String description, String logoUrl, String ville,
        List<EvenementResumeDto> evenements) {
}
