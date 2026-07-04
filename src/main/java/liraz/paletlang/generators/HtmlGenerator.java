package liraz.paletlang.generators;

import liraz.paletlang.model.DesignSystem;
import liraz.paletlang.model.DesignSystem.*;
import liraz.paletlang.util.HexColor;
import liraz.paletlang.util.Strings;

import java.util.List;
import java.util.Locale;

/**
 * Gera um style guide HTML autocontido
 * A página documenta a paleta (amostras com seus papéis, valores hex e contraste),
 * as fontes e a escala tipográfica (renderização em tempo real), a escala de
 * espaçamento (como barras proporcionais) e todos os componentes (com pré-visualização
 * ao vivo e uma tabela com os tokens efetivamente resolvidos).
 *
 * O guia se tematiza intencionalmente com o próprio sistema que descreve: o fundo,
 * o texto, as superfícies e as cores de destaque são derivados dos papéis da paleta,
 * e as fontes de corpo e títulos vêm diretamente da tipografia definida. Assim, um
 * guia baseado em uma paleta editorial suave fica visualmente suave, enquanto um
 * baseado em uma paleta vibrante também reflete isso. Quando algum papel não está
 * definido, são usados fallbacks neutros para garantir sempre legibilidade.
 */
public final class HtmlGenerator implements Generator {

    @Override public String targetName() { return "html"; }

    @Override
    public List<GeneratedFile> generate(DesignSystem ds, Target target) {
        String file = optString(target, "file", defaultFile(ds));
        return List.of(new GeneratedFile(file, render(ds)));
    }

    private String render(DesignSystem ds) {
        Theme t = Theme.from(ds);
        String title = ds.metaValue("name") != null ? ds.metaValue("name") : "Design System";

        StringBuilder h = new StringBuilder();
        h.append("<!doctype html>\n<html lang=\"").append(lang(ds)).append("\">\n<head>\n");
        h.append("<meta charset=\"utf-8\">\n");
        h.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        h.append("<title>").append(Strings.html(title)).append(" — Style Guide</title>\n");
        h.append("<style>\n").append(css(t)).append("</style>\n");
        h.append("</head>\n<body>\n<main class=\"wrap\">\n");

        header(h, ds, title);
        if (!ds.colors.isEmpty())
            palette(h, ds);

        if (!ds.fonts.isEmpty() || !ds.scale.isEmpty())
            typography(h, ds, t);

        if (!ds.spacingTokens.isEmpty())
            spacing(h, ds);

        if (!ds.components.isEmpty())
            components(h, ds);

        footer(h, title);

        h.append("</main>\n</body>\n</html>\n");
        return h.toString();
    }

    private void header(StringBuilder h, DesignSystem ds, String title) {
        h.append("<header class=\"masthead\">\n");
        h.append("  <p class=\"eyebrow\">Style Guide</p>\n");
        h.append("  <h1>").append(Strings.html(title)).append("</h1>\n");
        StringBuilder meta = new StringBuilder();
        appendMeta(meta, "Version", ds.metaValue("version"));
        appendMeta(meta, "Author", ds.metaValue("author"));
        appendMeta(meta, "Language", ds.metaValue("language"));
        if (ds.metaValue("description") != null)
            h.append("  <p class=\"lede\">").append(Strings.html(ds.metaValue("description"))).append("</p>\n");

        if (meta.length() > 0)
            h.append("  <p class=\"meta\">").append(meta).append("</p>\n");

        h.append("</header>\n");
    }

    private void palette(StringBuilder h, DesignSystem ds) {
        Color bg = ds.colorByRole("background");
        section(h, "Palette", "colors", ds.colors.size() + " colors");
        h.append("<div class=\"swatches\">\n");

        for (Color c : ds.colors) {
            HexColor col = HexColor.parse(c.hex());
            h.append("  <figure class=\"swatch\">\n");
            h.append("    <div class=\"chip\" style=\"background:").append(col.toHex())
             .append(";color:").append(col.readableInk()).append("\">").append(col.toHex()).append("</div>\n");
            h.append("    <figcaption>\n");
            h.append("      <span class=\"name\">").append(Strings.html(c.name())).append("</span>\n");

            if (c.hasRole())
                h.append("      <span class=\"role\">").append(Strings.html(c.role())).append("</span>\n");

            if (bg != null && !c.equals(bg)) {
                double ratio = HexColor.contrast(col, HexColor.parse(bg.hex()));
                h.append("      <span class=\"contrast\">").append(String.format(Locale.US, "%.2f:1", ratio))
                 .append(contrastBadge(ratio)).append("</span>\n");
            }
            h.append("    </figcaption>\n  </figure>\n");
        }
        h.append("</div>\n</section>\n");
    }

