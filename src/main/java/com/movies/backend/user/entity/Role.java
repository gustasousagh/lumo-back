package com.movies.backend.user.entity;

/**
 * Papéis do sistema. Guardados na coluna {@code users.role} como texto minúsculo
 * ("user" / "admin") — foi assim que o campo nasceu, e mudar o formato quebraria
 * as linhas que já existem no banco. Por isso a conversão passa sempre por aqui:
 * o banco fala minúsculo, o Spring Security fala {@code ROLE_MAIÚSCULO}.
 */
public enum Role {
    USER,
    ADMIN;

    /** Valor gravado na coluna. */
    public String dbValue() {
        return name().toLowerCase();
    }

    /** Authority no formato que o Spring Security espera em hasRole(...). */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Converte o texto do banco; qualquer coisa desconhecida vira USER. */
    public static Role from(String raw) {
        if (raw == null) {
            return USER;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }
}
