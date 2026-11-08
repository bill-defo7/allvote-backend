package com.allvote.dtos.publique;

import java.util.List;

public record CategorieDto(Long idCategorie, String nom, int ordre, List<CandidatDto> candidats) {
}