    private void typography(StringBuilder h, DesignSystem ds, Theme t) {
        section(h, "Typography", "type", null);
        if (!ds.fonts.isEmpty()) {
            h.append("<div class=\"fonts\">\n");
            for (Font f : ds.fonts) {
                h.append("  <div class=\"font\">\n");
                h.append("    <div class=\"font-sample\" style=\"font-family:").append(Strings.html(Strings.cssFontFamily(f.family())));

                if (f.hasWeight())
                    h.append(";font-weight:").append(f.weight());

                h.append("\">").append(Strings.html(f.family())).append("</div>\n");
                h.append("    <div class=\"font-meta\"><span class=\"name\">").append(Strings.html(f.name())).append("</span>");

                if (f.hasWeight())
                    h.append(" · weight ").append(f.weight());

                h.append("</div>\n  </div>\n");
            }
            h.append("</div>\n");
        }

        if (!ds.scale.isEmpty()) {
            h.append("<div class=\"scale\">\n");
            for (ScaleStep s : ds.scale) {
                h.append("  <div class=\"scale-row\">\n");
                h.append("    <div class=\"scale-label\"><span class=\"name\">").append(Strings.html(s.name()))
                 .append("</span><span class=\"dim\">").append(Strings.num(s.size())).append("px");

                if (s.hasLineHeight())
                    h.append(" / ").append(Strings.num(s.lineHeight()));

                h.append("</span></div>\n");
                h.append("    <div class=\"scale-sample\" style=\"font-family:").append(Strings.html(t.bodyFamily))
                 .append(";font-size:").append(Strings.num(s.size())).append("px");

                if (s.hasLineHeight())
                    h.append(";line-height:").append(Strings.num(s.lineHeight()));

                h.append("\">The quick brown fox jumps over the lazy dog</div>\n");
                h.append("  </div>\n");
            }
            h.append("</div>\n");
        }
        h.append("</section>\n");
    }

    private void spacing(StringBuilder h, DesignSystem ds) {
        String note = ds.spacingBase != null ? "base " + Strings.num(ds.spacingBase) + "px" : null;
        section(h, "Spacing", "space", note);
        h.append("<div class=\"spacing\">\n");

        for (SpacingToken tok : ds.spacingTokens) {
            double px = ds.spacingPx(tok);
            h.append("  <div class=\"space-row\">\n");
            h.append("    <div class=\"space-label\"><span class=\"name\">").append(Strings.html(tok.name()))
             .append("</span><span class=\"dim\">×").append(Strings.num(tok.multiplier()))
             .append(" · ").append(Strings.num(px)).append("px</span></div>\n");
            h.append("    <div class=\"space-bar\" style=\"width:").append(Strings.num(px)).append("px\"></div>\n");
            h.append("  </div>\n");
        }
        h.append("</div>\n</section>\n");
    }

    private void components(StringBuilder h, DesignSystem ds) {
        section(h, "Components", "components", ds.components.size() + " components");
        h.append("<div class=\"components\">\n");

        for (Component comp : ds.components) {
            h.append("  <div class=\"component\">\n");
            h.append("    <h3>").append(Strings.html(comp.name())).append("</h3>\n");
            h.append("    <div class=\"component-stage\">\n");
            h.append(componentPreview(ds, comp));
            h.append("    </div>\n");
            h.append(componentSpec(ds, comp));
            h.append("  </div>\n");
        }
        h.append("</div>\n</section>\n");
    }

