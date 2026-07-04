package liraz.paletlang.semantic;

import liraz.paletlang.diagnostics.Diagnostics;
import liraz.paletlang.model.*;
import liraz.paletlang.util.HexColor;

import java.util.*;
import java.util.function.Function;

/**
 * Responsável pela análise semântica: realiza todas as validações que não podem
 * ser expressas apenas pela gramática. Opera sobre o {@link DesignSystem}
 * produzido pelo construtor e registra erros e avisos em uma instância
 * compartilhada de {@link Diagnostics}.
 *
 * As verificações são organizadas por bloco e incluem:
 * - garantia de blocos de ocorrência única;
 * - validação das chaves de metadados reconhecidas;
 * - detecção de nomes duplicados (cores, fontes, níveis da escala tipográfica,
 *   tokens de espaçamento e componentes);
 * - validação do conjunto de papéis semânticos de cores;
 * - verificação da conformidade com a WCAG AA para contraste (erro para texto
 *   sobre fundo na paleta e aviso para pares definidos em componentes);
 * - validação das escalas tipográfica e de espaçamento, garantindo valores
 *   positivos e consistentes;
 * - verificação das propriedades de cada componente e, principalmente, de que
 *   cada propriedade referencia um token previamente declarado e do tipo
 *   esperado (cor, fonte, token de espaçamento, etc.);
 * - validação dos destinos de saída e de suas respectivas opções.
 */
public final class SemanticAnalyzer {

    /** Razão mínima de contraste exigida pela WCAG 2.1 nível AA para textos de tamanho normal. */
    private static final double AA_NORMAL = 4.5;

    private static final Set<String> VALID_ROLES = Set.of(
            "primary", "secondary", "accent", "background", "surface", "text",
            "muted", "border", "success", "warning", "error", "info"
    );

    private static final Set<String> KNOWN_META_KEYS = Set.of(
            "name", "version", "author", "language", "description", "license");

    private static final Set<String> VALID_PROP_KEYS = Set.of(
            "background", "foreground", "border", "font", "padding", "margin", "radius", "gap"
    );

    private static final Set<String> COLOR_PROPS = Set.of("background", "foreground", "border");
    private static final Set<String> FONT_PROPS = Set.of("font");
    private static final Set<String> SPACING_PROPS = Set.of("padding", "margin", "radius", "gap");

    private static final Set<String> VALID_TARGETS = Set.of("html", "css", "tailwind");
    private static final Set<String> TAILWIND_FORMATS = Set.of("js", "ts", "cjs", "mjs");

    private final Diagnostics diag;

    public SemanticAnalyzer(Diagnostics diagnostics) {
        this.diag = diagnostics;
    }

    public void analyze(DesignSystem ds) {
        checkSingleBlocks(ds);
        checkMeta(ds);
        checkPalette(ds);
        checkContrast(ds);
        checkTypography(ds);
        checkSpacing(ds);
        checkComponents(ds);
        checkOutput(ds);
    }

    private void checkSingleBlocks(DesignSystem ds) {
        if (ds.metaBlockCount > 1)
            diag.error(ds.metaLine, ds.metaCol, "more than one 'meta' block; only one is allowed");
        if (ds.paletteBlockCount > 1)
            diag.error(ds.paletteLine, ds.paletteCol, "more than one 'palette' block; only one is allowed");
        if (ds.typographyBlockCount > 1)
            diag.error(ds.typographyLine, ds.typographyCol, "more than one 'typography' block; only one is allowed");
        if (ds.spacingBlockCount > 1)
            diag.error(ds.spacingLine, ds.spacingCol, "more than one 'spacing' block; only one is allowed");
        if (ds.outputBlockCount > 1)
            diag.error(ds.outputLine, ds.outputCol, "more than one 'output' block; only one is allowed");
        if (ds.spacingBaseCount > 1)
            diag.error(ds.spacingBaseLine, ds.spacingBaseCol, "more than one 'base' in the spacing block; only one is allowed");
    }

