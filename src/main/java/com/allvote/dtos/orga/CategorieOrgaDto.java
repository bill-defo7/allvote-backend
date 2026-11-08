package com.allvote.dtos.orga;

import java.util.List;

public record CategorieOrgaDto(Long idCategorie, String nom, int ordre, List<CandidatOrgaDto> candidats) {
}
