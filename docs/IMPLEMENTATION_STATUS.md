# LeNote — Status de implementação

Registro inicial: 24/11/2026. Planejamento criado; nenhuma implementação ou validação física confirmada.

| ID | Issue | Estado inicial |
|---|---|---|
| P0.1 | [#1 — Instrumentar o desempenho real no TB128FU](https://github.com/LirielC/LeNote/issues/1) | Pendente |
| P0.2 | [#2 — Eliminar reconstrução completa do cache a cada traço](https://github.com/LirielC/LeNote/issues/2) | Implementado — aguardando validação |
| P0.3 | [#3 — Remover serialização da thread principal](https://github.com/LirielC/LeNote/issues/3) | Pendente |
| P0.4 | [#4 — Integrar MotionEventPredictor](https://github.com/LirielC/LeNote/issues/4) | Implementado — aguardando validação |
| P0.5 | [#5 — Otimizar onStrokesFinished](https://github.com/LirielC/LeNote/issues/5) | Pendente |
| P1.1 | [#6 — Implementar índice espacial](https://github.com/LirielC/LeNote/issues/6) | Pendente |
| P1.2 | [#7 — Implementar cache por tiles com limite de RAM](https://github.com/LirielC/LeNote/issues/7) | Pendente |
| P1.3 | [#8 — Calibrar pressão da Precision Pen 2](https://github.com/LirielC/LeNote/issues/8) | Pendente |
| P1.4 | [#9 — Aprimorar palm rejection](https://github.com/LirielC/LeNote/issues/9) | Pendente |
| P2.1 | [#10 — Implementar suavização adaptativa](https://github.com/LirielC/LeNote/issues/10) | Pendente |
| P2.2 | [#11 — Otimizar PDFs, OCR e miniaturas](https://github.com/LirielC/LeNote/issues/11) | Pendente |
| P2.3 | [#12 — Realizar testes prolongados de memória](https://github.com/LirielC/LeNote/issues/12) | Pendente |

## Execução — 26/11/2026 — P0.4 / Issue #4
- Estado: Implementado — aguardando validação.
- Alterações: dependência `androidx.input:input-motionprediction:1.0.0`; `JetpackInkEngine` registra eventos recebidos, gera no máximo uma predição por `ACTION_MOVE` com traço ativo e entrega-a ao `InProgressStrokesView.addToStroke()`. O evento previsto é reciclado e nunca entra na coleção persistida. O toggle `motionPredictionEnabled` permite comparação A/B; sequência inconsistente desativa predição e mantém o fluxo sem predição.
- Testes: `git diff --check` passou. `:app:assembleDebug` não iniciou compilação porque Android SDK/`ANDROID_HOME`/`sdk.dir` não estão configurados neste ambiente.
- Evidências/limitações: assinaturas conferidas na documentação AndroidX Input 1.0.0 e Jetpack Ink 1.0.0. Não há evidência de compilação nem de execução no TB128FU.
- Riscos/pendências: executar build e regressões com SDK disponível; medir distância ponta-traço e verificar ganchos, overshoot, pontos fantasmas e persistência A/B no TB128FU. Confirmar comportamento do preditor no caminho de eventos real da view.
- Próximo passo: validar em build e tablet; não fechar a Issue até obter evidência física e de persistência.

## Estados permitidos
Pendente, em andamento, implementado — aguardando validação, bloqueado, concluído. Manter roadmap e Issues sincronizados. As linhas acima representam o estado inicial; atualizar durante execução.

## Próxima tarefa
P0.1 — instrumentação e baseline. Se o tablet não estiver disponível, implementar instrumentação e testes automatizados, registrar a validação física pendente e só avançar em tarefas independentes.

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

## Execução — 26/11/2026 — P0.2 / Issue #2
- Estado: Implementado — aguardando validação.
- Alterações: `FinishedStrokesView.appendStrokes()` acrescenta novos traços ao bitmap estático válido; `updateStrokes()` atualiza somente a região alterada em inserções/remoções locais quando a ordem pode ser preservada. `JetpackInkEngine.onStrokesFinished()` usa o append e mantém fallback global para cache inválido, preview ativo, reordenação ou limites indisponíveis.
- Testes: `:app:testDebugUnitTest` e `:app:assembleDebug` passaram. A regressão instrumentada `verify25Strokes_colorSwitching_highlighter_eraser_undoRedo_and_noDuplication` passou no Lenovo TB128FU (Android 12): 25 traços sem rebuild global e Undo com patch regional.
- Evidências/limitações: validação automatizada executada no aparelho alvo. O teste não compara imagens de referência nem mede frame time/latência; BENCHMARKS.md não contém números de desempenho.
- Riscos/pendências: validar equivalência visual com sobreposições/transparência de marca-texto e registrar benchmark antes/depois no TB128FU.
- Próximo passo: comparação visual e medição de desempenho da P0.2.


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
