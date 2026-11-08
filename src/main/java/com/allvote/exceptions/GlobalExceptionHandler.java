package com.allvote.exceptions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/** Transforme toutes les erreurs en JSON propre {status, message, champs}, sans fuite d'informations internes. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        return body(e.getStatus(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException e) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> champs.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Certains champs sont invalides.", champs);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            HttpMediaTypeNotSupportedException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Map<String, Object>> illisible(Exception e) {
        return body(HttpStatus.BAD_REQUEST, "Requête invalide.", null);
    }

    /** @PreAuthorize refusé : l'utilisateur est connecté mais n'a pas la permission. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> refuse(AccessDeniedException e) {
        return body(HttpStatus.FORBIDDEN, "Vous n'avez pas les droits pour cette action.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> tropGros(MaxUploadSizeExceededException e) {
        return body(HttpStatus.CONTENT_TOO_LARGE, "Fichier trop volumineux (5 Mo maximum).", null);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> multipart(MultipartException e) {
        return body(HttpStatus.BAD_REQUEST, "Envoi de fichier invalide.", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> introuvable(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "Ressource introuvable.", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> methode(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Méthode non autorisée.", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrite(DataIntegrityViolationException e) {
        log.warn("Violation d'intégrité : {}", e.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "Opération impossible : conflit avec des données existantes.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> autre(Exception e) {
        log.error("Erreur inattendue", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Map<String, String> champs) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("status", status.value());
        b.put("message", message);
        if (champs != null && !champs.isEmpty()) {
            b.put("champs", champs);
        }
        return ResponseEntity.status(status).body(b);
    }
}
