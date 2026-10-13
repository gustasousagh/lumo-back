package com.movies.backend.media.gocine.exception;

/**
 * Não deu para ter sessão no GoCine (login recusado, bloqueado por excesso de
 * tentativas, ou a página insiste em pedir login). Não é culpa do título pedido
 * — quem chama deve esperar e tentar mais tarde, não desistir dele.
 */
public class GocineAuthException extends IllegalStateException {
    public GocineAuthException(String message) {
        super(message);
    }
}
