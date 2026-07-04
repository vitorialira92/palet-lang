package liraz.paletlang.model;

/** Um valor de opção dentro de um target de saída, identificado pelo seu tipo de token. */
public record OptVal(Kind kind, String text, int line, int col) {
    public boolean asBool() {
        return "true".equals(text);
    }
    public double asNumber() {
        return Double.parseDouble(text);
    }
}
