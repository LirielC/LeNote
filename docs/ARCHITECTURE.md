# LeNote — Arquitetura de escrita e desempenho

## Baseline observado por análise estática
Kotlin + Compose; AndroidView hospeda InProgressStrokesView e FinishedStrokesView. JetpackInkEngine coordena entrada, histórico, borracha e finalização. FinishedStrokesView mantém cache bitmap estático. InkPlaygroundViewModel coordena persistência e OCR. Conferir o código atual antes de assumir que estas observações ainda valem.

## Arquitetura proposta (não implementada)
MotionEvent -> MotionEventPredictor opcional -> InProgressStrokesView -> handoff sincronizado -> coleção canônica de Stroke -> cache incremental/tiles + índice espacial. Snapshots imutáveis alimentam serialização em Default e persistência em IO. OCR, PDFs e miniaturas são isolados do caminho crítico.

## Contratos
- Pontos previstos apenas para visualização; persistir amostras reais.
- Manter ordem de desenho, blending de marca-texto e consistência entre traço ativo e permanente.
- O motor possui o estado mutável; workers recebem snapshots seguros. Verificar regras de thread dos recursos Jetpack Ink.
- Resultados assíncronos exigem ID e revisão de página válidos; rejeitar resultados obsoletos.
- Cache não é fonte canônica. Invalidar regiões com margens adequadas; reconstrução total somente quando necessária.
- Índice espacial é auxiliar; validar contra busca linear e conservar ordem de desenho.
- Tiles e PDFs têm limite de memória e descarte seguro; não reciclar bitmaps em uso.
- Preservar formato existente, durabilidade dos dados, Undo/Redo e compatibilidade de documentos.
- Usar fallback sem predição e sem recursos de hardware opcionais.

## Stack
Manter Kotlin, Compose, Android Views, Jetpack Ink, Room e ML Kit. Avaliar MotionEventPredictor, CanvasStrokeRenderer, Spatial Hash Grid, LruCache, Coroutines, Perfetto e JankStats. One Euro Filter apenas após medição. Não priorizar C++/Vulkan próprios.

## Parâmetros experimentais
Tiles 256x256 ARGB_8888; cache inicial 32 MiB, até 64 MiB somente após medição; debounce de autosave 1000 ms sujeito a testes de durabilidade. Confirmar compatibilidade e comportamento no TB128FU.

## Referências
Consultar PERFORMANCE_ROADMAP.md para critérios detalhados e BENCHMARKS.md para evidência.


## Extensão proposta: escrita inteligente
Dois toggles independentes: caligrafia (natural, fonte manuscrita ou digitada) e matemática. Nenhum depende de laço. Jetpack Ink exibe tinta imediatamente; agrupador e detector de pausa alimentam reconhecimento fora do caminho crítico. ML Kit para texto; MathRecognitionEngine especializado para estrutura 2D. Validar revisão, página e qualidade antes da transformação atômica; preservar originais e Undo/Redo. Em escrita mista, matemática tem precedência apenas com classificação segura. Cachear fórmulas e preservar conteúdo sem resolver. Consultar SMART_HANDWRITING_ROADMAP.md e SMART_MATH_ROADMAP.md.
