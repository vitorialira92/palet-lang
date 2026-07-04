package liraz.paletlang.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representação intermediária de um programa PaletLang: um modelo simples e
 * independente do output do design system, construído a partir da árvore de
 * parsing pelo {@code ModelBuilder}, validado pelo {@code SemanticAnalyzer} e
 * consumido pelos geradores de código.
 *
 * <p>Cada nó preserva sua posição original ({@code line}/{@code col}) para que
 * as validações semânticas possam apontar exatamente onde um problema ocorreu.
 * A contagem de blocos é mantida porque a gramática é intencionalmente flexível
 * (os blocos podem aparecer repetidos e em qualquer ordem); as regras de
 * unicidade (meta, palette, typography, spacing, output) são aplicadas na fase
 * de análise semântica, e não na gramática.
 */
public final class DesignSystem {
    // metadata
    public final List<MetaEntry> meta = new ArrayList<>();
    public int metaBlockCount;
    public int metaLine, metaCol;

    // palette
    public final List<Color> colors = new ArrayList<>();
    public int paletteBlockCount;
    public int paletteLine, paletteCol;

    // typography
    public final List<Font> fonts = new ArrayList<>();
    public final List<ScaleStep> scale = new ArrayList<>();
    public int typographyBlockCount;
    public int typographyLine, typographyCol;

    // spacing
    public Double spacingBase;
    public int spacingBaseCount;
    public int spacingBaseLine, spacingBaseCol;
    public final List<SpacingToken> spacingTokens = new ArrayList<>();
    public int spacingBlockCount;
    public int spacingLine, spacingCol;

    // components
    public final List<Component> components = new ArrayList<>();

    // output
    public final List<Target> targets = new ArrayList<>();
    public int outputBlockCount;
    public int outputLine, outputCol;

    public String metaValue(String key) {
        return meta.stream().filter(e -> e.key().equals(key))
                .map(MetaEntry::value).findFirst().orElse(null);
    }

    public Color colorByName(String name) {
        return colors.stream().filter(c -> c.name().equals(name)).findFirst().orElse(null);
    }

    public Color colorByRole(String role) {
        return colors.stream().filter(c -> role.equals(c.role())).findFirst().orElse(null);
    }

    public Font fontByName(String name) {
        return fonts.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
    }

    public SpacingToken spacingByName(String name) {
        return spacingTokens.stream().filter(t -> t.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Valor absoluto em pixels de um token de espaçamento (base * multiplicador);
     * 0 caso não haja base definida.
     */
    public double spacingPx(SpacingToken token) {
        return (spacingBase == null ? 0 : spacingBase) * token.multiplier();
    }
}