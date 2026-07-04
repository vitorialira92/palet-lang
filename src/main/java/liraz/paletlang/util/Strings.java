package liraz.paletlang.util;

import java.math.BigDecimal;

/**
 * Conjunto de funções auxiliares para manipulação de texto utilizadas em todo
 * o compilador: convertem tokens da gramática em seus valores reais (como a
 * remoção de caracteres de escape em strings entre aspas) e escapam valores
 * de forma segura para os diferentes destinos de saída (texto HTML, literais
 * de string em JavaScript e valores CSS).
 */
public final class Strings {

    private Strings() {}

    /**
     * Converte um token STRING (incluindo as aspas duplas que o delimitam e
     * quaisquer sequências de escape) para o texto literal que ele representa.
     * O processamento segue exatamente as sequências de escape permitidas pelo
     * fragmento {@code ESC} da gramática.
     */
    public static String unquote(String tokenText) {
        if (tokenText == null || tokenText.length() < 2)
            return "";

        String inner = tokenText.substring(1, tokenText.length() - 1);
        StringBuilder sb = new StringBuilder(inner.length());

        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);

            if (c != '\\' || i == inner.length() - 1) {
                sb.append(c);
                continue;
            }

            char n = inner.charAt(++i);
            switch (n) {
                case '"'  -> sb.append('"');
                case '\\' -> sb.append('\\');
                case '/'  -> sb.append('/');
                case 'b'  -> sb.append('\b');
                case 'f'  -> sb.append('\f');
                case 'n'  -> sb.append('\n');
                case 'r'  -> sb.append('\r');
                case 't'  -> sb.append('\t');
                case 'u'  -> {
                    if ((i + 4) < (inner.length() + 1) && (i + 5) <= inner.length()) {
                        String hex = inner.substring(i + 1, i + 5);
                        try {
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            sb.append(n);
                        }
                    } else {
                        sb.append(n);
                    }
                }
                default -> sb.append(n);
            }
        }
        return sb.toString();
    }

    /** Escapa um texto para que possa ser inserido com segurança no conteúdo ou nos atributos de elementos HTML. */
    public static String html(String s) {
        if (s == null)
            return "";

        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&'  -> sb.append("&amp;");
                case '<'  -> sb.append("&lt;");
                case '>'  -> sb.append("&gt;");
                case '"'  -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Escapa um texto para que possa ser utilizado com segurança em um literal de string JavaScript delimitado por aspas simples. */
    public static String js(String s) {
        if (s == null)
            return "";

        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Gera um valor de {@code font-family} compatível com CSS: coloca o nome da
     * fonte entre aspas quando ele contém espaços ou caracteres de pontuação;
     * caso contrário, mantém palavras-chave genéricas sem aspas.
     */
    public static String cssFontFamily(String family) {
        if (family == null || family.isBlank())
            return "sans-serif";

        boolean needsQuotes = !family.matches("[A-Za-z_][A-Za-z0-9_-]*");

        return needsQuotes ? "\"" + family.replace("\"", "\\\"") + "\"" : family;
    }

    /** Converte um identificador em um slug em letras minúsculas e separado por hífens, adequado para IDs em CSS e HTML. */
    public static String slug(String s) {
        if (s == null)
            return "";

        String slug = s.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        return slug.isEmpty() ? "x" : slug;
    }

    /** Formata um valor {@code double}, removendo o sufixo {@code .0} quando desnecessário (ex.: 8.0 → "8", 1.5 → "1.5"). */
    public static String num(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return Long.toString((long) v);
        }

        String s = String.valueOf(v);
        BigDecimal bd = new BigDecimal(s).stripTrailingZeros();
        return bd.toPlainString();
    }

    /** Converte a primeira letra para maiúscula (utilizado em títulos de seções e rótulos). */    public static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}