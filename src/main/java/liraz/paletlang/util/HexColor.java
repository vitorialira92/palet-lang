package liraz.paletlang.util;

/**
 * Representa uma cor RGB já processada, juntamente com os cálculos de cor
 * necessários para o compilador: luminância relativa segundo as diretrizes
 * WCAG e a razão de contraste (utilizadas nas verificações de acessibilidade
 * e na geração do guia de estilos), além de alguns métodos auxiliares para
 * criar pré-visualizações legíveis.
 */
public final class HexColor {

    public final int r, g, b;

    private HexColor(int r, int g, int b) {
        this.r = r; this.g = g; this.b = b;
    }

    /**
     * Converte uma string hexadecimal no formato {@code #RGB} ou {@code #RRGGBB}
     * (sem distinção entre letras maiúsculas e minúsculas).
     * A gramática já garante o formato correto de um token HEX_COLOR, portanto
     * esta operação nunca deve falhar para tokens que chegaram à etapa de análise
     * semântica; a verificação existente é apenas uma medida defensiva.
     */
    public static HexColor parse(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (h.length() == 3) {
            int r = Integer.parseInt("" + h.charAt(0) + h.charAt(0), 16);
            int g = Integer.parseInt("" + h.charAt(1) + h.charAt(1), 16);
            int b = Integer.parseInt("" + h.charAt(2) + h.charAt(2), 16);
            return new HexColor(r, g, b);
        }
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new HexColor(r, g, b);
    }

    public String toHex() {
        return String.format("#%02x%02x%02x", r, g, b);
    }

    /** Luminância relativa segundo a WCAG 2.1, no intervalo [0, 1]. */
    public double luminance() {
        double rl = channel(r), gl = channel(g), bl = channel(b);
        return 0.2126 * rl + 0.7152 * gl + 0.0722 * bl;
    }

    private static double channel(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** Razão de contraste entre duas cores segundo a WCAG, variando de 1:1 a 21:1. */
    public static double contrast(HexColor a, HexColor b) {
        double la = a.luminance(), lb = b.luminance();
        double lighter = Math.max(la, lb), darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Escolhe entre preto e branco, utilizando a cor que oferece o maior
     * contraste em relação a esta cor.
     * Utilizado pelo guia de estilos para manter as legendas em hexadecimal
     * legíveis sobre qualquer amostra de cor.
     */
    public String readableInk() {
        HexColor black = new HexColor(0, 0, 0);
        HexColor white = new HexColor(255, 255, 255);
        return contrast(this, black) >= contrast(this, white) ? "#000000" : "#ffffff";
    }

    /**
     * Interpola esta cor em direção a outra utilizando o fator {@code t},
     * no intervalo [0, 1], de forma linear no espaço de cores sRGB.
     */
    public HexColor mix(HexColor other, double t) {
        int nr = (int) Math.round(r + (other.r - r) * t);
        int ng = (int) Math.round(g + (other.g - g) * t);
        int nb = (int) Math.round(b + (other.b - b) * t);
        return new HexColor(clamp(nr), clamp(ng), clamp(nb));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}