    private String componentPreview(DesignSystem ds, Component comp) {
        String bg = resolveColor(ds, comp, "background", "transparent");
        String fg = resolveColor(ds, comp, "foreground", "inherit");
        String fontFamily = resolveFont(ds, comp);
        String padding = resolveSpace(ds, comp, "padding", "16px");
        String radius = resolveSpace(ds, comp, "radius", "0");
        String gap = resolveSpace(ds, comp, "gap", "8px");
        String border = borderCss(ds, comp);

        StringBuilder style = new StringBuilder();
        style.append("background:").append(bg).append(";color:").append(fg)
             .append(";font-family:").append(fontFamily)
             .append(";padding:").append(padding).append(";border-radius:").append(radius)
             .append(";gap:").append(gap);

        if (border != null)
            style.append(";border:").append(border);

        return "      <div class=\"preview-box\" style=\"" + style + "\">"
                + "<span class=\"preview-dot\"></span>"
                + "<span>" + Strings.html(Strings.capitalize(comp.name())) + "</span></div>\n";
    }

    private String componentSpec(DesignSystem ds, Component comp) {
        StringBuilder s = new StringBuilder("    <table class=\"spec\">\n");
        for (Prop p : comp.props()) {
            s.append("      <tr><th>").append(Strings.html(p.key())).append("</th><td>")
             .append(Strings.html(p.value()));
            String resolved = describeResolved(ds, p);

            if (resolved != null)
                s.append(" <span class=\"resolved\">").append(Strings.html(resolved)).append("</span>");

            s.append("</td></tr>\n");
        }
        s.append("    </table>\n");
        return s.toString();
    }

    private String describeResolved(DesignSystem ds, Prop p) {
        switch (p.key()) {
            case "background", "foreground", "border" -> {
                Color c = ds.colorByName(p.value());
                return c != null ? HexColor.parse(c.hex()).toHex() : null;
            }
            case "font" -> {
                Font f = ds.fontByName(p.value());
                return f != null ? f.family() : null;
            }
            case "padding", "margin", "radius", "gap" -> {
                SpacingToken t = ds.spacingByName(p.value());
                return t != null ? Strings.num(ds.spacingPx(t)) + "px" : null;
            }
            default -> { return null; }
        }
    }

    private String resolveColor(DesignSystem ds, Component comp, String key, String fallback) {
        for (Prop p : comp.props())
            if (p.key().equals(key)) {
                Color c = ds.colorByName(p.value());
                if (c != null)
                    return HexColor.parse(c.hex()).toHex();
            }
        return fallback;
    }

    private String resolveFont(DesignSystem ds, Component comp) {
        for (Prop p : comp.props())
            if (p.key().equals("font")) {
                Font f = ds.fontByName(p.value());
                if (f != null)
                    return Strings.cssFontFamily(f.family());
            }
        return "inherit";
    }

    private String resolveSpace(DesignSystem ds, Component comp, String key, String fallback) {
        for (Prop p : comp.props())
            if (p.key().equals(key)) {
                SpacingToken t = ds.spacingByName(p.value());
                if (t != null)
                    return Strings.num(ds.spacingPx(t)) + "px";
            }
        return fallback;
    }

    private String borderCss(DesignSystem ds, Component comp) {
        for (Prop p : comp.props())
            if (p.key().equals("border")) {
                Color c = ds.colorByName(p.value());
                if (c != null) return "1px solid " + HexColor.parse(c.hex()).toHex();
            }
        return null;
    }

    private void section(StringBuilder h, String title, String id, String note) {
        h.append("<section class=\"section\" id=\"").append(id).append("\">\n");
        h.append("  <div class=\"section-head\"><h2>").append(Strings.html(title)).append("</h2>");

        if (note != null)
            h.append("<span class=\"section-note\">").append(Strings.html(note)).append("</span>");

        h.append("</div>\n");
    }

    private void footer(StringBuilder h, String title) {
        h.append("<footer class=\"colophon\">Generated by <strong>PaletLang</strong> for ")
         .append(Strings.html(title)).append(".</footer>\n");
    }

    private String contrastBadge(double ratio) {
        if (ratio >= 7.0)
            return " <b class=\"pass\">AAA</b>";

        if (ratio >= 4.5)
            return " <b class=\"pass\">AA</b>";

        if (ratio >= 3.0)
            return " <b class=\"warn\">AA Large</b>";

        return " <b class=\"fail\">Fail</b>";
    }

    private static void appendMeta(StringBuilder sb, String label, String value) {
        if (value == null) return;

        if (sb.length() > 0)
            sb.append("<span class=\"sep\">·</span>");

        sb.append("<span class=\"meta-item\"><b>").append(Strings.html(label)).append("</b> ")
          .append(Strings.html(value)).append("</span>");
    }

