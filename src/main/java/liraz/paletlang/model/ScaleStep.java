package liraz.paletlang.model;

public record ScaleStep(String name, double size, Double lineHeight, int line, int col) {
    public boolean hasLineHeight() {
        return lineHeight != null;
    }
}
