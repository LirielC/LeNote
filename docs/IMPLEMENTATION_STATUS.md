# LeNote — Status de implementação

Registro inicial: 24/11/2026. Planejamento criado; nenhuma implementação ou validação física confirmada.

| ID | Issue | Estado inicial |
|---|---|---|
| P0.1 | [#1 — Instrumentar o desempenho real no TB128FU](https://github.com/LirielC/LeNote/issues/1) | Pendente |
| P0.2 | [#2 — Eliminar reconstrução completa do cache a cada traço](https://github.com/LirielC/LeNote/issues/2) | Pendente |
| P0.3 | [#3 — Remover serialização da thread principal](https://github.com/LirielC/LeNote/issues/3) | Implementado — aguardando validação física |
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
P0.3 — validar durabilidade após trocas rápidas de página e reabertura no TB128FU; sem aparelho, manter aguardando validação física.

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

## Execução — 26/11/2026 — P0.3 / Issue #3
- Estado: Implementado — aguardando validação física.
- Alterações: `JetpackInkEngine.kt` agora captura metadados e `ImmutableStrokeInputBatch` no thread proprietário; a codificação do formato existente ocorre em `Dispatchers.Default`. `InkPlaygroundViewModel.kt` mantém flushes sem bloqueio, associa snapshots à página/revisão, serializa gravações concorrentes e grava arquivos/banco em `Dispatchers.IO`. A miniatura também é gerada fora da main thread e liberada após uso.
- Testes: `:app:testDebugUnitTest` — passou (44 tarefas); `:app:assembleDebug` — passou. `git diff --check` — passou.
- Métricas antes/depois: não medidas; nenhuma alegação de ganho de desempenho.
- Validação física pendente: trocas rápidas de página, encerramento e reabertura no TB128FU; confirmar integridade e ausência de perda.
- Riscos/bloqueios: assinatura não demonstra, por si só, segurança de API nativa no aparelho; snapshots usam `ImmutableStrokeInputBatch` e a codificação é isolada em worker.
- Próximo passo: executar cenário físico de durabilidade e registrar evidência antes de concluir a Issue.
