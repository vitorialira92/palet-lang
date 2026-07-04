package liraz.paletlang.model;

import java.util.List;

public record Target(String name, List<Opt> options, int line, int col) {
    public Opt option(String key) {
        return options.stream().filter(o -> o.key().equals(key)).findFirst().orElse(null);
    }
}
