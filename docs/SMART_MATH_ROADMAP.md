# LeNote — SMART MATH

Hardware: Lenovo TB128FU + Precision Pen 2. Planejamento, não implementação.

## Requisitos e tecnologias

Toggle independente, sem laço. Formatar automaticamente após pausa, sem resolver ou corrigir cálculos. Preservar conteúdo inclusive erros. Jetpack Ink, agrupamento 2D, interface MathRecognitionEngine, AST/LaTeX seguro e bloco de fórmula editável. Avaliar MyScript iink SDK (licença, custo, ABI Android, offline) versus regras geométricas e classificador local; ML Kit de texto não é parser 2D. Avaliar JLaTeXMath ou KaTeX local com cache e sem scripts/URLs externas; ONNX Runtime Mobile/LiteRT somente com modelo adequado. Não adotar SDK pago nem transmitir notas sem autorização. Em escrita mista, matemática tem precedência apenas quando classificação segura. Expressões ambíguas ou incompletas mantêm tinta. MVP números, operadores, frações, potências e raízes; evoluir para integrais, somatórios, derivadas, matrizes e sistemas. Validar revisão/página antes de transformação atômica e preservar originais após reabertura. Priorizar #27 e #33 antes de habilitar substituição. Medir precisão estrutural, latência e memória em corpus misto.

## Checklist e especificações

- [ ] [#24 — Toggle independente √x](https://github.com/LirielC/LeNote/issues/24)
- [ ] [#25 — Reconhecer números e símbolos](https://github.com/LirielC/LeNote/issues/25)
- [ ] [#26 — Agrupar expressões 2D](https://github.com/LirielC/LeNote/issues/26)
- [ ] [#27 — Avaliar motor matemático](https://github.com/LirielC/LeNote/issues/27)
- [ ] [#28 — Frações e potências](https://github.com/LirielC/LeNote/issues/28)
- [ ] [#29 — Raízes e índices](https://github.com/LirielC/LeNote/issues/29)
- [ ] [#30 — Integrais e somatórios](https://github.com/LirielC/LeNote/issues/30)
- [ ] [#31 — Matrizes e sistemas](https://github.com/LirielC/LeNote/issues/31)
- [ ] [#32 — Renderizar expressões](https://github.com/LirielC/LeNote/issues/32)
- [ ] [#33 — Originais e Undo/Redo](https://github.com/LirielC/LeNote/issues/33)

Cada Issue contém instruções técnicas e critérios de aceitação. Priorizar P0 de desempenho antes das transformações automáticas. Registrar progresso em IMPLEMENTATION_STATUS.md e resultados reais em BENCHMARKS.md. Não fechar tarefas sem testes necessários, incluindo validação física quando aplicável.
