package com.movies.backend.media.gocine.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê o "flight" RSC que o Next embute na página.
 *
 * O site do GoCine é um Next.js (App Router). Os dados do catálogo não vêm numa
 * API JSON limpa: vêm renderizados pelo servidor e despejados no HTML em vários
 * `self.__next_f.push([1, "<fragmento>"])`, onde cada fragmento é uma string
 * JSON-escapada. Juntando e desescapando os fragmentos reconstrói-se o fluxo,
 * de onde dá para recortar os objetos/arrays que interessam (home, detalhe,
 * fontes de vídeo).
 *
 * É mais frágil que uma API — se eles redesenharem a página, muda aqui. Mas é o
 * que o novo backend expõe, e o recorte por chave/âncora aguenta mudanças
 * pequenas de estrutura.
 */
final class Rsc {

    private Rsc() {}

    /**
     * Quantificadores possessivos (++ / *+) de propósito: com `(?:a|b)*` comum o
     * motor de regex do Java empilha uma chamada por caractere e estoura a pilha
     * (StackOverflowError) nos fragmentos grandes — era o que derrubava a home.
     */
    private static final Pattern PUSH =
            Pattern.compile("__next_f\\.push\\(\\[1,\\s*\"((?:[^\"\\\\]++|\\\\.)*+)\"\\]\\)");

    /** Reconstrói o fluxo RSC desescapando todos os fragmentos. */
    static String flight(ObjectMapper om, String html) {
        if (html == null) return "";
        StringBuilder sb = new StringBuilder(html.length());
        Matcher m = PUSH.matcher(html);
        while (m.find()) {
            try {
                sb.append(om.readValue("\"" + m.group(1) + "\"", String.class));
            } catch (Exception ignore) {
                // fragmento malformado: ignora e segue, não vale derrubar tudo
            }
        }
        return sb.toString();
    }

    /** Array em `"key":[ ... ]`, como JsonNode, ou null se não achar. */
    static JsonNode array(ObjectMapper om, String flight, String key) {
        int i = flight.indexOf("\"" + key + "\":");
        if (i < 0) return null;
        int j = i + key.length() + 3;
        while (j < flight.length() && flight.charAt(j) != '[') j++;
        return parse(om, balanced(flight, j, '[', ']'));
    }

    /**
     * O array `"key":[ ... ]` mais próximo ANTES da âncora. Serve para achar a
     * lista certa quando a mesma chave aparece várias vezes na página (o
     * carrossel de destaques da home e as linhas usam ambos `"items"`).
     */
    static JsonNode arrayBefore(ObjectMapper om, String flight, String anchor, String key) {
        int a = flight.indexOf(anchor);
        if (a < 0) return null;
        int i = flight.lastIndexOf("\"" + key + "\":[", a);
        if (i < 0) return null;
        return parse(om, balanced(flight, i + key.length() + 3, '[', ']'));
    }

    /** Primeira ocorrência do padrão (grupo 1), ou null. */
    static String firstMatch(String flight, Pattern p) {
        Matcher m = p.matcher(flight);
        return m.find() ? m.group(1) : null;
    }

    /** Valor string de `"key":"..."` (primeira ocorrência), ou null. */
    static String firstString(String flight, String key) {
        String needle = "\"" + key + "\":\"";
        int i = flight.indexOf(needle);
        if (i < 0) return null;
        int start = i + needle.length();
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int j = start; j < flight.length(); j++) {
            char c = flight.charAt(j);
            if (esc) {
                sb.append(c);
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * O objeto `{...}` que CONTÉM a âncora (primeira ocorrência). Útil quando o
     * dado é passado como prop de um componente e não tem uma chave de topo.
     */
    static JsonNode objectContaining(ObjectMapper om, String flight, String anchor) {
        int a = flight.indexOf(anchor);
        if (a < 0) return null;
        int depth = 0, k = a;
        for (; k >= 0; k--) {
            char c = flight.charAt(k);
            if (c == '}') depth++;
            else if (c == '{') {
                if (depth == 0) break;
                depth--;
            }
        }
        if (k < 0) return null;
        return parse(om, balanced(flight, k, '{', '}'));
    }

    /** Recorte balanceado de open..close a partir de `start`, respeitando strings. */
    private static String balanced(String s, int start, char open, char close) {
        if (start < 0 || start >= s.length() || s.charAt(start) != open) return null;
        int depth = 0;
        boolean instr = false, esc = false;
        for (int j = start; j < s.length(); j++) {
            char c = s.charAt(j);
            if (esc) {
                esc = false;
            } else if (c == '\\') {
                esc = true;
            } else if (c == '"') {
                instr = !instr;
            } else if (!instr) {
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return s.substring(start, j + 1);
                }
            }
        }
        return null;
    }

    private static JsonNode parse(ObjectMapper om, String json) {
        if (json == null) return null;
        try {
            return om.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
