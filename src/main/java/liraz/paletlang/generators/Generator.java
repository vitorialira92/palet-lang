package liraz.paletlang.generators;

import liraz.paletlang.model.DesignSystem;
import liraz.paletlang.model.Target;

import java.util.List;

/**
 * Pai dos geradores de código. Cada um converte um {@link DesignSystem}
 * validado em um ou mais {@link GeneratedFile}. O bloco {@code output} de um
 * programa define quais geradores serão executados e fornece a cada um suas
 * opções (nome do arquivo, prefixo, formato, etc.) por meio de {@link Target}.
 */
public interface Generator {

    /** Nome do target de saída que este gerador processa: {@code html}, {@code css}, {@code tailwind}. */
    String targetName();

    /** Gera o(s) artefato(s) para a configuração de target fornecida. */
    List<GeneratedFile> generate(DesignSystem ds, Target target);
}
