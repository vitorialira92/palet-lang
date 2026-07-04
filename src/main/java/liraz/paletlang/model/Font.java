package liraz.paletlang.model;

public record Font(String name, String family, Integer weight, int line, int col) {
    public boolean hasWeight() {
        return weight != null;
    }
}
