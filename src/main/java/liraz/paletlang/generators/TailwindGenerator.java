package liraz.paletlang.generators;

import liraz.paletlang.model.*;
import liraz.paletlang.util.HexColor;
import liraz.paletlang.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera um tema Tailwind a partir do design system. Escreve um arquivo
 * {@code tailwind.config} com {@code theme.extend} contendo a paleta
 * (cores), fontes (fontFamily), escala tipográfica (fontSize com line-height)
 * e escala de espaçamento.
 *
 * A opção {@code format} define o tipo de módulo (js/cjs/mjs/ts). Quando
 * {@code markup: true}, também gera uma página de pré-visualização simples e
 * autocontida (via Tailwind Play CDN), que renderiza cada componente usando as
 * classes utilitárias geradas, servindo como validação prática de que o tema
 * está funcionando corretamente.
 */
public final class TailwindGenerator implements Generator {

    @Override public String targetName() { return "tailwind"; }

    @Override
    public List<GeneratedFile> generate(DesignSystem ds, Target target) {
        String format = optIdent(target, "format", "js");
        String file = optString(target, "file", "tailwind.config." + extFor(format));
        boolean markup = optBool(target, "markup", false);

        String theme = buildTheme(ds);
        String config = wrapModule(format, theme);

        List<GeneratedFile> files = new ArrayList<>();
        files.add(new GeneratedFile(file, config));
        if (markup)
            files.add(new GeneratedFile(previewName(file), buildPreview(ds, theme)));
        return files;
    }

    private String buildTheme(DesignSystem ds) {
        StringBuilder e = new StringBuilder();
        e.append("    extend: {\n");

        if (!ds.colors.isEmpty()) {
            e.append("      colors: {\n");
            for (Color c : ds.colors)
                e.append("        ").append(key(c.name())).append(": '")
                 .append(HexColor.parse(c.hex()).toHex()).append("',\n");
            e.append("      },\n");
        }

        if (!ds.fonts.isEmpty()) {
            e.append("      fontFamily: {\n");
            for (Font f : ds.fonts)
                e.append("        ").append(key(f.name())).append(": ['")
                 .append(Strings.js(f.family())).append("', '").append(fallback(f)).append("'],\n");
            e.append("      },\n");
        }

        if (!ds.scale.isEmpty()) {
            e.append("      fontSize: {\n");
            for (ScaleStep s : ds.scale) {
                e.append("        ").append(key(s.name())).append(": ");
                if (s.hasLineHeight())
                    e.append("['").append(Strings.num(s.size())).append("px', { lineHeight: '")
                     .append(Strings.num(s.lineHeight())).append("' }],\n");
                else
                    e.append("'").append(Strings.num(s.size())).append("px',\n");
            }
            e.append("      },\n");
        }

        if (!ds.spacingTokens.isEmpty()) {
            e.append("      spacing: {\n");
            for (SpacingToken t : ds.spacingTokens)
                e.append("        ").append(key(t.name())).append(": '")
                 .append(Strings.num(ds.spacingPx(t))).append("px',\n");
            e.append("      },\n");
        }

        e.append("    },\n");
        return e.toString();
    }

    private String wrapModule(String format, String theme) {
        String body = "  content: [],\n  theme: {\n" + theme + "  },\n  plugins: [],\n";
        return switch (format) {
            case "mjs" -> "/** @type {import('tailwindcss').Config} */\nexport default {\n" + body + "};\n";
            case "ts" -> "import type { Config } from 'tailwindcss';\n\nexport default {\n" + body + "} satisfies Config;\n";
            default -> "/** @type {import('tailwindcss').Config} */\nmodule.exports = {\n" + body + "};\n";
        };
    }