    private static String lang(DesignSystem ds) {
        String l = ds.metaValue("language");
        return l != null && l.matches("[A-Za-z-]{2,8}") ? l : "en";
    }

    private static String defaultFile(DesignSystem ds) {
        String name = ds.metaValue("name");
        return (name != null ? Strings.slug(name) : "styleguide") + ".html";
    }

    private static String optString(Target t, String key, String fallback) {
        Opt o = t.option(key);
        return o != null && o.value().kind() == Kind.STRING ? o.value().text() : fallback;
    }

    private record Theme(String bg, String ink, String surface, String border,
                         String muted, String accent, String bodyFamily, String headingFamily) {

        static Theme from(DesignSystem ds) {
            HexColor bg  = colorOr(ds, "background", "#ffffff");
            HexColor ink = colorOr(ds, "text", "#14161c");
            HexColor accent = firstOf(ds, "#4f46e5", "primary", "accent", "secondary");
            HexColor surface = roleColor(ds, "surface");
            HexColor border = roleColor(ds, "border");
            HexColor muted = roleColor(ds, "muted");

            String surfaceHex = surface != null ? surface.toHex() : bg.mix(ink, 0.045).toHex();
            String borderHex  = border  != null ? border.toHex()  : bg.mix(ink, 0.14).toHex();
            String mutedHex   = muted   != null ? muted.toHex()   : ink.mix(bg, 0.42).toHex();

            String system = "system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif";
            String body = familyOr(ds, system, "body");
            String heading = familyOr(ds, body, "heading", "display", "title");
            return new Theme(bg.toHex(), ink.toHex(), surfaceHex, borderHex, mutedHex,
                    accent.toHex(), body, heading);
        }

        private static HexColor colorOr(DesignSystem ds, String role, String fallback) {
            Color c = ds.colorByRole(role);
            return HexColor.parse(c != null ? c.hex() : fallback);
        }

        private static HexColor roleColor(DesignSystem ds, String role) {
            Color c = ds.colorByRole(role);
            return c != null ? HexColor.parse(c.hex()) : null;
        }

        private static HexColor firstOf(DesignSystem ds, String fallback, String... roles) {
            for (String r : roles) {
                Color c = ds.colorByRole(r);
                if (c != null)
                    return HexColor.parse(c.hex());
            }
            return HexColor.parse(fallback);
        }

        private static String familyOr(DesignSystem ds, String fallback, String... names) {
            for (String n : names) {
                Font f = ds.fontByName(n);
                if (f != null)
                    return Strings.cssFontFamily(f.family()) + ", " + fallback;
            }
            return !ds.fonts.isEmpty()
                    ? Strings.cssFontFamily(ds.fonts.get(0).family()) + ", " + fallback
                    : fallback;
        }
    }

