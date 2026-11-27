# LeNote — SMART HANDWRITING

Hardware: Lenovo TB128FU + Precision Pen 2. Planejamento, não implementação.

## Requisitos e tecnologias

Reutilizar DigitalInkManager, ML Kit Digital Ink offline após download (pt-BR), Jetpack Ink, CanvasTextBlock, Compose TextMeasurer/TextField e Coroutines. Avaliar RecognitionContext/WritingArea conforme API. Toggle na toolbar, sem laço, com modos desligado, natural (preserva traços), fonte manuscrita ou digitada. Fontes locais licenciadas Caveat, Kalam, Patrick Hand, Nunito ou Inter; One Euro Filter somente após profiling. A tinta aparece imediatamente; agrupar automaticamente por espaço e tempo, aguardar pausa experimental de 500 ms (ajustável 200–1500 ms), reconhecer em worker limitado e substituir somente após validar página, revisão e qualidade. Não presumir confiança calibrada do ML Kit. Preservar originais recuperáveis após reabertura e Undo/Redo atômico de tinta e texto. Cancelar trabalhos ao desativar ou mudar página. Não alterar retrospectivamente. Ambiguidades preservam tinta; proteger desenhos e fórmulas. Priorizar #22 antes de habilitar substituição automática. Testar CER/WER, acentos, escrita cursiva e de forma, zoom, jank e memória.

## Checklist e especificações

- [ ] [#14 — Toggle e modos natural/manuscrito/digitado](https://github.com/LirielC/LeNote/issues/14)
- [ ] [#15 — Agrupar palavras e linhas](https://github.com/LirielC/LeNote/issues/15)
- [ ] [#16 — Reconhecimento assíncrono ML Kit](https://github.com/LirielC/LeNote/issues/16)
- [ ] [#17 — Normalização geométrica](https://github.com/LirielC/LeNote/issues/17)
- [ ] [#18 — Substituição por fontes](https://github.com/LirielC/LeNote/issues/18)
- [ ] [#19 — Uniformizar tamanho e espaçamento](https://github.com/LirielC/LeNote/issues/19)
- [ ] [#20 — Fontes locais e importação](https://github.com/LirielC/LeNote/issues/20)
- [ ] [#21 — Intensidade e pausa](https://github.com/LirielC/LeNote/issues/21)
- [ ] [#22 — Originais e Undo/Redo](https://github.com/LirielC/LeNote/issues/22)
- [ ] [#23 — Validação TB128FU](https://github.com/LirielC/LeNote/issues/23)

Cada Issue contém instruções técnicas e critérios de aceitação. Priorizar P0 de desempenho antes das transformações automáticas. Registrar progresso em IMPLEMENTATION_STATUS.md e resultados reais em BENCHMARKS.md. Não fechar tarefas sem testes necessários, incluindo validação física quando aplicável.
