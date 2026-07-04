package liraz.paletlang.model;

import java.util.List;

public record Component(String name, List<Prop> props, int line, int col) {}
