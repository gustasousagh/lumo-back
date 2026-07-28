package com.movies.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Mapeia o bloco "app:" do application.yml para um objeto Java tipado.
 * Assim os Services leem as configs sem espalhar @Value por todo lado.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl = "http://localhost:3000";
    private String mailFrom = "Movies <nao-responda@movies.local>";
    private Jwt jwt = new Jwt();
    private Tokens tokens = new Tokens();

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Tokens getTokens() {
        return tokens;
    }

    public void setTokens(Tokens tokens) {
        this.tokens = tokens;
    }

    public static class Jwt {
        private String secret;
        private long expirationMs = 604800000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Tokens {
        private long verificationExpirationMinutes = 1440;
        private long resetExpirationMinutes = 60;

        public long getVerificationExpirationMinutes() {
            return verificationExpirationMinutes;
        }

        public void setVerificationExpirationMinutes(long verificationExpirationMinutes) {
            this.verificationExpirationMinutes = verificationExpirationMinutes;
        }

        public long getResetExpirationMinutes() {
            return resetExpirationMinutes;
        }

        public void setResetExpirationMinutes(long resetExpirationMinutes) {
            this.resetExpirationMinutes = resetExpirationMinutes;
        }
    }
}
