package com.movies.backend.media.gocine.exception;

/**
 * O GoCine não está atendendo agora (sem sessão, ou limitou as requisições com
 * 429). Não é culpa do título pedido — quem chama deve esperar e tentar mais
 * tarde, não desistir dele.
 */
public class GocineUnavailableException extends IllegalStateException {
    public GocineUnavailableException(String message) {
        super(message);
    }
}
