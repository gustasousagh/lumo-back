package com.movies.backend.user.entity;

/** Diferencia para que serve o token: confirmar email ou resetar senha. */
public enum TokenType {
    EMAIL_VERIFICATION,
    PASSWORD_RESET
}
