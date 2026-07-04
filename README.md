# PaletLang
<img width="3507" height="2480" alt="banner-img" src="https://github.com/user-attachments/assets/3015a5fd-2aee-4465-af11-aa5a91288708" />

> Escreva seu sistema de design uma única vez, em uma sintaxe legível por humanos, e gere automaticamente vários artefatos a partir dele: um guia de estilo em HTML, um arquivo de design tokens em CSS e uma configuração de tema para Tailwind CSS.

**PaletLang** é uma linguagem de domínio específico (DSL) declarativa para definir **sistemas de design visual**: paletas de cores com papéis semânticos, tipografia, escalas de espaçamento e componentes de interface. A ideia é parecida com *design tokens* do Figma ou do CSS, mas escrita em uma sintaxe própria, expressiva e fácil de ler.

Este projeto é um **compilador completo** desenvolvido para a disciplina **Construção de Compiladores** (Trabalho 6 — Prof. Daniel Lucrédio). Ele recebe um arquivo `.paletlang`, faz a **análise léxica e sintática**, aplica uma etapa de **análise semântica** (verificação de conformidade) e, por fim, faz a **geração de código**: a partir da mesma fonte, produz um ou mais artefatos escolhidos por um bloco `output`.

---

## O que é o PaletLang

PaletLang é uma linguagem **declarativa**: você descreve **o que** o sistema de design é, e não o passo a passo de como construí-lo. Um arquivo PaletLang descreve uma **identidade visual** completa por meio de seis blocos:

| Bloco | Para que serve |
|-------|----------------|
| `meta` | Metadados do projeto (nome, versão, autoria, idioma, descrição). |
| `palette` | Cores nomeadas, cada uma com um **papel semântico** (primária, fundo, texto, erro...). |
| `typography` | Famílias de fontes e a **escala tipográfica** (tamanhos, pesos, altura de linha). |
| `spacing` | Uma unidade base e **tokens de espaçamento** definidos como multiplicadores. |
| `component` | Componentes de UI que **referenciam** as cores, fontes e espaçamentos declarados. |
| `output` | Quais artefatos gerar (`html`, `css`, `tailwind`) e com quais opções. |

---

## Por que PaletLang existe

Manter a coerência visual de um produto é difícil. Cores, fontes e espaçamentos costumam ficar espalhados por planilhas, arquivos de design e folhas de estilo, e facilmente saem de sincronia. PaletLang resolve isso ao tornar o sistema de design um **artefato único, versionável e verificável**:

- **Fonte única da verdade**: todas as decisões visuais ficam em um arquivo de texto.
- **Verificação automática**: o compilador rejeita erros comuns (cores inexistentes, contraste insuficiente, escala tipográfica inconsistente) **antes** de o design chegar à implementação.
- **Geração de código**: o guia de estilo em HTML, os *tokens* em CSS e o tema do Tailwind são gerados automaticamente, sempre a partir da mesma fonte.
- **Acessibilidade desde o início**: a validação de contraste **WCAG AA** é parte do próprio processo de compilação.

---

## Sintaxe da linguagem

A gramática ANTLR em [`src/main/antlr4/liraz/paletlang/PaletLang.g4`](src/main/antlr4/liraz/paletlang/PaletLang.g4) é a **fonte única da verdade** para a análise léxica e sintática. Os blocos podem aparecer em qualquer ordem, e comentários `//` e `/* ... */` são permitidos. O exemplo abaixo resume toda a sintaxe:

```paletlang
meta {
    name: "Aurora Design System"
    version: "1.0.0"
    author: "Equipe de Design"
    language: "pt-BR"
}

palette {
    primary    #4F46E5 role: primary
    background #FFFFFF role: background
    text       #0F172A role: text
    error      #DC2626 role: error
}

typography {
    font body    "Inter"         weight: 400
    font heading "Space Grotesk" weight: 600

    scale {
        base 16 lineHeight: 1.5
        lg   20 lineHeight: 1.4
    }
}

spacing {
    base 4
    token sm 2
    token md 4
    token lg 6
}

component button {
    background: primary
    foreground: background
    font:       body
    padding:    md
    radius:     sm
}

output {
    html     { file: "aurora.html" }
    css      { file: "aurora-tokens.css" prefix: "--aurora-" }
    tailwind { file: "tailwind.config.js" format: js markup: true }
}
```

