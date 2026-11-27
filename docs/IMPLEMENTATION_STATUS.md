# LeNote — Status de implementação

Registro inicial: 24/11/2026. Planejamento criado; nenhuma implementação ou validação física confirmada.

| ID | Issue | Estado inicial |
|---|---|---|
| P0.1 | [#1 — Instrumentar o desempenho real no TB128FU](https://github.com/LirielC/LeNote/issues/1) | Instrumentado — aguardando baseline físico |
| P0.2 | [#2 — Eliminar reconstrução completa do cache a cada traço](https://github.com/LirielC/LeNote/issues/2) | Pendente |
| P0.3 | [#3 — Remover serialização da thread principal](https://github.com/LirielC/LeNote/issues/3) | Implementado na PR #34 — aguardando validação física |
| P0.4 | [#4 — Integrar MotionEventPredictor](https://github.com/LirielC/LeNote/issues/4) | Pendente |
| P0.5 | [#5 — Otimizar onStrokesFinished](https://github.com/LirielC/LeNote/issues/5) | Pendente |
| P1.1 | [#6 — Implementar índice espacial](https://github.com/LirielC/LeNote/issues/6) | Pendente |
| P1.2 | [#7 — Implementar cache por tiles com limite de RAM](https://github.com/LirielC/LeNote/issues/7) | Pendente |
| P1.3 | [#8 — Calibrar pressão da Precision Pen 2](https://github.com/LirielC/LeNote/issues/8) | Pendente |
| P1.4 | [#9 — Aprimorar palm rejection](https://github.com/LirielC/LeNote/issues/9) | Pendente |
| P2.1 | [#10 — Implementar suavização adaptativa](https://github.com/LirielC/LeNote/issues/10) | Pendente |
| P2.2 | [#11 — Otimizar PDFs, OCR e miniaturas](https://github.com/LirielC/LeNote/issues/11) | Pendente |
| P2.3 | [#12 — Realizar testes prolongados de memória](https://github.com/LirielC/LeNote/issues/12) | Pendente |

## Estados permitidos
Pendente, em andamento, implementado — aguardando validação, bloqueado, concluído. Manter roadmap e Issues sincronizados. As linhas acima representam o estado inicial; atualizar durante execução.

## Próxima tarefa
P0.1 — executar baseline reproduzível no TB128FU; instrumentação disponível, validação física pendente.

## Dependências
P0.1 orienta as decisões de desempenho; P0.2 precede a otimização de handoff e tiles; P1.1 precede P1.2. P2.3 consolida a validação após as melhorias. Rever dependências se o código atual justificar.

## Modelo de registro por tarefa
- Data, ID e Issue:
- Estado:
- Alterações e arquivos:
- Testes executados (com resultado):
- Métricas antes/depois:
- Validação física pendente:
- Riscos, regressões e bloqueios:
- Próximo passo:


## Escrita inteligente
Todas as novas tarefas começam pendentes. Ver os dois roadmaps. Priorizar estabilidade P0, histórico/persistência atômicos (#22 e #33) antes de habilitar substituição automática, e avaliação do motor matemático (#27).

| Issue | Estado |
|---|---|
| [#14](https://github.com/LirielC/LeNote/issues/14) | Pendente |
| [#15](https://github.com/LirielC/LeNote/issues/15) | Pendente |
| [#16](https://github.com/LirielC/LeNote/issues/16) | Pendente |
| [#17](https://github.com/LirielC/LeNote/issues/17) | Pendente |
| [#18](https://github.com/LirielC/LeNote/issues/18) | Pendente |
| [#19](https://github.com/LirielC/LeNote/issues/19) | Pendente |
| [#20](https://github.com/LirielC/LeNote/issues/20) | Pendente |
| [#21](https://github.com/LirielC/LeNote/issues/21) | Pendente |
| [#22](https://github.com/LirielC/LeNote/issues/22) | Pendente |
| [#23](https://github.com/LirielC/LeNote/issues/23) | Pendente |
| [#24](https://github.com/LirielC/LeNote/issues/24) | Pendente |
| [#25](https://github.com/LirielC/LeNote/issues/25) | Pendente |
| [#26](https://github.com/LirielC/LeNote/issues/26) | Pendente |
| [#27](https://github.com/LirielC/LeNote/issues/27) | Pendente |
| [#28](https://github.com/LirielC/LeNote/issues/28) | Pendente |
| [#29](https://github.com/LirielC/LeNote/issues/29) | Pendente |
| [#30](https://github.com/LirielC/LeNote/issues/30) | Pendente |
| [#31](https://github.com/LirielC/LeNote/issues/31) | Pendente |
| [#32](https://github.com/LirielC/LeNote/issues/32) | Pendente |
| [#33](https://github.com/LirielC/LeNote/issues/33) | Pendente |

## Execução — 26/11/2026 — P0.1 / Issue #1
- Estado: Instrumentado — aguardando baseline físico.
- Alterações: coletor com buffer circular limitado para latência/intervalo de input, amostras históricas e frame time P50/P95/P99; overlay atualiza a cada 500 ms, sem recomposição por amostra. `FrameMetrics` observa duração total dos frames; `android.os.Trace` marca input, finalização, serialização e rebuild/patch do cache. Diagnósticos de ponteiro incluem pressão, inclinação e orientação.
- Testes: `:app:testDebugUnitTest` — passou (44 tarefas); `:app:assembleDebug` — passou; teste do coletor verifica percentis, limite de amostras e contagem de frames acima do orçamento.
- Métricas antes/depois: não medidas.
- Validação física pendente: baseline TB128FU para escrita, borracha, zoom e autosave nas quatro densidades da Issue; Perfetto/profiler, memória/GC e ponta-a-pixel ainda requerem execução física/ferramentas externas.
- Riscos/limitações: orçamento de 16,67 ms é apenas referência nominal; FrameMetrics coleta a janela do app, não mede ponta-a-pixel e inclui telas fora do canvas. A aceitação da Issue ainda não foi cumprida.
- Próximo passo: executar e registrar medições no aparelho antes de concluir.
