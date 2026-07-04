package liraz.paletlang;

import liraz.paletlang.build.ModelBuilder;
import liraz.paletlang.diagnostics.Diagnostics;
import liraz.paletlang.diagnostics.SyntaxErrorListener;
import liraz.paletlang.generators.CssGenerator;
import liraz.paletlang.generators.Generator;
import liraz.paletlang.generators.GeneratedFile;
import liraz.paletlang.generators.HtmlGenerator;
import liraz.paletlang.generators.TailwindGenerator;
import liraz.paletlang.model.*;
import liraz.paletlang.semantic.SemanticAnalyzer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pipeline completa do compilador: executa as três fases em ordem: parsing
 * (léxico + sintático), análise semântica e geração de código, interrompendo
 * assim que alguma delas encontrar erros.
 *
 * Todas as mensagens são acumuladas em um único {@link Diagnostics}, permitindo
 * que a CLI exiba um relatório único e ordenado ao final da execução.
 */
public final class PaletLangCompiler {

    public enum Stage { PARSE_FAILED, SEMANTIC_FAILED, OK }

    public record Result(Stage stage, Diagnostics diagnostics, DesignSystem model, List<GeneratedFile> files) {
        public boolean ok() { return stage == Stage.OK; }
    }

    private final Map<String, Generator> generators = Map.of(
            "html", new HtmlGenerator(),
            "css", new CssGenerator(),
            "tailwind", new TailwindGenerator());

    /**
     * Compila um arquivo fonte. Quando {@code generate} é falso, o compilador
     * interrompe após a análise semântica (modo "--check", útil para testar as
     * entregas de ALS/AS sem gerar arquivos de saída).
     */
    public Result compile(Path source, boolean generate) throws IOException {
        Diagnostics diag = new Diagnostics();

        String text = Files.readString(source, StandardCharsets.UTF_8);
        CharStream input = CharStreams.fromString(text, source.getFileName().toString());

        PaletLangLexer lexer = new PaletLangLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new SyntaxErrorListener(diag));

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PaletLangParser parser = new PaletLangParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(diag));

        PaletLangParser.ProgramContext tree = parser.program();
        if (diag.hasErrors())
            return new Result(Stage.PARSE_FAILED, diag, null, List.of());

        DesignSystem model = new ModelBuilder().build(tree);
        new SemanticAnalyzer(diag).analyze(model);

        if (diag.hasErrors())
            return new Result(Stage.SEMANTIC_FAILED, diag, model, List.of());

        List<GeneratedFile> files = generate ? runGenerators(model) : List.of();
        return new Result(Stage.OK, diag, model, files);
    }

    /** Executa os geradores selecionados pelo bloco de output do programa. */
    private List<GeneratedFile> runGenerators(DesignSystem model) {
        List<GeneratedFile> out = new ArrayList<>();
        List<Target> targets = model.targets;

        if (targets.isEmpty()) {
            // Sem bloco de output (ou com bloco vazio): gera o guia HTML padrão
            Target html = new Target("html", List.<Opt>of(), 0, 0);
            out.addAll(generators.get("html").generate(model, html));
            return out;
        }

        for (Target target : targets) {
            Generator g = generators.get(target.name());
            if (g != null)
                out.addAll(g.generate(model, target));
        }

        return out;
    }
}