    private void checkMeta(DesignSystem ds) {
        Set<String> seen = new HashSet<>();
        for (MetaEntry e : ds.meta) {
            if (!seen.add(e.key()))
                diag.warning(e.line(), e.col(), "duplicate meta key '" + e.key() + "'; the later value is used");

            if (!KNOWN_META_KEYS.contains(e.key()))
                diag.warning(e.line(), e.col(), "unknown meta key '" + e.key() + "'", suggest(e.key(), KNOWN_META_KEYS));
        }

        if (ds.metaBlockCount > 0 && ds.metaValue("name") == null)
            diag.warning(ds.metaLine, ds.metaCol, "meta block has no 'name'; the style guide will use a placeholder title");
    }

    private void checkPalette(DesignSystem ds) {
        if (ds.paletteBlockCount == 0) {
            diag.warning(0, 0, "no 'palette' block; the design system has no colors");
            return;
        }

        if (ds.colors.isEmpty())
            diag.warning(ds.paletteLine, ds.paletteCol, "the palette is empty");

        Map<String, Color> byName = new HashMap<>();
        Map<String, Color> byRole = new HashMap<>();
        for (Color c : ds.colors) {
            Color prev = byName.putIfAbsent(c.name(), c);
            if (prev != null)
                diag.error(c.line(), c.col(),
                        "duplicate color name '" + c.name() + "' (first defined on line " + prev.line() + ")");

            if (c.hasRole()) {
                if (!VALID_ROLES.contains(c.role()))
                    diag.error(c.roleLine(), c.roleCol(),
                            "unknown color role '" + c.role() + "'", suggest(c.role(), VALID_ROLES));
                else {
                    Color prevRole = byRole.putIfAbsent(c.role(), c);
                    if (prevRole != null)
                        diag.warning(c.roleLine(), c.roleCol(),
                                "role '" + c.role() + "' is assigned to both '" + prevRole.name()
                                        + "' and '" + c.name() + "'");
                }
            }
        }

        if (ds.colorByRole("background") == null)
            diag.warning(ds.paletteLine, ds.paletteCol,
                    "no color has role 'background'; contrast checks and the page background rely on it");
        if (ds.colorByRole("text") == null)
            diag.warning(ds.paletteLine, ds.paletteCol,
                    "no color has role 'text'; contrast checks rely on it");
    }

    private void checkContrast(DesignSystem ds) {
        Color bg = ds.colorByRole("background");
        Color text = ds.colorByRole("text");
        if (bg != null && text != null) {
            double ratio = HexColor.contrast(HexColor.parse(bg.hex()), HexColor.parse(text.hex()));
            if (ratio < AA_NORMAL)
                diag.error(text.roleLine() != 0 ? text.roleLine() : text.line(),
                        text.roleLine() != 0 ? text.roleCol() : text.col(),
                        String.format(Locale.US,
                                "insufficient contrast between text '%s' and background '%s': %.2f:1 (WCAG AA requires 4.5:1)",
                                text.name(), bg.name(), ratio));
        }

        for (Component comp : ds.components) {
            Color cbg = resolvedColor(ds, comp, "background");
            Color cfg = resolvedColor(ds, comp, "foreground");
            if (cbg != null && cfg != null) {
                double ratio = HexColor.contrast(HexColor.parse(cbg.hex()), HexColor.parse(cfg.hex()));
                if (ratio < AA_NORMAL)
                    diag.warning(comp.line(), comp.col(),
                            String.format(Locale.US,
                                    "component '%s': foreground on background is %.2f:1 (below WCAG AA 4.5:1)",
                                    comp.name(), ratio));
            }
        }
    }

    private Color resolvedColor(DesignSystem ds, Component comp, String key) {
        for (Prop p : comp.props())
            if (p.key().equals(key)) return ds.colorByName(p.value());
        return null;
    }

