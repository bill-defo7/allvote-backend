package com.allvote.services.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.allvote.config.AppProperties;
import com.allvote.exceptions.ApiException;
import com.allvote.services.StockageService;

import lombok.extern.slf4j.Slf4j;

/**
 * Stockage sur disque, avec durcissement :
 * - seuls JPG/PNG sont acceptés, et le contenu est réellement décodé (pas juste l'extension) ;
 * - dimensions contrôlées AVANT décodage (anti « bombe d'image ») ;
 * - l'image est redimensionnée puis RÉ-ENCODÉE en JPEG : métadonnées (GPS…) et contenu caché supprimés ;
 * - nom de fichier aléatoire (UUID) : aucun nom fourni par l'utilisateur n'est utilisé.
 */
@Slf4j
@Service
public class StockageLocalServiceImpl implements StockageService {

    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png");
    private static final int TAILLE_MAX = 1200;           // px (plus grand côté)
    private static final long PIXELS_MAX = 40_000_000L;   // 40 Mpx

    private final Path racine;

    public StockageLocalServiceImpl(AppProperties props) throws IOException {
        this.racine = Path.of(props.storage().dir()).toAbsolutePath().normalize();
        Files.createDirectories(racine);
        log.info("Stockage des images : {}", racine);
    }

    public Path racine() {
        return racine;
    }

    @Override
    public String stockerImage(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw ApiException.badRequest("Aucun fichier reçu.");
        }
        if (fichier.getContentType() == null || !TYPES.contains(fichier.getContentType())) {
            throw ApiException.badRequest("Formats acceptés : JPG ou PNG.");
        }

        BufferedImage source = lire(fichier);
        int w = source.getWidth();
        int h = source.getHeight();
        double ratio = Math.min(1.0, TAILLE_MAX / (double) Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * ratio));
        int nh = Math.max(1, (int) Math.round(h * ratio));

        BufferedImage sortie = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sortie.createGraphics();
        try {
            g.setColor(Color.WHITE);                // fond blanc pour les PNG transparents
            g.fillRect(0, 0, nw, nh);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }

        String nom = UUID.randomUUID() + ".jpg";
        Path cible = racine.resolve(nom).normalize();
        if (!cible.startsWith(racine)) {
            throw ApiException.badRequest("Chemin invalide.");
        }
        ecrireJpeg(sortie, cible);
        return "/uploads/" + nom;
    }

    private BufferedImage lire(MultipartFile fichier) {
        try (InputStream in = fichier.getInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            if (iis == null) {
                throw ApiException.badRequest("Image illisible.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw ApiException.badRequest("Le fichier n'est pas une image valide.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                String format = reader.getFormatName().toLowerCase();
                if (!format.equals("jpeg") && !format.equals("png")) {
                    throw ApiException.badRequest("Formats acceptés : JPG ou PNG.");
                }
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > PIXELS_MAX) {
                    throw ApiException.badRequest("Image trop grande (dimensions).");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw ApiException.badRequest("Image illisible ou corrompue.");
        }
    }

    private void ecrireJpeg(BufferedImage img, Path cible) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("Aucun encodeur JPEG disponible");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.85f);
        try (OutputStream os = Files.newOutputStream(cible);
             ImageOutputStream ios = ImageIO.createImageOutputStream(os)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } catch (IOException e) {
            throw new IllegalStateException("Écriture de l'image impossible", e);
        } finally {
            writer.dispose();
        }
    }
}
