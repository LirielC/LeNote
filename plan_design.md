# Plano de redesign do LeNote

## 1. Objetivo

Redesenhar somente a camada visual e de interação do LeNote para que o aplicativo pareça um produto Android maduro, próximo da sobriedade do Samsung Notes e de outros bons aplicativos de anotação, sem copiar marca, ícones proprietários ou telas pixel a pixel.

O redesign deve:

- preservar integralmente dados, comportamentos, gestos e recursos existentes;
- reduzir a aparência atual de “editorial/atelier”, que hoje usa serifas, caixa alta, capas cenográficas, muitos contornos e componentes em formato de pílula;
- priorizar escrita e leitura, com menos elementos competindo com a folha;
- funcionar bem em telefone, tablet, paisagem, tela dividida e modo escuro;
- continuar nativo em Jetpack Compose e reutilizar a estrutura atual, sem dependências visuais novas.

Este documento é uma especificação de design. Nenhuma mudança funcional está proposta nesta etapa.

## 2. Pesquisa e referências

### Mobbin

[Mobbin](https://mobbin.com/?utm_source=chatgpt.com) organiza mais de 600 mil telas de produtos publicados e separa referências por telas, fluxos, animações e elementos de UI. A lição aplicável ao LeNote é pesquisar e validar **fluxos completos**, não montar uma tela a partir de componentes bonitos isolados.

Aplicação no LeNote:

- manter padrões conhecidos de biblioteca, busca, seleção, edição e exportação;
- fazer cada estado do fluxo parecer parte do mesmo produto;
- preferir soluções já validadas em apps Android reais a novidades decorativas.

### Layers

[Layers](https://layers.to/?utm_source=chatgpt.com) apresenta uma curadoria visual aberta e pesquisável de trabalhos de design. Sua contribuição para o LeNote deve ficar no acabamento: proporção, ritmo, alinhamento e detalhes de componentes.

Aplicação no LeNote:

- usar referências de alta qualidade para refinamento, não para misturar estilos;
- evitar tendências de portfólio que prejudiquem densidade, legibilidade ou familiaridade Android;
- submeter qualquer detalhe estético ao fluxo de escrita e organização.

### Samsung Notes e Android

A referência principal de produto é o Samsung Notes: estrutura por pastas, visualização em lista ou miniaturas, foco no conteúdo real das notas, toolbar de escrita contextual, tratamento forte de PDF e ações secundárias recolhidas em menus. A documentação atual da Samsung também confirma organização por pastas, alinhamento/limpeza de escrita e exportação de PDF.

Para telas grandes, o LeNote deve seguir o padrão Android de **list-detail**, exibindo navegação e conteúdo lado a lado quando houver espaço. Controles interativos devem manter área de toque mínima de 48 dp.

Referências:

- [Organizar notas e PDFs no Samsung Notes — Samsung](https://www.samsung.com/us/support/answer/ANS10004548/)
- [Samsung Notes — Google Play](https://play.google.com/store/apps/details?id=com.samsung.android.app.notes)
- [Canonical layouts — Android Developers](https://developer.android.com/develop/adaptive-apps/guides/canonical-layouts)
- [Tamanho de alvos de toque — Android Accessibility](https://support.google.com/accessibility/android/answer/7101858)
- [Princípios do One UI — Samsung Developer](https://developer.samsung.com/one-ui/index.html)
- [Material Symbols — Google Fonts](https://fonts.google.com/icons)
- [Ícones no Jetpack Compose — Android Developers](https://developer.android.com/develop/ui/compose/graphics/images/material)
- [Icon buttons — Android Developers](https://developer.android.com/develop/ui/compose/components/icon-button)
- [Tooltips — Android Developers](https://developer.android.com/develop/ui/compose/components/tooltip)
- [Adaptive icons — Android Developers](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)

## 3. Diagnóstico da interface atual

O LeNote já possui uma identidade consistente, mas ela se aproxima mais de uma papelaria editorial do que de uma ferramenta Android de uso diário.

Os elementos que mais produzem a aparência que se deseja abandonar são:

- títulos grandes em fonte serifada;
- sobretítulos em monoespaçada, caixa alta e alto espaçamento entre letras;
- fundo marfim e paleta “atelier” como padrão global;
- cadernos desenhados como capas físicas grandes;
- contornos visíveis em quase todos os componentes;
- excesso de pílulas, chips, badges e superfícies arredondadas;
- múltiplas hierarquias visuais concorrendo na Home;
- toolbar do editor com aspecto de painel flutuante volumoso;
- diálogos com linguagem visual própria, diferente de um app One UI/Android comum.

Isso não significa tornar o app genérico. A identidade do LeNote deve vir da qualidade da folha, da escrita, das miniaturas e de um azul discreto de marca — não de ornamentos.

## 4. Princípios obrigatórios

### 4.1 Conteúdo antes do contêiner

Miniaturas das páginas, títulos e datas devem formar a hierarquia. Cards não devem existir apenas para criar caixas ao redor de tudo.

### 4.2 Familiaridade Android

Usar top app bars, menus, bottom sheets, FABs, listas e diálogos de forma reconhecível. Componentes customizados só permanecem quando dão suporte real à escrita, como canvas, toolbar de caneta, miniaturas de papel e seletor de templates.

### 4.3 Densidade calma

O visual deve ser limpo sem desperdiçar espaço. Em telefone, ações raras ficam em menus; em tablet, o espaço adicional revela contexto e navegação, não versões gigantes dos mesmos controles.

### 4.4 Estado visível, decoração mínima

Seleção, ferramenta ativa, salvamento, favorito, busca e erro precisam de estados inequívocos. Sombras, bordas e animações não substituem esses sinais.

### 4.5 Nenhuma função desaparece

Uma ação pode mudar de posição ou ir para um menu/bottom sheet, mas não pode ser removida, fundida de maneira ambígua nem ficar mais difícil de descobrir sem justificativa.

## 5. Direção visual proposta

### 5.1 Personalidade

Três adjetivos: **silencioso, preciso e confiável**.

O resultado deve parecer instalado de fábrica em um bom tablet Android, mas ainda ser reconhecível como LeNote.

### 5.2 Cores

Tema claro:

- fundo do app: `#F7F7F7`;
- superfície principal: `#FFFFFF`;
- superfície secundária: `#F1F1F3`;
- texto principal: `#1C1B1F`;
- texto secundário: `#68666B`;
- divisores: `#E4E2E6`;
- azul LeNote: `#315F9B`;
- azul selecionado suave: `#E5EEFA`;
- erro/destrutivo: vermelho semântico do Material 3.

Tema escuro:

- fundo do app: `#101012`;
- superfície principal: `#1B1B1E`;
- superfície secundária: `#242428`;
- texto principal: `#F1F0F4`;
- texto secundário: `#AAA7AF`;
- divisores: `#323238`;
- azul LeNote: `#A8C7FA`.

As cores escolhidas pelo usuário para papel, tinta e capa continuam intactas. O canvas não deve herdar automaticamente a cor da interface.

Dynamic Color pode ser avaliado depois, mas não entra no primeiro redesign porque tornaria a identidade menos previsível e ampliaria a validação necessária.

### 5.3 Tipografia

- eliminar `FontFamily.Serif` e `FontFamily.Monospace` da interface de produto;
- usar a fonte sans do sistema, com a escala Material 3 já disponível;
- títulos de tela: 28–32 sp, semibold, apenas onde houver espaço;
- títulos de seção: 18–20 sp, semibold;
- títulos de item: 16 sp, medium/semibold;
- corpo: 14–16 sp;
- metadados: 12–14 sp, cor secundária;
- evitar caixa alta e espaçamento artificial entre letras.

### 5.4 Forma e profundidade

- raios predominantes de 8–12 dp;
- 16 dp apenas em bottom sheets, diálogos e FABs;
- cards sem elevação por padrão;
- sombra apenas em elementos que realmente flutuam sobre o canvas;
- divisores de 1 dp ou mudança sutil de superfície em vez de bordas completas;
- não usar gradientes, glassmorphism, brilho, blobs ou ilustrações 3D.

### 5.5 Ícones

#### Família escolhida

Usar **Material Symbols Rounded** como única família de ícones da interface. Ela é a opção mais adequada porque:

- segue a linguagem Android atual sem parecer genérica ou excessivamente geométrica;
- tem cantos e terminações suaves próximos da linguagem One UI;
- oferece símbolos específicos para tinta, caneta, marca-texto, borracha e seleção por laço;
- possui estados outlined/filled coerentes;
- é mantida pelo Google e licenciada sob Apache 2.0;
- pode ser importada localmente sem adicionar uma biblioteca em runtime.

Não usar `androidx.compose.material:material-icons-extended`: a biblioteca antiga de Material Icons não é mais recomendada pelo Android, tem aparência anterior ao Material Symbols e pode aumentar o tempo de build. Baixar somente os Vector Drawables XML necessários pela aba Android do Google Fonts e armazená-los em `res/drawable`.

Phosphor deve permanecer apenas durante a migração. Depois que todas as referências forem substituídas e verificadas, remover `PhosphorIcons.kt`. Não manter duas famílias visíveis na versão final.

#### Configuração visual

- estilo: `Rounded`;
- tamanho padrão do desenho: 24 dp;
- peso: 400;
- grade: 0;
- tamanho óptico: 24;
- estado normal: fill 0;
- estado selecionado: fill 1 quando existir variante apropriada;
- ícones compactos de metadado: 20 dp, sem reduzir a área de toque;
- ícones de estado vazio: 32–40 dp, sem transformá-los em ilustrações;
- área interativa mínima: 48 × 48 dp;
- cor normal: `onSurfaceVariant`;
- cor ativa: `primary` sobre `secondaryContainer` ou `primaryContainer` discreto;
- cor destrutiva: `error`, sempre acompanhada de texto em menus/diálogos.

Não misturar Rounded, Outlined e Sharp. “Outlined” aqui significa apenas `FILL = 0` dentro da família Rounded.

#### Estados

- ferramenta ou destino inativo: ícone sem preenchimento;
- ferramenta ou destino ativo: versão preenchida e fundo tonal, sem aumentar o ícone;
- ação pressionada: ripple padrão Material, sem animação de escala customizada;
- ação desabilitada: opacidade definida pelo componente Material 3;
- favorito: `star` sem preenchimento/preenchido;
- salvo: `check_circle` discreto e temporário; não usar `cloud_done`, pois o app não sincroniza na nuvem;
- ações irreversíveis nunca dependem apenas da cor ou do ícone.

#### Mapeamento — navegação e biblioteca

| Ação/conceito | Material Symbol Rounded | Observação |
| --- | --- | --- |
| Voltar | `arrow_back` | Espelhar automaticamente em layouts RTL |
| Abrir painel | `menu` | Usar no editor e na Home compacta |
| Fechar | `close` | Sheets, diálogos e overlays |
| Buscar | `search` | Busca global e busca no caderno |
| Mais opções | `more_vert` | Somente ações secundárias |
| Expandir/recolher | `expand_more` / `expand_less` | Nunca usar caret de outra família |
| Reordenar | `drag_indicator` | Handle visual; manter alternativa acessível |
| Lista/grade | `view_list` / `grid_view` | Preferência persistida atual |
| Biblioteca/cadernos | `menu_book` | Não usar ícone de app do Samsung Notes |
| Seção | `folder` | Reforça a hierarquia conhecida do Android |
| Página | `description` | Para item individual de página |
| Novo caderno/página | `note_add` | Acompanhado do rótulo na ação principal |
| Favorito | `star` | Fill 0/1 como toggle |
| Lixeira | `delete` | Destino ou mover para lixeira |
| Restaurar | `restore_from_trash` | Sempre com texto |
| Excluir definitivamente | `delete_forever` | Cor de erro e confirmação |
| Idioma | `language` | Menu de preferências |
| Tema claro/escuro | `light_mode` / `dark_mode` | Mostrar o estado/ação sem ambiguidade |

#### Mapeamento — editor e escrita

| Ação/conceito | Material Symbol Rounded | Observação |
| --- | --- | --- |
| Caneta | `ink_pen` | Ferramenta principal |
| Marca-texto | `ink_highlighter` | Não usar marcador circular abstrato |
| Borracha | `ink_eraser` | Estado ativo preenchido/tonal |
| Laço | `lasso_select` | Correspondência direta com a função atual |
| Texto | `text_fields` | Inserção de bloco de texto |
| Imagem | `add_photo_alternate` | Indica inserir, não apenas visualizar |
| Pan/mão | `pan_tool` | Navegar sem desenhar |
| Desenho por toque | `touch_app` | Toggle com rótulo em tooltip |
| Desfazer/refazer | `undo` / `redo` | Desabilitar corretamente sem histórico |
| Formas | `shapes` | Draw & Hold e ferramentas geométricas |
| Cor | `palette` | Abrir seletor, não representar a cor atual sozinho |
| Modelo de papel | `texture` | Sempre acompanhado de `Papel` no menu |
| Ajustar à página | `fit_screen` | Menu de zoom |
| Aproximar/afastar | `zoom_in` / `zoom_out` | Ações repetíveis por pressão longa se desejado |
| Duplicar | `content_copy` | Página, seleção ou mídia conforme contexto |
| Alinhar à pauta | `format_line_spacing` | Rótulo obrigatório por ser menos óbvio |
| Melhorar escrita | `auto_fix_high` | Não prometer IA; o texto explica geometria |
| Embelezar com fonte | `stylus_note` | Rótulo obrigatório |
| Ação Gemini | `auto_awesome` | Sem copiar marca/estrela proprietária do Gemini |
| Diagnóstico | `bug_report` | Restrito ao menu de desenvolvimento/overflow |

#### Mapeamento — arquivos e feedback

| Ação/conceito | Material Symbol Rounded | Observação |
| --- | --- | --- |
| Importar PDF | `picture_as_pdf` | Sempre com texto |
| Importar arquivo | `file_open` | Para pacote `.lenote` |
| Exportar/compartilhar | `share` | Usar o padrão reconhecível do Android |
| Backup/pacote | `archive` | Texto diferencia backup de arquivamento |
| Salvar/salvo | `save` / `check_circle` | `save` só para ação manual; check para estado |
| Abrir resultado | `open_in_new` | Banner/Snackbar após exportação |
| Sucesso | `check_circle` | Verde apenas como apoio semântico |
| Aviso/erro | `warning` / `error` | Não usar o mesmo símbolo para ambos |
| Carregando | `progress_activity` | Preferir `CircularProgressIndicator` animado |

#### Regras de uso

- ícones de ação incomum devem vir com texto; apenas ações universais podem ficar sozinhas;
- todo icon button sem rótulo visível recebe `contentDescription` localizada e `PlainTooltip` por toque longo/hover;
- `contentDescription` descreve a ação (`Adicionar aos favoritos`), não o desenho (`estrela`);
- ícones decorativos ao lado de texto equivalente usam `contentDescription = null`;
- não usar emoji, caracteres Unicode ou texto como substituto de ícone;
- não desenhar ícones proprietários do Samsung Notes;
- não aplicar círculos ou quadrados de fundo em todos os ícones; o contêiner tonal aparece apenas para seleção, toggle ou ênfase real;
- usar nomes de recurso previsíveis, como `ic_ink_pen_24.xml` e `ic_star_filled_24.xml`;
- registrar a origem e a licença Apache 2.0 em `docs/DEPENDENCIES.md`.

### 5.6 Componentes Material 3

Material 3 deve fornecer comportamento, acessibilidade e estados; os tokens do LeNote fornecem identidade. Não aceitar a aparência padrão sem ajustar cores, formas e densidade.

| Necessidade | Componente recomendado | Uso no LeNote |
| --- | --- | --- |
| Estrutura de tela | `Scaffold` | Home e editor |
| Barra da Home | `MediumTopAppBar` colapsável | Título e ações globais |
| Barra do editor | `TopAppBar` fixo | Voltar, painel, título, favorito, overflow |
| Busca | `SearchBar`/`DockedSearchBar` | Global e painel lateral |
| Filtros principais | `SecondaryTabRow` | Todos, Favoritos e Lixeira |
| Ação de criação | `ExtendedFloatingActionButton` | `Novo caderno`; recolhe ao rolar |
| Painel em telefone | `ModalNavigationDrawer` | Cadernos, seções e páginas |
| Painel em tela grande | `PermanentNavigationDrawer` ou list-detail | Navegação persistente |
| Item de navegação | `NavigationDrawerItem` | Seleção tonal coerente |
| Linha de conteúdo | `ListItem` | Lixeira, menus extensos e resultados |
| Ferramenta | `IconToggleButton` com cores customizadas | Estado ativo/inativo da toolbar |
| Ação simples | `IconButton` | Voltar, buscar, overflow, undo/redo |
| Ajuda de ícone | `TooltipBox` + `PlainTooltip` | Toque longo, mouse e caneta em hover |
| Ações contextuais | `ModalBottomSheet` | Página, papel, exportação e laço em telefone |
| Ação ancorada curta | `DropdownMenu` | Overflow em tablet/desktop |
| Confirmação destrutiva | `AlertDialog` | Exclusão definitiva e esvaziar lixeira |
| Feedback transitório | `SnackbarHost` | Sucesso e erro recuperável |
| Progresso | `CircularProgressIndicator` | Importação/exportação |
| Separação | `HorizontalDivider` | Apenas entre grupos reais |

Evitar no primeiro ciclo:

- `NavigationBar` inferior, porque o LeNote não tem três ou mais destinos de primeiro nível independentes;
- `FilterChip` para substituir todas as abas e botões;
- `ElevatedCard` em toda a Home;
- `LargeTopAppBar` no editor, pois rouba área do papel;
- morphing expressivo de formas e botões saltitantes;
- `RichTooltip` para explicar fluxos que deveriam ser autoexplicativos;
- componentes customizados quando o equivalente Material 3 resolve o mesmo comportamento.

### 5.7 Ícone do aplicativo

O launcher icon atual — caneta azul com gradiente, brilho e fundo branco já arredondado — deve ser redesenhado porque conflita com a linguagem plana e sóbria proposta.

Direção recomendada:

- fundo sólido no azul LeNote `#315F9B`;
- símbolo branco único, combinando uma folha simples com um traço de caneta;
- desenho frontal e geométrico, legível em tamanho pequeno;
- nenhuma palavra, letra minúscula delicada, gradiente, brilho ou sombra embutida;
- não copiar a folha vermelha do Samsung Notes nem usar Material Symbol diretamente como logotipo;
- foreground e background vetoriais separados em canvas de 108 × 108 dp;
- símbolo principal contido na safe zone de até 66 × 66 dp;
- camada `monochrome` própria para ícones temáticos do Android;
- validar máscaras circular, squircle, rounded square e recorte usado por launchers Samsung;
- gerar também o asset 512 × 512 px da Play Store a partir da mesma construção.

O splash screen deve reutilizar o ícone adaptativo e uma cor sólida, sem animação de logotipo customizada no primeiro ciclo.

## 6. Arquitetura de informação

A estrutura de dados atual permanece:

`Biblioteca → Caderno → Seção → Página`

Na interface, essa hierarquia deve parecer mais simples:

- **Home/Biblioteca:** acesso rápido a recentes, cadernos, favoritos e lixeira;
- **Navegação do caderno:** caderno, seções e páginas no mesmo painel;
- **Editor:** página e ferramentas, sem duplicar navegação permanente em telefone;
- **Configurações e ações raras:** menus e bottom sheets contextuais.

Não criar novas abas principais, onboarding, conta, sincronização ou painel de IA nesta fase.

## 7. Especificação por tela

### 7.1 Home — Biblioteca

#### Telefone

1. Top app bar simples:
   - título `LeNote` ou `Notas`;
   - busca;
   - menu de mais opções.
2. O menu reúne importar PDF, importar `.lenote`, backup, idioma e tema.
3. Segmento compacto abaixo da barra: `Todos`, `Favoritos`, `Lixeira`.
4. `Recentes` aparece como faixa horizontal de miniaturas reais, sem card externo grande.
5. `Cadernos` aparece em grade de duas colunas por padrão, com opção atual de densidade preservada.
6. Cada caderno usa capa plana e discreta: faixa de cor, título, contagem e data. Remover a simulação volumosa de livro.
7. FAB circular ou estendido curto no canto inferior direito: `Nova nota` ou `Novo caderno`, conforme se decidir manter a criação centrada em cadernos. Para preservar o fluxo atual, a primeira implementação mantém `Novo caderno`.

#### Tablet e paisagem

- usar list-detail: painel lateral fixo de 280–320 dp com filtros/cadernos e painel principal para recentes/páginas;
- o painel pode recolher, preservando o comportamento atual;
- limitar a largura de conteúdo para evitar grades excessivamente largas;
- usar 3–5 colunas conforme o espaço e a densidade escolhida.

#### Estados

- loading: indicador discreto no local do conteúdo;
- vazio: ícone simples, frase curta e uma ação;
- erro: banner/Snackbar com ação de tentar novamente quando aplicável;
- sucesso de importação/exportação: Snackbar com `Abrir` ou `Compartilhar`.

### 7.2 Favoritos

- reutilizar exatamente o mesmo item visual das páginas recentes;
- mostrar caderno/seção como metadado para dar contexto;
- manter abertura direta da página;
- evitar uma composição exclusiva que pareça outro módulo.

### 7.3 Lixeira

- lista agrupada em `Cadernos` e `Páginas`;
- cada linha mostra nome, tipo e data de exclusão;
- `Restaurar` é ação clara; exclusão permanente fica em menu;
- `Esvaziar lixeira` permanece ação destrutiva separada e exige confirmação;
- nunca usar FAB vermelho como principal elemento permanente se a tela puder usar ação de menu no topo.

### 7.4 Painel de cadernos, seções e páginas

#### Telefone

- abrir como drawer/bottom sheet de largura confortável;
- título do caderno na top bar e seletor de caderno logo abaixo;
- seções como tabs roláveis, sem contornos em todas as opções;
- páginas em lista ou grade, preservando a preferência existente;
- botão `Nova página` próximo à lista, com prioridade visual moderada;
- busca no topo pesquisa título e escrita reconhecida, como já ocorre;
- PDF, backup e lixeira migram para um menu de overflow ou seção inferior discreta.

#### Tablet

- painel lateral persistente de 300–360 dp;
- redimensionamento ou recolhimento opcional pode ficar para uma fase posterior;
- manter canvas visível ao trocar páginas;
- seleção ativa indicada por fundo tonal, não por contorno grosso.

#### Menus preservados

Renomear, favoritar, duplicar, mover para cima/baixo, reordenar, alterar template e excluir continuam disponíveis. Ações do caderno e da seção também permanecem.

### 7.5 Editor/canvas

O canvas é a tela principal do produto e deve ter o mínimo de chrome possível.

#### Cabeçalho

- altura compacta, sem título serifado;
- voltar, abrir painel de páginas, nome editável, favorito e overflow;
- estado `Salvo` vira ícone/check discreto ou texto temporário;
- template/papel e ações da página ficam no overflow/bottom sheet;
- manter acesso a exportação, Gemini, diagnóstico e exclusão, mas fora da faixa principal.

#### Toolbar de escrita

Ordem recomendada:

1. caneta;
2. marca-texto;
3. borracha;
4. laço;
5. texto;
6. imagem;
7. mão/toque;
8. desfazer;
9. refazer.

Comportamento:

- telefone em retrato: barra inferior, acima da navegação do sistema;
- tablet: barra flutuante compacta ou acoplada no topo/lateral, respeitando as quatro posições já persistidas;
- ferramenta ativa: fundo tonal e pequeno indicador de cor/espessura;
- segundo toque na ferramenta ativa abre suas opções;
- opções de cor e espessura aparecem em popover/bottom sheet próximo, sem uma segunda caixa grande permanente;
- botões indisponíveis ficam visivelmente desabilitados;
- áreas de toque continuam com no mínimo 48 dp, ainda que o ícone tenha 22–24 dp.

#### Laço contextual

Quando houver seleção, substituir temporariamente a barra secundária por ações contextuais:

- mover;
- recolorir;
- duplicar;
- apagar;
- alinhar à pauta;
- melhorar geometria;
- embelezar com fonte;
- enviar ao Gemini;
- fechar seleção.

Ações menos frequentes podem ficar em `Mais`, mas nenhuma deve ser eliminada.

#### Canvas e zoom

- a folha continua sendo o foco e preserva template/cor escolhidos;
- indicador de zoom menor e translúcido, visível após gesto e acessível por toque;
- manter zoom de 25% a 500%, pan, ajuste à página e reset;
- não alterar palm rejection, stylus, toque com um dedo, gestos de dois/três dedos ou botão lateral da caneta;
- overlays de texto e imagem continuam movíveis, redimensionáveis e rotacionáveis.

### 7.6 Templates e cor do papel

- trocar o diálogo alto atual por bottom sheet em telefone e diálogo adaptativo em tablet;
- título direto: `Papel`;
- cores do papel em uma linha com nome acessível;
- templates em grade de miniaturas maiores, sem subtítulos truncados quando houver espaço;
- manter os 9 templates e as 4 cores atuais;
- ação principal `Aplicar`; cancelar pelo gesto, botão voltar ou `Cancelar`.

### 7.7 Diálogos, menus e feedback

- criação e renomeação: diálogo compacto com foco automático no campo;
- escolha de capa: swatches simples;
- ações de página/caderno: bottom sheet em telefone, menu em tablet quando houver âncora;
- confirmações destrutivas: nomear claramente o item e o efeito;
- progresso longo: banner ou indicador não bloqueante;
- sucesso: Snackbar curto;
- falha: mensagem persistente o bastante para leitura e recuperação.

## 8. Adaptação por tamanho de janela

Usar tamanho de janela, não modelo do dispositivo:

| Largura | Biblioteca | Navegação do caderno | Editor |
| --- | --- | --- | --- |
| Compacta | Uma tela, grade 1–2 colunas | Drawer/bottom sheet | Toolbar inferior |
| Média | Grade 2–3 colunas | Painel sobreposto ou estreito | Toolbar inferior/flutuante |
| Expandida | List-detail, grade 3–5 colunas | Painel persistente | Canvas + painel + toolbar acoplável |

O layout deve aceitar rotação, multi-window e fontes maiores sem cortar ações. Não fixar decisões apenas em orientação retrato/paisagem.

## 9. Acessibilidade e ergonomia

- alvos interativos de no mínimo 48 × 48 dp;
- contraste mínimo de 4,5:1 para texto comum e 3:1 para texto grande/elementos gráficos relevantes;
- `contentDescription` localizado em PT, EN e ES para controles sem texto;
- ordem de foco coerente e suporte a teclado/trackpad em tablet;
- estado selecionado exposto semanticamente, não apenas por cor;
- suporte a escala de fonte sem títulos ou botões truncados em ações críticas;
- feedback háptico apenas em seleção de ferramenta, encaixe e ações destrutivas confirmadas;
- não depender de hover, pressão ou S Pen para acessar uma função essencial.

## 10. Movimento

- transições entre Home e editor: 180–240 ms, fade/slide curto;
- abertura de painel: movimento padrão de drawer/sheet;
- troca de ferramenta: mudança tonal de 120–160 ms;
- nada de animação ornamental em cards, títulos ou listas;
- respeitar `Remove animations`/preferências de movimento do sistema;
- preservar resposta imediata da tinta: nenhuma animação Compose deve disputar o caminho de renderização do `androidx.ink`.

## 11. Matriz de preservação funcional

| Área | Recursos que não podem regredir |
| --- | --- |
| Biblioteca | recentes, busca, densidade 1/2/3, favoritos, lixeira, criar caderno |
| Cadernos | abrir, renomear, cor da capa, exportar pacote, mover à lixeira, importar `.lenote` |
| Seções | criar, selecionar, renomear, excluir |
| Páginas | criar, selecionar, renomear, favoritar, duplicar, reordenar/mover, excluir/restaurar |
| Escrita | pressão, 7 cores, 4 espessuras, suavização, marca-texto, borracha parcial |
| Seleção | laço, mover, recolorir, duplicar, apagar, alinhar, melhorar escrita, fonte, Gemini |
| Gestos | stylus, rejeição de palma, botão lateral, pan, pinch zoom, undo/redo por toque |
| Conteúdo | texto, imagem, mover, redimensionar, rotacionar |
| Papel | 9 templates, 4 cores, PDF como fundo |
| Arquivos | importar/anotar PDF, exportar PNG/PDF, pacote/backup `.lenote`, abrir/compartilhar |
| Preferências | tema sistema/claro/escuro, PT/EN/ES, toolbar em 4 posições, toque para desenhar, lista/grade |
| Persistência | salvamento automático, strokes editáveis, miniaturas, OCR/FTS5 e destaque de busca |
| Diagnóstico | overlay e ações existentes acessíveis pelo menu de desenvolvimento/mais opções |

## 12. Impacto esperado no código

A implementação deve se concentrar na camada Compose já existente:

- `LeNoteDesignSystem.kt`: substituir tokens editoriais por tokens neutros e reduzir componentes customizados;
- `res/drawable`: adicionar somente os Material Symbols Rounded usados, como Vector Drawables XML locais;
- `PhosphorIcons.kt`: manter durante a migração e excluir quando a última referência for substituída;
- `HomeScreen.kt`: nova hierarquia da biblioteca e cards simplificados;
- `CollapsibleNavigationSidebar.kt`: painel adaptativo e lista/grade mais simples;
- `InkPlaygroundScreen.kt`: cabeçalho e toolbar responsivos;
- `PageTemplatePickerDialog.kt`: sheet/diálogo adaptativo;
- `CanvasMediaOverlay.kt`: somente ajustes visuais de handles e seleção, sem mudar transformação;
- `LeNoteStrings.kt`: rótulos e descrições necessários para os novos componentes;
- `MainActivity.kt`: apenas transição/navegação visual, se necessário;
- testes de UI/ergonomia: atualizar seletores e adicionar verificações de presença e acessibilidade.

Não alterar `data`, `ink`, `export`, banco, serialização ou formatos de arquivo, salvo se uma integração visual exigir uma assinatura de callback estritamente equivalente.

## 13. Ordem de implementação

### Fase 1 — Fundação visual

- cores, tipografia, formas, elevação e estados;
- componentes básicos de barra, item, miniatura, menu e feedback;
- temas claro e escuro;
- previews compacta/média/expandida.

### Fase 2 — Biblioteca

- Home, recentes, cadernos, favoritos e lixeira;
- importação, backup, idioma e tema preservados;
- estados vazio/erro/loading.

### Fase 3 — Navegação do caderno

- painel adaptativo;
- cadernos, seções, páginas, busca e lista/grade;
- menus de gestão completos.

### Fase 4 — Editor

- cabeçalho;
- toolbar responsiva e configurações das ferramentas;
- barra contextual do laço;
- zoom e overlays.

### Fase 5 — Sheets, diálogos e polimento

- papel/templates;
- exportação, compartilhamento e Gemini;
- acessibilidade, teclado, rotação, multi-window e animações.

Cada fase deve ser integrável e testável isoladamente. Não fazer uma reescrita total de uma só vez.

## 14. Critérios de aceite

O redesign estará pronto quando:

1. todas as linhas da matriz funcional forem demonstradas no app;
2. nenhum formato de banco, `.ink`, PDF, PNG ou `.lenote` tiver sido alterado;
3. Home e editor funcionarem em largura compacta, média e expandida;
4. temas claro e escuro mantiverem contraste e legibilidade;
5. todos os controles essenciais tiverem alvo mínimo de 48 dp e descrição acessível;
6. não houver ação crítica disponível apenas por gesto oculto;
7. escrever, apagar, selecionar, aplicar undo/redo e trocar páginas mantiverem a resposta atual;
8. screenshots comparativas mostrarem menos ornamento, menos caixas e hierarquia mais clara;
9. testes unitários e instrumentados atuais continuarem passando;
10. uma revisão manual confirmar importação, edição, salvamento, reinício, exportação, restauração e exclusão permanente;
11. nenhuma tela final misturar Material Symbols e Phosphor;
12. todo icon button sem rótulo visível possuir descrição localizada e tooltip;
13. o launcher icon permanecer legível nas máscaras Android e na versão monocromática.

## 15. O que não faz parte deste redesign

- copiar logotipo, ícones, ilustrações ou assets do Samsung Notes;
- adicionar conta, nuvem, colaboração, áudio, tags ou novas funções de IA;
- modificar o motor de tinta, reconhecimento, banco ou formatos de arquivo;
- adicionar pacote completo de ícones, fonte variável de símbolos ou dependência de icon pack;
- adicionar blur, gradientes, glassmorphism ou animações demonstrativas;
- refatorar toda a arquitetura antes de mudar a interface;
- reproduzir tendências de Mobbin/Layers que não melhorem o uso real do LeNote.

O alvo não é “parecer uma cópia”; é alcançar o mesmo nível de clareza, previsibilidade e acabamento de um aplicativo de notas Android consolidado.
