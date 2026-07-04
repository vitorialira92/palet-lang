package liraz.paletlang.build;

import liraz.paletlang.PaletLangParser;
import liraz.paletlang.PaletLangParser.*;
import liraz.paletlang.model.*;
import liraz.paletlang.model.Opt;
import liraz.paletlang.model.OptVal;
import liraz.paletlang.model.Target;
import liraz.paletlang.util.Strings;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Converte um programa PaletLang já parseado em um {@link DesignSystem}.
 * Ele apenas transpõe o que a gramática aceitou, mesclando blocos repetidos e
 * registrando a posição de origem de cada nó, sem realizar validações.
 *
 * Todas as verificações semânticas (papéis desconhecidos, referências não
 * resolvidas, nomes duplicados, contraste, etc.) ficam a cargo do analisador
 * semântico.
 */
public final class ModelBuilder {

    public DesignSystem build(ProgramContext program) {
        DesignSystem ds = new DesignSystem();
        for (BlockContext block : program.block()) {
            if (block.metaBlock() != null)
                meta(ds, block.metaBlock());
            else if (block.paletteBlock() != null)
                palette(ds, block.paletteBlock());
            else if (block.typographyBlock() != null)
                typography(ds, block.typographyBlock());
            else if (block.spacingBlock() != null)
                spacing(ds, block.spacingBlock());
            else if (block.componentBlock() != null)
                component(ds, block.componentBlock());
            else if (block.outputBlock() != null)
                output(ds, block.outputBlock());
        }
        return ds;
    }

    private void meta(DesignSystem ds, MetaBlockContext ctx) {
        if (ds.metaBlockCount == 0) {
            ds.metaLine = line(ctx);
            ds.metaCol = col(ctx);
        }

        ds.metaBlockCount++;
        for (MetaEntryContext e : ctx.metaEntry()) {
            ds.meta.add(new MetaEntry(
                    e.identifier().getText(),
                    Strings.unquote(e.STRING().getText()),
                    line(e), col(e)));
        }
    }

    private void palette(DesignSystem ds, PaletteBlockContext ctx) {
        if (ds.paletteBlockCount == 0) {
            ds.paletteLine = line(ctx);
            ds.paletteCol = col(ctx);
        }

        ds.paletteBlockCount++;
        for (ColorDefContext c : ctx.colorDef()) {
            boolean hasRole = c.identifier().size() > 1;
            String role = hasRole ? c.identifier(1).getText() : null;
            int rLine = hasRole ? line(c.identifier(1)) : 0;
            int rCol  = hasRole ? col(c.identifier(1)) : 0;
            ds.colors.add(new Color(
                    c.identifier(0).getText(),
                    c.HEX_COLOR().getText(),
                    role,
                    line(c), col(c), rLine, rCol));
        }
    }

    private void typography(DesignSystem ds, TypographyBlockContext ctx) {
        if (ds.typographyBlockCount == 0) {
            ds.typographyLine = line(ctx);
            ds.typographyCol = col(ctx);
        }

        ds.typographyBlockCount++;
        for (TypographyMemberContext m : ctx.typographyMember()) {
            if (m.fontDef() != null) {
                FontDefContext f = m.fontDef();
                Integer weight = f.INT() != null ? Integer.valueOf(f.INT().getText()) : null;
                ds.fonts.add(new Font(
                        f.identifier().getText(),
                        Strings.unquote(f.STRING().getText()),
                        weight, line(f), col(f)));
            } else if (m.scaleBlock() != null) {
                for (ScaleStepContext s : m.scaleBlock().scaleStep()) {
                    Double lh = s.number().size() > 1 ? number(s.number(1)) : null;
                    ds.scale.add(new ScaleStep(
                            s.identifier().getText(),
                            number(s.number(0)),
                            lh, line(s), col(s)));
                }
            }
        }
    }

    private void spacing(DesignSystem ds, SpacingBlockContext ctx) {
        if (ds.spacingBlockCount == 0) {
            ds.spacingLine = line(ctx);
            ds.spacingCol = col(ctx);
        }

        ds.spacingBlockCount++;
        for (SpacingMemberContext m : ctx.spacingMember()) {
            if (m.baseDef() != null) {
                if (ds.spacingBaseCount == 0) {
                    ds.spacingBase = number(m.baseDef().number());
                    ds.spacingBaseLine = line(m.baseDef());
                    ds.spacingBaseCol = col(m.baseDef());
                }
                ds.spacingBaseCount++;
            } else if (m.spacingToken() != null) {
                SpacingTokenContext t = m.spacingToken();
                ds.spacingTokens.add(new SpacingToken(
                        t.identifier().getText(),
                        number(t.number()),
                        line(t), col(t)));
            }
        }
    }

    private void component(DesignSystem ds, ComponentBlockContext ctx) {
        List<Prop> props = new ArrayList<>();
        for (ComponentPropContext p : ctx.componentProp()) {
            props.add(new Prop(
                    p.componentPropKey().getText(),
                    p.identifier().getText(),
                    line(p.componentPropKey()), col(p.componentPropKey())));
        }
        ds.components.add(new Component(ctx.identifier().getText(), props, line(ctx), col(ctx)));
    }

    private void output(DesignSystem ds, OutputBlockContext ctx) {
        if (ds.outputBlockCount == 0) {
            ds.outputLine = line(ctx);
            ds.outputCol = col(ctx);
        }

        ds.outputBlockCount++;
        for (OutputTargetContext t : ctx.outputTarget()) {
            List<Opt> opts = new ArrayList<>();
            for (OutputOptionContext o : t.outputOption()) {
                opts.add(new Opt(o.identifier().getText(), optionValue(o.optionValue()),
                        line(o), col(o)));
            }
            ds.targets.add(new Target(t.identifier().getText(), opts, line(t), col(t)));
        }
    }

    private OptVal optionValue(OptionValueContext context) {
        int l = line(context);
        int c = col(context);

        if (context.STRING() != null)
            return new OptVal(Kind.STRING, Strings.unquote(context.STRING().getText()), l, c);

        if (context.BOOL() != null)
            return new OptVal(Kind.BOOL, context.BOOL().getText(), l, c);

        if (context.number() != null)
            return new OptVal(Kind.NUMBER, context.number().getText(), l, c);

        return new OptVal(Kind.IDENT, context.identifier().getText(), l, c);
    }

    private static double number(NumberContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    private static int line(ParserRuleContext ctx) {
        return ctx.getStart().getLine();
    }

    private static int col(ParserRuleContext ctx) {
        return ctx.getStart().getCharPositionInLine() + 1;
    }
}