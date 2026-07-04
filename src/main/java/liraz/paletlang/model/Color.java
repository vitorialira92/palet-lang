package liraz.paletlang.model;

public record Color(String name, String hex, String role,
                    int line, int col, int roleLine, int roleCol) {
    public boolean hasRole() {
        return role != null;
    }
}
