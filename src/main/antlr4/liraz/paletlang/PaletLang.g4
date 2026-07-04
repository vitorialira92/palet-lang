/*
 * PaletLang
 *
 * Um programa PaletLang descreve uma identidade visual completa por meio de um
 * pequeno conjunto de blocos declarativos, mais um bloco `output` que seleciona
 * quais artefatos o compilador deve gerar (guia de estilo HTML, config/marcação
 * do Tailwind, variáveis CSS).
 *
 * Convenções da gramática
 * -----------------------
 *  - Regras do parser começam com letra minúscula (ex.: `program`, `paletteBlock`).
 *  - Regras do lexer (tokens) são MAIÚSCULAS (ex.: `IDENT`, `HEX_COLOR`).
 *  - A gramática é propositalmente tolerante à ordem dos blocos de topo, de modo
 *    que "ordem errada de blocos" NÃO é um erro de sintaxe; regras estruturais/
 *    semânticas, como "exatamente um bloco meta", são verificadas depois, na
 *    análise semântica.
 *
 */
grammar PaletLang;

/* ===========================================================================
 * REGRAS DO PARSER
 * ===========================================================================
 */

/*
 * Um programa é uma sequência de blocos de topo em qualquer ordem,
 * seguida do fim de arquivo. Permitir qualquer ordem mantém a gramática
 * permissiva; a análise semântica decide quais blocos são obrigatórios e quantos
 * podem aparecer.
 */
program
    : block* EOF
    ;

/*
 * Palavras-chave "suaves" (soft keywords).
 *
 * As palavras-chave de bloco/instrução do PaletLang (meta, font, base, token,
 * scale, ...) são termos que designers também querem usar como NOMES, por
 * exemplo, um passo da escala tipográfica chamado `base`, ou uma cor chamada
 * `primary`. Para manter a linguagem ergonômica, qualquer palavra-chave também
 * pode ser usada onde se espera um identificador/nome fornecido pelo usuário.
 * Esta regra `identifier` é o único lugar que concede essa permissão, então toda
 * posição de "nome" na gramática se refere a ela, e não ao token IDENT cru.
 * Posições de palavra-chave de verdade, como o `font` que introduz a definição
 * de uma fonte, continuam casando o literal diretamente, então não há ambiguidade.
 */
identifier
    : IDENT
    | 'meta' | 'palette' | 'typography' | 'spacing' | 'component' | 'output'
    | 'role' | 'font' | 'weight' | 'scale' | 'lineHeight' | 'base' | 'token'
    ;

/* Os cinco blocos de design mais o bloco de saída (geração de código). */
block
    : metaBlock
    | paletteBlock
    | typographyBlock
    | spacingBlock
    | componentBlock
    | outputBlock
    ;

/* ---------------------------------------------------------------------------
 * meta: metadados do projeto.
 *   meta {
 *     name: "Acme Design System"
 *     version: "1.0.0"
 *     author: "Liraz"
 *     language: "pt-BR"
 *   }
 * As entradas são pares chave/valor livres, de modo que novas chaves de metadados
 * não exigem mudança na gramática; a análise semântica valida as chaves conhecidas.
 * ---------------------------------------------------------------------------
 */
metaBlock
    : 'meta' LBRACE metaEntry* RBRACE
    ;

metaEntry
    : identifier COLON STRING
    ;

/* ---------------------------------------------------------------------------
 * palette: cores nomeadas, cada uma com um papel semântico.
 *   palette {
 *     primary   #3366FF role: primary
 *     background #FFFFFF role: background
 *     text       #111111 role: text
 *   }
 * ---------------------------------------------------------------------------
 */
paletteBlock
    : 'palette' LBRACE colorDef* RBRACE
    ;

colorDef
    : identifier HEX_COLOR ('role' COLON identifier)?
    ;

/*
 * O papel semântico (o IDENT depois de `role:`) é propositalmente um
 * identificador comum, e não um conjunto fixo de palavras-chave. Isso evita
 * colisão com nomes de cor, designers costumam nomear uma cor como `primary` ou
 * `background`, então essas palavras precisam continuar utilizáveis como
 * identificadores. O conjunto de papéis *válidos* (primary, secondary, accent,
 * background, surface, text, muted, border, success, warning, error, info) é
 * verificado durante a análise semântica, o que nos permite emitir um diagnóstico
 * amigável de "papel desconhecido" em vez de um erro de sintaxe.
 */

/* ---------------------------------------------------------------------------
 * typography: famílias de fontes e uma escala tipográfica.
 *   typography {
 *     font body    "Inter"      weight: 400
 *     font heading "Poppins"    weight: 700
 *     scale {
 *       sm   14 lineHeight: 1.4
 *       base 16 lineHeight: 1.5
 *       lg   20 lineHeight: 1.4
 *     }
 *   }
 * ---------------------------------------------------------------------------
 */
typographyBlock
    : 'typography' LBRACE typographyMember* RBRACE
    ;

typographyMember
    : fontDef
    | scaleBlock
    ;

fontDef
    : 'font' identifier STRING ('weight' COLON INT)?
    ;

