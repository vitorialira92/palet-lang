# PaletLang
[adicionar a lgoo aq qnd estiver pronta]
> Escreva seu sistema de design uma única vez, em uma sintaxe legível por humanos, e gere automaticamente um guia de estilo visual completo em HTML.

**PaletLang** é uma linguagem de domínio específico (DSL) declarativa para definir **sistemas de design visual**: paletas de cores com papéis semânticos, tipografia, escalas de espaçamento e componentes de interface. A ideia é parecida com *design tokens* do Figma ou do CSS, mas escrita em uma sintaxe própria, expressiva e fácil de ler.

Este projeto é um **compilador completo** desenvolvido para a disciplina **Construção de Compiladores** (Trabalho 6 - Prof. Daniel Lucrédio). Ele recebe um arquivo `.paletlang`, valida sua estrutura e suas regras de conformidade, e produz como saída uma página HTML que documenta visualmente todo o sistema de design, pronta para ser usada como style guide. O arquivo inclui amostras de cor, escala tipográfica, escala de espaçamento e prévias de componentes, entre outros.

---

## O que é o PaletLang

PaletLang é uma linguagem **declarativa**, ou seja, você descreve **o que** o sistema de design é, e não o passo a passo de como construí-lo. Um arquivo PaletLang descreve uma **identidade visual** completa por meio de cinco blocos:

| Bloco | Para que serve |
|-------|----------------|
| `meta` | Metadados do projeto (descrição, autoria, versão, idioma). |
| `palette` | Cores nomeadas, cada uma com um **papel semântico** (primária, fundo, texto, erro...). |
| `typography` | Famílias de fontes e a **escala tipográfica** (tamanhos, pesos, altura de linha). |
| `spacing` | Uma unidade base e **tokens de espaçamento** definidos como multiplicadores. |
| `component` | Componentes de UI que **referenciam** as cores, fontes e espaçamentos declarados. |

Como saída, o compilador gera uma página HTML autossuficiente que serve como documentação viva do sistema de design.

---

## Por que PaletLang existe

Manter a coerência visual de um produto é difícil. Cores, fontes e espaçamentos costumam ficar espalhados por planilhas, arquivos de design e folhas de estilo, e facilmente saem de sincronia. PaletLang resolve isso ao tornar o sistema de design um **artefato único, versionável e verificável**:

- **Fonte única da verdade**: todas as decisões visuais ficam em um arquivo de texto.
- **Verificação automática**: o compilador rejeita erros comuns (cores inexistentes, contraste insuficiente, escala tipográfica inconsistente) **antes** de o design chegar à implementação.
- **Documentação gratuita**: o guia de estilo em HTML é gerado automaticamente, sempre atualizado.
- **Acessibilidade desde o início**: a validação de contraste **WCAG AA** é parte do próprio processo de compilação.

---
## Minha motivação

Eu amo design em praticamente todas as suas formas. Como hobby, trabalho bastante com design gráfico: faço revistas, peças de vestuário (camisetas, moletons), cadernos, marca-páginas e pôsteres. Antes disso, estudei e trabalhei bastante com UI design, inclusive em um projeto recente na Amazon. E, para além da prática, sou apaixonada por acompanhar e estudar moda, que é uma área em que a paleta de cores também é uma questão central.

Quem trabalha com qualquer forma de design sabe: criar uma paleta de cores coerente e bem organizada é uma das tarefas mais difíceis de um projeto. É o tipo de decisão que sustenta (ou derruba) toda a identidade visual, e é fácil que ela se perca ou fique inconsistente ao longo do caminho.

Como o trabalho permitia implementar qualquer linguagem desejada, escolhi seguir por este caminho: o PaletLang une os conceitos da disciplina de Construção de Compiladores com algo genuinamente útil para o meu dia a dia e que, espero, pode ser útil também para outras pessoas que trabalham com design.
