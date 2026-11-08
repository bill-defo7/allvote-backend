package com.allvote.services;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stockage des images (logos, affiches, photos de candidats).
 * Implémentation actuelle : disque local. Pour passer au cloud, ajouter une autre implémentation.
 */
public interface StockageService {

    /** Valide, nettoie et enregistre l'image. @return l'URL relative publique, ex. /uploads/xxxx.jpg */
    String stockerImage(MultipartFile fichier);
}
