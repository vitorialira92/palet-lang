package liraz.paletlang.diagnostics;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrupa as mensagens geradas durante a compilação de um programa PaletLang:
 * erros léxicos e sintáticos do parser, além de erros e avisos semânticos
 * emitidos durante a análise.
 *
 * Um único coletor é compartilhado por toda a pipeline para permitir que o
 * compilador reporte tudo de uma vez e, a partir de {@link #hasErrors()},
 * decida se é seguro continuar para a geração de código.
 */
public final class Diagnostics {

    private final List<Diagnostic> items = new ArrayList<>();

    public void error(int line, int col, String message) {
        add(Severity.ERROR, line, col, message, null);
    }

    public void error(int line, int col, String message, String hint) {
        add(Severity.ERROR, line, col, message, hint);
    }

    public void warning(int line, int col, String message) {
        add(Severity.WARNING, line, col, message, null);
    }

    public void warning(int line, int col, String message, String hint) {
        add(Severity.WARNING, line, col, message, hint);
    }

    private void add(Severity s, int line, int col, String message, String hint) {
        items.add(new Diagnostic(s, line, col, message, hint));
    }

    public boolean hasErrors() {
        return items.stream().anyMatch(Diagnostic::isError);
    }

    public long count(Severity s) {
        return items.stream().filter(d -> d.severity() == s).count();
    }

    public List<Diagnostic> all() {
        return List.copyOf(items);
    }
}