Referências importantes:
- Uma cor é `nome #HEX [role: papel]`. O **nome** é usado nas referências dos componentes; o **papel** é opcional e vem de um conjunto fechado de papéis semânticos.
- Um passo da escala é `nome TAMANHO [lineHeight: VALOR]`; uma fonte é `font nome "família" [weight: PESO]`.
- No `spacing`, cada `token nome MULT` vale `base × MULT` pixels.
- Em um `component`, cada propriedade referencia **pelo nome** algo declarado antes (uma cor, uma fonte ou um token de espaçamento).

---

## Saída: geração de código

O bloco `output` seleciona os *backends* de geração. Se o bloco for omitido, o compilador gera por padrão o guia de estilo em HTML. Três alvos estão disponíveis:

| Alvo (`target`) | O que gera | Opções |
|-----------------|------------|--------|
| `html` | Um **guia de estilo** HTML autossuficiente: paleta com contraste WCAG, espécimes de fonte, escala tipográfica renderizada, escala de espaçamento e prévias dos componentes. O próprio guia é **tematizado com as cores e fontes do sistema** que ele documenta. | `file` (nome do arquivo). |
| `css` | Um arquivo de **design tokens** em CSS: todas as cores, papéis, fontes, tamanhos e espaçamentos como *custom properties* em `:root`. | `file`, `prefix` (prefixo das variáveis, padrão `--pl-`). |
| `tailwind` | Um arquivo de **configuração do Tailwind** (`theme.extend` com cores, `fontFamily`, `fontSize` e `spacing`). Com `markup: true`, gera também uma página de prévia que renderiza cada componente usando as classes utilitárias do tema. | `file`, `format` (`js`/`cjs`/`mjs`/`ts`), `markup` (booleano). |

---

## Análise semântica

Depois de o arquivo ser reconhecido pela gramática, o compilador aplica uma etapa de **análise semântica** com verificações que a gramática, sozinha, não consegue expressar. Se qualquer **erro** for encontrado, a geração de código é abortada; **avisos** (*warnings*) não impedem a compilação. As principais verificações são:

**Estrutura**
- Cada bloco singular (`meta`, `palette`, `typography`, `spacing`, `output`, e o `base` do espaçamento) pode aparecer **no máximo uma vez**.

**Paleta e cores**
- Nomes de cor **duplicados** (erro) e papéis repetidos (aviso).
- Papel semântico fora do conjunto válido (`primary`, `secondary`, `accent`, `background`, `surface`, `text`, `muted`, `border`, `success`, `warning`, `error`, `info`) com sugestão *"did you mean ...?"* (erro).
- Ausência dos papéis `background`/`text`, dos quais dependem o contraste e o fundo da página (aviso).

**Acessibilidade (WCAG AA)**
- Contraste entre as cores de `text` e `background` abaixo de **4.5:1** (erro).
- Contraste insuficiente entre `foreground` e `background` de um componente (aviso).

**Tipografia**
- Nomes de fonte ou de passo da escala **duplicados**, tamanho <= 0 e `lineHeight` <= 0 (erros); pesos ou alturas de linha fora da faixa usual e tamanhos repetidos (avisos).

**Espaçamento**
- Bloco `spacing` sem `base`, `base` <= 0, multiplicador negativo ou token duplicado (erros).

**Componentes**
- Nome de componente ou propriedade duplicados; propriedade fora do conjunto válido (`background`, `foreground`, `border`, `font`, `padding`, `margin`, `radius`, `gap`).
- **Resolução de referências**: cada propriedade precisa apontar para algo **declarado antes** e do **tipo certo**. Uma referência a um nome inexistente é erro; uma referência ao *tipo errado* recebe uma mensagem específica (por exemplo, *"'brand' is a color, not a spacing token"*).

