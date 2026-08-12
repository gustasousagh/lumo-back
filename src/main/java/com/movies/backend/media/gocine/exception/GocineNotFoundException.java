package com.movies.backend.media.gocine.exception;

/** O GoCine respondeu 404 — o título não existe naquele endpoint. */
public class GocineNotFoundException extends RuntimeException {

    public GocineNotFoundException(String message) {
        super(message);
    }
}
