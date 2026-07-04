package liraz.paletlang.diagnostics;

/**
 * Uma única mensagem. {@code line}/{@code col} são 1-based para exibição (ou 0
 * quando a posição é desconhecida). {@code hint} é uma sugestão opcional no
 * estilo “did you mean…”, exibida logo abaixo da mensagem.
 */
public record Diagnostic(Severity severity, int line, int col, String message, String hint) {
    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public boolean isWarning() {
        return severity == Severity.WARNING;
    }
}