scaleBlock
    : 'scale' LBRACE scaleStep* RBRACE
    ;

/* Um passo da escala: um nome, um tamanho de fonte e uma altura de linha opcional.
 * Tamanho e lineHeight aceitam INT ou NUMBER decimal. */
scaleStep
    : identifier number ('lineHeight' COLON number)?
    ;

/* ---------------------------------------------------------------------------
 * spacing: uma unidade base e tokens definidos como multiplicadores dela.
 *   spacing {
 *     base 8
 *     token xs 0.5
 *     token sm 1
 *     token md 2
 *     token lg 4
 *   }
 * ---------------------------------------------------------------------------
 */
spacingBlock
    : 'spacing' LBRACE spacingMember* RBRACE
    ;

spacingMember
    : baseDef
    | spacingToken
    ;

baseDef
    : 'base' number
    ;

spacingToken
    : 'token' identifier number
    ;

/* ---------------------------------------------------------------------------
 * component: um componente de UI que referencia tokens declarados antes.
 *   component button {
 *     background: primary
 *     foreground: text
 *     font:       body
 *     padding:    md
 *     radius:     sm
 *   }
 * O valor de cada propriedade é um identificador que deve resolver para uma cor,
 * fonte ou token de espaçamento; a resolução de referências ocorre na análise
 * semântica.
 * ---------------------------------------------------------------------------
 */
componentBlock
    : 'component' identifier LBRACE componentProp* RBRACE
    ;

componentProp
    : componentPropKey COLON identifier
    ;

/*
 * As chaves de propriedade de componente são identificadores comuns, validados na
 * análise semântica contra o conjunto suportado (background, foreground, border,
 * font, padding, margin, radius, gap). Manter como IDENT, ao invés de palavras-
 * chave fixas, deixa o lexer livre de palavras que designers também podem querer
 * como nomes de token, e nos permite produzir um diagnóstico útil de "propriedade
 * desconhecida" com sugestões.
 */
componentPropKey
    : identifier
    ;

/* ---------------------------------------------------------------------------
 * output: seleciona quais artefatos o compilador gera.
 *   output {
 *     html     { file: "styleguide.html" }
 *     tailwind { file: "tailwind.config.js" format: js markup: true }
 *     css      { file: "tokens.css" prefix: "--pl-" }
 *   }
 * Os alvos (targets) são identificadores livres, de modo que a gramática não
 * precisa mudar quando um novo backend é adicionado; a análise semântica valida os
 * alvos suportados e as opções de cada alvo.
 * ---------------------------------------------------------------------------
 */
outputBlock
    : 'output' LBRACE outputTarget* RBRACE
    ;

outputTarget
    : identifier LBRACE outputOption* RBRACE
    ;

outputOption
    : identifier COLON optionValue
    ;

/* Valores de opção podem ser strings, identificadores (ex.: js/ts), booleanos ou números. */
optionValue
    : STRING
    | BOOL
    | number
    | identifier
    ;

/* Um literal numérico que é um inteiro ou um decimal. Centralizar isto faz com
 * que tamanhos, multiplicadores e alturas de linha compartilhem uma única regra. */
number
    : INT
    | NUMBER
    ;

/* ===========================================================================
 * REGRAS DO LEXER
 * ===========================================================================
 */

/* Pontuação estrutural. */
LBRACE : '{' ;
RBRACE : '}' ;
COLON  : ':' ;

/* Literais booleanos (usados por opções de saída como `markup: true`). */
BOOL
    : 'true'
    | 'false'
    ;

/* Uma cor hexadecimal de 3 ou 6 dígitos, sem distinção de maiúsculas/minúsculas, ex.: #FFF ou #3366FF. */
HEX_COLOR
    : '#' HEX HEX HEX (HEX HEX HEX)?
    ;

fragment HEX : [0-9a-fA-F] ;

/* Um número decimal com parte fracionária, ex.: 1.5 ou 0.25. */
NUMBER
    : [0-9]+ '.' [0-9]+
    ;

/* Um número inteiro, ex.: 8 ou 400. */
INT
    : [0-9]+
    ;

/* String entre aspas duplas com sequências de escape comuns. */
STRING
    : '"' (ESC | ~["\\\r\n])* '"'
    ;

fragment ESC : '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;

/*
 * Identificadores: começam com letra/sublinhado, depois letras/dígitos/sublinhado/hífen.
 * Hífens são permitidos para que nomes de token como `pl-primary` sejam
 * identificadores válidos.
 * NOTA: esta regra aparece DEPOIS de todos os tokens de palavra-chave acima, para
 * que palavras reservadas (meta, palette, role, font, etc.) tenham precedência
 * sobre IDENT.
 */
IDENT
    : [a-zA-Z_] [a-zA-Z0-9_-]*
    ;

/* Comentários de linha e de bloco são ignorados. */
LINE_COMMENT  : '//' ~[\r\n]* -> skip ;
BLOCK_COMMENT : '/*' .*? '*/' -> skip ;

/* Espaços em branco são insignificantes e ignorados. */
WS : [ \t\r\n]+ -> skip ;