    private String css(Theme t) {
        return """
            :root {
              --bg: %s; --ink: %s; --surface: %s; --border: %s; --muted: %s; --accent: %s;
              --body: %s; --heading: %s;
            }
            * { box-sizing: border-box; }
            html { scroll-behavior: smooth; }
            body { margin: 0; background: var(--bg); color: var(--ink);
                   font-family: var(--body); line-height: 1.55;
                   -webkit-font-smoothing: antialiased; }
            .wrap { max-width: 960px; margin: 0 auto; padding: 72px 24px 96px; }
            .masthead { border-bottom: 1px solid var(--border); padding-bottom: 32px; margin-bottom: 48px; }
            .eyebrow { font-size: 12px; letter-spacing: .18em; text-transform: uppercase;
                       color: var(--accent); margin: 0 0 12px; font-weight: 600; }
            h1 { font-family: var(--heading); font-size: clamp(34px, 6vw, 56px); line-height: 1.05;
                 margin: 0; letter-spacing: -0.02em; }
            .lede { color: var(--muted); font-size: 18px; max-width: 60ch; margin: 18px 0 0; }
            .meta { color: var(--muted); font-size: 14px; margin: 18px 0 0; }
            .meta b { color: var(--ink); font-weight: 600; }
            .meta .sep { margin: 0 10px; opacity: .5; }
            .section { margin-top: 64px; }
            .section-head { display: flex; align-items: baseline; gap: 14px;
                            border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 28px; }
            .section-head h2 { font-family: var(--heading); font-size: 13px; letter-spacing: .16em;
                               text-transform: uppercase; margin: 0; }
            .section-note { font-size: 12px; color: var(--muted); font-family: var(--body); }
            .name { font-weight: 600; }
            .dim, .role, .contrast { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }

            .swatches { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 16px; }
            .swatch { margin: 0; border: 1px solid var(--border); border-radius: 12px; overflow: hidden;
                      background: var(--surface); }
            .chip { height: 88px; display: flex; align-items: flex-end; justify-content: flex-end;
                    padding: 8px 10px; font-family: ui-monospace, Menlo, monospace; font-size: 11px; }
            figcaption { padding: 12px 12px 14px; display: flex; flex-direction: column; gap: 4px; }
            .role { color: var(--muted); text-transform: uppercase; letter-spacing: .08em; }
            .contrast { color: var(--muted); display: flex; align-items: center; gap: 6px; }
            .contrast b { font-size: 10px; padding: 1px 6px; border-radius: 999px; letter-spacing: .04em; }
            .pass { background: color-mix(in srgb, var(--accent) 16%%, transparent); color: var(--accent); }
            .warn { background: #fff3cd; color: #7a5b00; }
            .fail { background: #fde2e1; color: #b42318; }

            .fonts { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 20px; margin-bottom: 36px; }
            .font { border: 1px solid var(--border); border-radius: 12px; padding: 22px; background: var(--surface); }
            .font-sample { font-size: 34px; line-height: 1.1; }
            .font-meta { margin-top: 14px; color: var(--muted); font-size: 13px; }
            .font-meta .name { color: var(--ink); }
            .scale { display: flex; flex-direction: column; gap: 14px; }
            .scale-row { display: grid; grid-template-columns: 120px 1fr; gap: 20px; align-items: baseline;
                         border-bottom: 1px dashed var(--border); padding-bottom: 14px; }
            .scale-label { display: flex; flex-direction: column; gap: 3px; }
            .scale-label .dim { color: var(--muted); }
            .scale-sample { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }

            .spacing { display: flex; flex-direction: column; gap: 12px; }
            .space-row { display: grid; grid-template-columns: 160px 1fr; gap: 20px; align-items: center; }
            .space-label { display: flex; flex-direction: column; gap: 2px; }
            .space-label .dim { color: var(--muted); }
            .space-bar { height: 18px; border-radius: 5px; background: var(--accent); min-width: 2px;
                         opacity: .85; }

            .components { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 24px; }
            .component { border: 1px solid var(--border); border-radius: 14px; padding: 20px; background: var(--surface); }
            .component h3 { font-family: var(--heading); margin: 0 0 16px; font-size: 15px; }
            .component-stage { display: flex; align-items: center; justify-content: center; min-height: 96px;
                               border-radius: 10px; padding: 20px; margin-bottom: 16px;
                               background: repeating-conic-gradient(var(--bg) 0 25%%, color-mix(in srgb, var(--ink) 4%%, var(--bg)) 0 50%%) 0 0 / 18px 18px; }
            .preview-box { display: inline-flex; align-items: center; }
            .preview-dot { width: .6em; height: .6em; border-radius: 999px; background: currentColor; opacity: .55; }
            .spec { width: 100%%; border-collapse: collapse; font-size: 13px; }
            .spec th, .spec td { text-align: left; padding: 7px 0; border-top: 1px solid var(--border);
                                 vertical-align: top; }
            .spec th { color: var(--muted); font-weight: 500; width: 40%%; text-transform: capitalize; }
            .resolved { color: var(--muted); font-family: ui-monospace, Menlo, monospace; font-size: 11px; }

            .colophon { margin-top: 72px; padding-top: 24px; border-top: 1px solid var(--border);
                        color: var(--muted); font-size: 13px; }
            @media (max-width: 560px) {
              .scale-row, .space-row { grid-template-columns: 1fr; gap: 6px; }
              .wrap { padding: 48px 18px 72px; }
            }
            """.formatted(t.bg, t.ink, t.surface, t.border, t.muted, t.accent, t.bodyFamily, t.headingFamily);
    }
}