    private void checkTypography(DesignSystem ds) {
        Set<String> fontNames = new HashSet<>();
        for (Font f : ds.fonts) {
            if (!fontNames.add(f.name()))
                diag.error(f.line(), f.col(), "duplicate font name '" + f.name() + "'");
            if (f.hasWeight() && (f.weight() < 1 || f.weight() > 1000))
                diag.warning(f.line(), f.col(),
                        "font weight " + f.weight() + " is outside the usual 1-1000 range");
        }

        Set<String> stepNames = new HashSet<>();
        Map<Double, ScaleStep> bySize = new HashMap<>();
        for (ScaleStep s : ds.scale) {
            if (!stepNames.add(s.name()))
                diag.error(s.line(), s.col(), "duplicate scale step '" + s.name() + "'");
            if (s.size() <= 0)
                diag.error(s.line(), s.col(), "scale step '" + s.name() + "' has a non-positive size");
            if (s.hasLineHeight()) {
                if (s.lineHeight() <= 0)
                    diag.error(s.line(), s.col(), "scale step '" + s.name() + "' has a non-positive lineHeight");
                else if (s.lineHeight() < 1.0 || s.lineHeight() > 2.5)
                    diag.warning(s.line(), s.col(),
                            "lineHeight " + s.lineHeight() + " on '" + s.name() + "' is unusual (typical range is 1.0-2.5)");
            }
            ScaleStep dupSize = bySize.putIfAbsent(s.size(), s);
            if (dupSize != null)
                diag.warning(s.line(), s.col(),
                        "scale steps '" + dupSize.name() + "' and '" + s.name() + "' have the same size");
        }
        if (!ds.scale.isEmpty() && stepNames.stream().noneMatch(n -> n.equals("base")))
            diag.warning(ds.typographyLine, ds.typographyCol,
                    "the type scale has no 'base' step; a 'base' anchor is recommended");
    }

    private void checkSpacing(DesignSystem ds) {
        boolean hasSpacing = ds.spacingBlockCount > 0;
        if (hasSpacing && ds.spacingBase == null)
            diag.error(ds.spacingLine, ds.spacingCol,
                    "the spacing block needs a 'base' unit; tokens are multipliers of it");
        if (ds.spacingBase != null && ds.spacingBase <= 0)
            diag.error(ds.spacingBaseLine, ds.spacingBaseCol, "spacing base must be greater than 0");

        Set<String> tokenNames = new HashSet<>();
        for (SpacingToken t : ds.spacingTokens) {
            if (!tokenNames.add(t.name()))
                diag.error(t.line(), t.col(), "duplicate spacing token '" + t.name() + "'");
            if (t.multiplier() < 0)
                diag.error(t.line(), t.col(), "spacing token '" + t.name() + "' has a negative multiplier");
        }
    }

    private void checkComponents(DesignSystem ds) {
        Set<String> componentNames = new HashSet<>();
        for (Component comp : ds.components) {
            if (!componentNames.add(comp.name()))
                diag.error(comp.line(), comp.col(), "duplicate component name '" + comp.name() + "'");

            Set<String> propKeys = new HashSet<>();
            for (Prop p : comp.props()) {
                if (!propKeys.add(p.key()))
                    diag.warning(p.line(), p.col(),
                            "duplicate property '" + p.key() + "' in component '" + comp.name() + "'");

                if (!VALID_PROP_KEYS.contains(p.key())) {
                    diag.error(p.line(), p.col(),
                            "unknown component property '" + p.key() + "'", suggest(p.key(), VALID_PROP_KEYS));
                    continue; // can't resolve a reference we don't understand
                }
                resolveReference(ds, comp, p);
            }
        }
    }

    /** Verifica se uma propriedade de componente referencia um token previamente declarado e do tipo esperado. */
    private void resolveReference(DesignSystem ds, Component comp, Prop p) {
        String v = p.value();
        if (COLOR_PROPS.contains(p.key())) {
            if (ds.colorByName(v) == null)
                diag.error(p.line(), p.col(), unresolved(comp, p, "color", v),
                        crossKindHint(ds, v, "color"));
        } else if (FONT_PROPS.contains(p.key())) {
            if (ds.fontByName(v) == null)
                diag.error(p.line(), p.col(), unresolved(comp, p, "font", v),
                        crossKindHint(ds, v, "font"));
        } else if (SPACING_PROPS.contains(p.key())) {
            if (ds.spacingByName(v) == null)
                diag.error(p.line(), p.col(), unresolved(comp, p, "spacing token", v),
                        crossKindHint(ds, v, "spacing"));
        }
    }

    private String unresolved(Component comp, Prop p, String kind, String name) {
        return "component '" + comp.name() + "': property '" + p.key() + "' references undefined " + kind + " '" + name + "'";
    }