    private String buildPreview(DesignSystem ds, String theme) {
        String title = ds.metaValue("name") != null ? ds.metaValue("name") : "Design System";
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n");
        h.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        h.append("<title>").append(Strings.html(title)).append(" — Tailwind preview</title>\n");
        h.append("<script src=\"https://cdn.tailwindcss.com\"></script>\n");
        h.append("<script>\n  tailwind.config = {\n    theme: {\n").append(theme).append("    }\n  };\n</script>\n");
        h.append("</head>\n<body class=\"p-10 bg-slate-50 font-sans\">\n");
        h.append("<h1 class=\"text-2xl font-bold mb-8\">").append(Strings.html(title))
         .append(" — component preview</h1>\n");

        if (ds.components.isEmpty()) {
            h.append("<p class=\"text-slate-500\">This design system declares no components.</p>\n");
        } else {
            h.append("<div class=\"space-y-8\">\n");
            for (Component comp : ds.components) {
                h.append("  <section>\n    <h2 class=\"text-sm uppercase tracking-wide text-slate-500 mb-2\">")
                 .append(Strings.html(comp.name())).append("</h2>\n");
                h.append("    <div class=\"").append(previewClasses(ds, comp)).append("\">")
                 .append(Strings.html(Strings.capitalize(comp.name()))).append("</div>\n");
                h.append("  </section>\n");
            }
            h.append("</div>\n");
        }
        h.append("</body>\n</html>\n");
        return h.toString();
    }

    private String previewClasses(DesignSystem ds, Component comp) {
        List<String> classes = new ArrayList<>();
        classes.add("inline-flex items-center justify-center");
        for (Prop p : comp.props()) {
            String v = Strings.slug(p.value());
            switch (p.key()) {
                case "background" -> {
                    if (ds.colorByName(p.value()) != null)
                        classes.add("bg-" + v);
                }
                case "foreground" -> {
                    if (ds.colorByName(p.value()) != null)
                        classes.add("text-" + v);
                }
                case "border" -> {
                    if (ds.colorByName(p.value()) != null)
                        classes.add("border border-" + v);
                }
                case "font" -> {
                    if (ds.fontByName(p.value()) != null)
                        classes.add("font-" + v);
                }
                case "padding" -> {
                    if (ds.spacingByName(p.value()) != null)
                        classes.add("p-" + v);
                }
                case "margin" -> {
                    if (ds.spacingByName(p.value()) != null)
                        classes.add("m-" + v);
                }
                case "gap" -> {
                    if (ds.spacingByName(p.value()) != null)
                        classes.add("gap-" + v);
                }
                case "radius" -> {
                    SpacingToken t = ds.spacingByName(p.value());
                    if (t != null)
                        classes.add("rounded-[" + Strings.num(ds.spacingPx(t)) + "px]");
                }
                default -> { }
            }
        }
        return String.join(" ", classes);
    }

    private static String fallback(Font f) {
        String hay = (f.name() + " " + f.family()).toLowerCase();
        if (hay.contains("mono"))
            return "monospace";

        if (hay.contains("serif") && !hay.contains("sans"))
            return "serif";

        return "sans-serif";
    }

    private static String extFor(String format) {
        return switch (format) {
            case "ts" -> "ts";
            case "mjs" -> "mjs";
            case "cjs" -> "cjs";
            default -> "js";
        };
    }

    private static String previewName(String configFile) {
        int dot = configFile.indexOf('.');
        String stem = dot > 0 ? configFile.substring(0, dot) : configFile;
        return stem + ".preview.html";
    }

    /**
     * object key JavaScript segura: usa forma sem aspas quando for um
     * identificador válido; caso contrário, retorna a chave entre aspas.
     */
    private static String key(String name) {
        return name.matches("[A-Za-z_$][A-Za-z0-9_$]*") ? name : "'" + Strings.js(name) + "'";
    }

    private static String optString(Target t, String key, String fallback) {
        Opt o = t.option(key);
        return o != null && o.value().kind() == Kind.STRING ? o.value().text() : fallback;
    }

    private static String optIdent(Target t, String key, String fallback) {
        Opt o = t.option(key);
        return o != null && o.value().kind() == Kind.IDENT ? o.value().text() : fallback;
    }

    private static boolean optBool(Target t, String key, boolean fallback) {
        Opt o = t.option(key);
        return o != null && o.value().kind() == Kind.BOOL ? o.value().asBool() : fallback;
    }
}
