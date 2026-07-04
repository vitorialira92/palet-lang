package liraz.paletlang.diagnostics;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Conecta o sistema de erros do lexer/parser do ANTLR ao coletor de
 * {@link Diagnostics}. É registrado tanto no lexer quanto no parser (após a
 * remoção do listener padrão que imprime no console), garantindo que todos os
 * erros léxicos e sintáticos sejam convertidos em diagnósticos estruturados de
 * nível {@code ERROR}, com linha e coluna, em vez de serem enviados diretamente
 * para o stderr.
 *
 * Isso é o que permite que as falhas da fase ALS (análise léxica/sintática)
 * apareçam como mensagens organizadas e coletadas nos exemplos inválidos.
 */
public final class SyntaxErrorListener extends BaseErrorListener {

    private final Diagnostics diagnostics;

    public SyntaxErrorListener(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        // O ANTLR usa colunas começando em 0; aqui ajustamos para 1-based, como na maioria dos editores
        diagnostics.error(line, charPositionInLine + 1, msg);
    }
}