    /**
     * Caso o nome referenciado exista, mas pertença a outro tipo de token,
     * informa esse fato, produzindo uma mensagem de erro mais útil do que
     * simplesmente indicar que o identificador é indefinido. Caso contrário,
     * sugere o nome mais próximo dentro do tipo de token esperado.
     */
    private String crossKindHint(DesignSystem ds, String name, String expected) {
        boolean isColor = ds.colorByName(name) != null;
        boolean isFont = ds.fontByName(name) != null;
        boolean isSpacing = ds.spacingByName(name) != null;
        if (!expected.equals("color") && isColor)
            return "'" + name + "' is a color, not a " + label(expected);

        if (!expected.equals("font") && isFont)
            return "'" + name + "' is a font, not a " + label(expected);

        if (!expected.equals("spacing") && isSpacing)
            return "'" + name + "' is a spacing token, not a " + label(expected);

        Set<String> pool = switch (expected) {
            case "color" -> names(ds.colors, Color::name);
            case "font" -> names(ds.fonts, Font::name);
            default -> names(ds.spacingTokens, SpacingToken::name);
        };

        return suggest(name, pool);
    }

    private static String label(String kind) {
        return kind.equals("spacing") ? "spacing token" : kind;
    }

    private void checkOutput(DesignSystem ds) {
        if (ds.outputBlockCount == 0) {
            diag.warning(0, 0, "no 'output' block; a default HTML style guide will be generated");
            return;
        }
        if (ds.targets.isEmpty())
            diag.warning(ds.outputLine, ds.outputCol,
                    "the output block is empty; a default HTML style guide will be generated");

        Set<String> seenTargets = new HashSet<>();
        for (Target t : ds.targets) {
            if (!seenTargets.add(t.name()))
                diag.error(t.line(), t.col(), "duplicate output target '" + t.name() + "'");
            if (!VALID_TARGETS.contains(t.name())) {
                diag.error(t.line(), t.col(),
                        "unknown output target '" + t.name() + "'", suggest(t.name(), VALID_TARGETS));
                continue;
            }
            checkTargetOptions(t);
        }
    }

    private void checkTargetOptions(Target t) {
        Set<String> allowed = switch (t.name()) {
            case "html" -> Set.of("file");
            case "css" -> Set.of("file", "prefix");
            case "tailwind" -> Set.of("file", "format", "markup");
            default -> Set.of();
        };
        for (Opt o : t.options()) {
            if (!allowed.contains(o.key())) {
                diag.warning(o.line(), o.col(),
                        "unknown option '" + o.key() + "' for target '" + t.name() + "'; it will be ignored",
                        suggest(o.key(), allowed));
                continue;
            }

            switch (o.key()) {
                case "file", "prefix" -> {
                    if (o.value().kind() != Kind.STRING)
                        diag.error(o.value().line(), o.value().col(),
                                "'" + o.key() + "' must be a quoted string");
                }
                case "markup" -> {
                    if (o.value().kind() != Kind.BOOL)
                        diag.error(o.value().line(), o.value().col(),
                                "'markup' must be a boolean (true or false)");
                }
                case "format" -> {
                    if (o.value().kind() != Kind.IDENT || !TAILWIND_FORMATS.contains(o.value().text()))
                        diag.warning(o.value().line(), o.value().col(),
                                "unknown tailwind format '" + o.value().text() + "'; expected one of js, ts, cjs, mjs");
                }
                default -> { /* unreachable */ }
            }
        }
    }

    private <T> Set<String> names(List<T> items, Function<T, String> f) {
        Set<String> s = new HashSet<>();
        for (T item : items) s.add(f.apply(item));
        return s;
    }

    /** Retorna uma sugestão no formato "did you mean 'x'?" para o candidato mais próximo, ou null se não houver sugestão. */
    private String suggest(String input, Collection<String> candidates) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String c : candidates) {
            int d = editDistance(input, c);
            if (d < bestDist) { bestDist = d; best = c; }
        }

        int threshold = Math.max(2, input.length() / 2);
        return (best != null && bestDist <= threshold) ? "did you mean '" + best + "'?" : null;
    }

    // calcula a distância de Levenshtein. Referência: https://en.wikipedia.org/wiki/Levenshtein_distance
    private static int editDistance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++)
            prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }
}