**Saída**
- Alvo de saída desconhecido ou duplicado (erro, com sugestão); opções com o tipo errado (por exemplo, `markup` que não é booleano) e opções desconhecidas (aviso, com sugestão).

Todas as mensagens seguem o formato de compiladores/editores `arquivo:linha:coluna: severidade: mensagem`, muitas vezes acompanhadas de uma dica (`hint:`).

---

## Como compilar

**Pré-requisitos:** JDK 21+ e Maven 3.8+.

```bash
mvn clean package
```

O Maven usa o `antlr4-maven-plugin` para gerar o *lexer*/*parser* a partir da gramática e o `maven-shade-plugin` para empacotar tudo em um *fat jar* executável:

```
target/PaletLang-1.0-SNAPSHOT.jar
```

---

## Como executar

```bash
java -jar target/PaletLang-1.0-SNAPSHOT.jar <arquivo.paletlang> [opções]
```

Ou, sem empacotar, via Maven:

```bash
mvn -q exec:java -Dexec.args="examples/valid/03-full-system.paletlang -o out"
```

**Opções da linha de comando:**

| Opção | Efeito |
|-------|--------|
| `-o, --out <dir>` | Diretório de saída dos artefatos (padrão: `out`). |
| `--check` | Apenas analisa (léxico + sintático + semântico); **não** gera arquivos. Útil para testar as etapas ALS/AS. |
| `--quiet` | Não lista os arquivos gerados. |
| `-h, --help` | Mostra a ajuda. |

**Códigos de saída** (úteis para scripts de teste): `0` sucesso, `1` erros de compilação (léxicos, sintáticos ou semânticos), `2` erro de uso ou de E/S.

Exemplo completo:

```bash
java -jar target/PaletLang-1.0-SNAPSHOT.jar examples/valid/03-full-system.paletlang -o out
# Gera: out/aurora.html, out/aurora-tokens.css,
#       out/tailwind.config.js, out/tailwind.preview.html
```

---

## Estrutura do projeto

```
src/main/antlr4/liraz/paletlang/PaletLang.g4   Gramática
src/main/java/liraz/paletlang/
├── Main.java                  CLI: opções, relatório de diagnósticos, escrita dos arquivos
├── PaletLangCompiler.java     Orquestra as fases e para na primeira que falha
├── diag/                      Coleta de erros/avisos e o ouvinte de erros de sintaxe
├── build/ModelBuilder.java    Constrói o modelo a partir da árvore sintática
├── model/DesignSystem.java    Representação intermediária do sistema de design
├── semantic/SemanticAnalyzer  Análise semântica (verificações de conformidade)
└── gen/                       Geração de código: HtmlGenerator, CssGenerator, TailwindGenerator
```

O compilador funciona em três fases executadas em ordem:

1. **ALS: análise léxica e sintática**: a gramática ANTLR reconhece o arquivo; erros de *token* ou de estrutura são reportados aqui.
2. **AS: análise semântica**: as verificações de conformidade descritas acima.
3. **GCI: geração de código**: os *backends* de saída produzem os artefatos.

---

## Exemplos

A pasta `examples/` traz casos organizados por fase, prontos para os roteiros de teste:

- **`examples/valid/`** com programas corretos, que compilam e geram artefatos.
    - `01-minimal` (só `meta` + `palette` + saída HTML), `02-typography-spacing` (adiciona tipografia, espaçamento e CSS), `03-full-system` ("Aurora", com os três *backends*).
- **`examples/invalid/`** com erros **léxicos/sintáticos** (exercitam a ALS).
    - chave `}` faltando, *token* inválido (`#12XZ`, `1.5.2`), entradas malformadas.
- **`examples/semantic/`** com programas que **passam** na análise sintática mas **falham** na semântica (exercitam a AS).
    - papel desconhecido, referências indefinidas ou do tipo errado, duplicatas, contraste insuficiente, bloco `output` inválido, e um caso **só com avisos** que ainda compila.

---

## Roteiros de teste

Os scripts abaixo compilam o projeto e rodam **todos** os exemplos, conferindo o código de saída esperado de cada um (`valid` → 0, `invalid` → 1, `semantic` → 1, exceto o caso de avisos → 0).

### Linux / macOS / WSL / Git Bash

```bash
mvn -q clean package
JAR="target/PaletLang-1.0-SNAPSHOT.jar"
pass=0; fail=0
run() { # $1 = arquivo, $2 = código esperado
  java -jar "$JAR" "$1" -o "out/$(basename "$1" .paletlang)" >/dev/null 2>&1
  code=$?
  if [ "$code" -eq "$2" ]; then
    echo "PASS  (saída $code)  $1"; pass=$((pass+1))
  else
    echo "FALHA (esperado $2, obtido $code)  $1"; fail=$((fail+1))
  fi
}
for f in examples/valid/*.paletlang;   do run "$f" 0; done
for f in examples/invalid/*.paletlang; do run "$f" 1; done
for f in examples/semantic/*.paletlang; do
  case "$f" in *06-warnings-only*) run "$f" 0;; *) run "$f" 1;; esac
done
echo "----------"; echo "Resultado: PASS=$pass  FALHA=$fail"
```

### Windows (PowerShell)

```powershell
mvn -q clean package
$jar = "target\PaletLang-1.0-SNAPSHOT.jar"
$script:pass = 0; $script:fail = 0
function Run($file, $expected) {
  $name = [IO.Path]::GetFileNameWithoutExtension($file)
  java -jar $jar $file -o "out\$name" *> $null
  if ($LASTEXITCODE -eq $expected) {
    Write-Host "PASS  (saida $LASTEXITCODE)  $file"; $script:pass++
  } else {
    Write-Host "FALHA (esperado $expected, obtido $LASTEXITCODE)  $file"; $script:fail++
  }
}
Get-ChildItem examples\valid\*.paletlang    | ForEach-Object { Run $_.FullName 0 }
Get-ChildItem examples\invalid\*.paletlang  | ForEach-Object { Run $_.FullName 1 }
Get-ChildItem examples\semantic\*.paletlang | ForEach-Object {
  if ($_.Name -like "*06-warnings-only*") { Run $_.FullName 0 } else { Run $_.FullName 1 }
}
Write-Host "----------"; Write-Host "Resultado: PASS=$($script:pass)  FALHA=$($script:fail)"
```

### Resultados esperados

| Caso | Fase exercitada | Código |
|------|-----------------|:------:|
| `valid/01-minimal` · `valid/02-typography-spacing` · `valid/03-full-system` | ALS + AS + GCI (gera arquivos) | `0` |
| `invalid/01-missing-brace` · `invalid/02-bad-token` · `invalid/03-malformed-entries` | ALS (erro léxico/sintático) | `1` |
| `semantic/01`…`semantic/05` | AS (erro semântico) | `1` |
| `semantic/06-warnings-only` | AS (apenas avisos, ainda gera HTML) | `0` |

Para inspecionar as mensagens de uma etapa específica sem gerar arquivos, use `--check`, por exemplo:

```bash
java -jar target/PaletLang-1.0-SNAPSHOT.jar examples/semantic/02-references.paletlang --check
```

---

## Minha motivação

Eu amo design em praticamente todas as suas formas. Como hobby, trabalho bastante com design gráfico: faço revistas, peças de vestuário (camisetas, moletons), cadernos, marca-páginas e pôsteres. Antes disso, estudei e trabalhei bastante com UI design, inclusive em um projeto recente na Amazon. E, para além da prática, sou apaixonada por acompanhar e estudar moda, que é uma área em que a paleta de cores também é uma questão central.

Quem trabalha com qualquer forma de design sabe: criar uma paleta de cores coerente e bem organizada é uma das tarefas mais difíceis de um projeto. É o tipo de decisão que sustenta (ou derruba) toda a identidade visual, e é fácil que ela se perca ou fique inconsistente ao longo do caminho.

Como o trabalho permitia implementar qualquer linguagem desejada, escolhi seguir por este caminho: o PaletLang une os conceitos da disciplina de Construção de Compiladores com algo genuinamente útil para o meu dia a dia e que, espero, pode ser útil também para outras pessoas que trabalham com design.
