# LeNote — Status de implementação

Registro inicial: 24/11/2026. Planejamento criado; nenhuma implementação ou validação física confirmada.

| ID | Issue | Estado inicial |
|---|---|---|
| P0.1 | [#1 — Instrumentar o desempenho real no TB128FU](https://github.com/LirielC/LeNote/issues/1) | Pendente |
| P0.2 | [#2 — Eliminar reconstrução completa do cache a cada traço](https://github.com/LirielC/LeNote/issues/2) | Pendente |
| P0.3 | [#3 — Remover serialização da thread principal](https://github.com/LirielC/LeNote/issues/3) | Pendente |
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
