package com.allvote.dtos.commun;

import java.util.List;

import org.springframework.data.domain.Page;

/** Une page de résultats (contenu + informations de pagination). */
public record PageDto<T>(List<T> contenu, int page, int taille, long total, int totalPages) {

    public static <T> PageDto<T> de(Page<?> p, List<T> contenu) {
        return new PageDto<>(contenu, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
