package liraz.paletlang;

import liraz.paletlang.PaletLangCompiler.Result;
import liraz.paletlang.diag.Diagnostics.Diagnostic;
import liraz.paletlang.diag.Diagnostics.Severity;
import liraz.paletlang.gen.Generator.GeneratedFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class Main {

    public static void main(String[] args) {
        Options opts;
        try {
            opts = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("paletlang: " + e.getMessage());
            usage(System.err);
            System.exit(2);
            return;
        }
        if (opts.help) {
            usage(System.out);
            return;
        }

        Path source = Path.of(opts.input);
        if (!Files.isRegularFile(source)) {
            System.err.println("paletlang: cannot open '" + opts.input + "': no such file");
            System.exit(2);
            return;
        }

        try {
            Result result = new PaletLangCompiler().compile(source, !opts.check);
            printReport(source, result);
            if (!result.ok()) {
                System.exit(1);
                return;
            }
            if (!opts.check)
                writeArtifacts(result.files(), Path.of(opts.outDir), opts.quiet);
        } catch (IOException e) {
            System.err.println("paletlang: I/O error: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void printReport(Path source, Result result) {
        String name = source.getFileName().toString();
        List<Diagnostic> items = result.diagnostics().all().stream()
                .sorted(Comparator
                        .comparingInt((Diagnostic d) -> d.line() == 0 ? Integer.MAX_VALUE : d.line())
                        .thenComparingInt(Diagnostic::col))
                .toList();

        for (Diagnostic d : items) {
            String where = d.line() > 0 ? name + ":" + d.line() + ":" + d.col() : name;
            System.err.println(where + ": " + label(d.severity()) + ": " + d.message());
            if (d.hint() != null)
                System.err.println("        hint: " + d.hint());
        }

        long errors = result.diagnostics().count(Severity.ERROR);
        long warnings = result.diagnostics().count(Severity.WARNING);
        if (errors > 0 || warnings > 0)
            System.err.println(plural(errors, "error") + ", " + plural(warnings, "warning"));

        if (result.ok())
            System.out.println("OK - " + name + " compiled" + (warnings > 0 ? " with " + plural(warnings, "warning") : ""));
    }

    private static void writeArtifacts(List<GeneratedFile> files, Path outDir, boolean quiet) throws IOException {
        Files.createDirectories(outDir);
        if (!quiet)
            System.out.println("Wrote " + files.size() + " file" + (files.size() == 1 ? "" : "s") + " to " + outDir + "/:");
        for (GeneratedFile f : files) {
            Path target = outDir.resolve(f.filename());
            if (target.getParent() != null)
                Files.createDirectories(target.getParent());
            byte[] bytes = f.content().getBytes(StandardCharsets.UTF_8);
            Files.write(target, bytes);
            if (!quiet)
                System.out.println("  " + target + "  (" + humanSize(bytes.length) + ")");
        }
    }

    private static String label(Severity s) {
        return switch (s) {
            case ERROR -> "error";
            case WARNING -> "warning";
            case INFO -> "note";
        };
    }

    private static String plural(long n, String noun) {
        return n + " " + noun + (n == 1 ? "" : "s");
    }

    private static String humanSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        return String.format(java.util.Locale.US, "%.1f KB", kb);
    }

    private static void usage(java.io.PrintStream out) {
        out.println("""
            PaletLang

            Usage:
              paletlang <file.paletlang> [options]

            Options:
              -o, --out <dir>   Output directory for generated artifacts (default: out)
                  --check       Parse and analyze only; do not generate files
                  --quiet       Do not list written files
              -h, --help        Show this help

            Examples:
              paletlang examples/valid/03-full-system.paletlang -o out
              paletlang examples/invalid/02-bad-token.paletlang --check""");
    }

    private static final class Options {
        String input;
        String outDir = "out";
        boolean check;
        boolean quiet;
        boolean help;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "-h", "--help" -> o.help = true;
                    case "--check"      -> o.check = true;
                    case "--quiet"      -> o.quiet = true;
                    case "-o", "--out"  -> {
                        if (i + 1 >= args.length)
                            throw new IllegalArgumentException(a + " requires a directory argument");
                        o.outDir = args[++i];
                    }
                    default -> {
                        if (a.startsWith("-"))
                            throw new IllegalArgumentException("unknown option '" + a + "'");
                        if (o.input != null)
                            throw new IllegalArgumentException("more than one input file given");
                        o.input = a;
                    }
                }
            }
            if (!o.help && o.input == null)
                throw new IllegalArgumentException("no input file");
            return o;
        }
    }
}
