package com.movies.backend.media.gocine.exception;

/**
 * Não deu para ter sessão no GoCine (login recusado, bloqueado por excesso de
 * tentativas, ou a página insiste em pedir login).
 */
public class GocineAuthException extends GocineUnavailableException {
    public GocineAuthException(String message) {
        super(message);
    }
}
