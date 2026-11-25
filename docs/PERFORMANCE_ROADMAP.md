# LeNote — Lenovo Performance Roadmap

## 1. Objetivo

Aprimorar o motor de escrita do LeNote para oferecer uma experiência próxima à do Samsung Notes em fluidez, latência, naturalidade dos traços e estabilidade, priorizando o hardware Lenovo.

### Hardware de referência

- Tablet: Lenovo Xiaoxin Pad 2022 (TB128FU).
- Caneta: Lenovo Precision Pen 2.
- SoC: Qualcomm Snapdragon 680.
- GPU: Adreno 610.
- Tela: 2000 × 1200, 60 Hz.
- Orçamento nominal por frame: 16,67 ms.

A frequência efetiva de amostragem, os eixos disponíveis, o comportamento do firmware e o suporte à renderização de baixa latência devem ser medidos no aparelho. As especificações e os parâmetros abaixo são referências de projeto, não resultados de testes.

## 2. Princípios arquiteturais

- Preservar Kotlin, Jetpack Compose e Jetpack Ink, incluindo o formato de armazenamento existente.
- Manter a escrita ativa independente da renderização dos traços finalizados, do OCR, dos PDFs e do salvamento.
- Priorizar alterações incrementais e mensuráveis, evitando reescrever o motor gráfico sem evidência de necessidade.
- Preservar Undo/Redo, borracha parcial, laço, zoom, pan, reconhecimento de formas e compatibilidade com documentos existentes.
- Não depender de APIs proprietárias da Samsung nem presumir acesso privilegiado ao hardware Lenovo.
- Nunca persistir pontos previstos como amostras reais da caneta.

## 3. Checklist

### P0 — Crítico: latência e travamentos

- [ ] Instrumentar o desempenho real no TB128FU.
- [ ] Eliminar reconstrução completa do cache a cada traço.
- [ ] Remover serialização da thread principal.
- [ ] Integrar MotionEventPredictor.
- [ ] Otimizar onStrokesFinished.

### P1 — Alta prioridade: escalabilidade e caneta

- [ ] Implementar índice espacial.
- [ ] Implementar cache por tiles com limite de RAM.
- [ ] Calibrar pressão da Precision Pen 2.
- [ ] Aprimorar palm rejection.

### P2 — Refinamento e estabilidade

- [ ] Implementar suavização adaptativa.
- [ ] Otimizar PDFs, OCR e miniaturas.
- [ ] Realizar testes prolongados de memória.

## 4. Especificação técnica

### P0.1 — Instrumentar o desempenho real no TB128FU

**Tecnologias:** Perfetto, Android Studio Profiler, AndroidX JankStats, Macrobenchmark, FrameMetrics e `android.os.Trace`.

Expandir os diagnósticos existentes para medir:

- Frame time P50, P95 e P99, frames perdidos e jank.
- Tempo de processamento de `MotionEvent` e intervalo entre amostras, incluindo amostras históricas.
- Tempo de finalização, serialização, reconstrução e atualização parcial do cache.
- Alocações, pausas de GC, heap Java e memória nativa/gráfica.
- Pressão, inclinação, orientação, identificação da caneta e cancelamento de gestos.

Usar buffers de métricas com capacidade limitada, evitando logs ou recomposições a cada amostra. Medir builds release/profileable no tablet. Para latência ponta-a-pixel, utilizar câmera de alta velocidade; timestamps de eventos sozinhos não medem a apresentação física na tela.

**Aceitação:** estabelecer baseline reproduzível para escrita, borracha, zoom e autosave em páginas com 100, 1.000, 5.000 e 10.000 traços.

### P0.2 — Eliminar reconstrução completa do cache a cada traço

**Arquivos:** `FinishedStrokesView.kt`, `JetpackInkEngine.kt`.

**Tecnologias:** Android Canvas, Bitmap, RectF, CanvasStrokeRenderer, dirty rectangles.

O método `updateStrokes()` atualmente marca o cache estático como sujo. O fluxo `syncStateAndRender()` o chama ao finalizar traços, podendo provocar reconstrução completa.

- Introduzir operações incrementais como `appendStroke()`, `appendStrokes()` e `invalidateRegion()`.
- Acrescentar o novo traço ao cache existente quando sua transformação e versão forem válidas.
- Para remoções, reconstruir a região afetada respeitando ordem de desenho, transparência, marca-texto e interseções.
- Reservar reconstrução global para alterações que realmente a exijam, como troca de página ou cache inválido.
- Separar atualização de estado da atualização de renderização.

**Aceitação:** zero reconstruções globais desnecessárias por traço em escrita normal; equivalência visual com renderização de referência, incluindo Undo/Redo e marca-texto.

### P0.3 — Remover serialização da thread principal

**Arquivos:** `InkPlaygroundViewModel.kt`, `JetpackInkEngine.kt`, `PageStrokeStorage.kt`.

**Tecnologias:** Kotlin Coroutines, Dispatchers.Default, Dispatchers.IO, StateFlow, Mutex ou fila de escrita com consumidor único.

Atualmente, `engine.saveStrokes()` ocorre antes do bloco `withContext(Dispatchers.IO)` em `persistCurrentPageAsync()` e percorre os traços para codificá-los.

- Capturar snapshot imutável e metadados mínimos na thread proprietária do motor.
- Serializar o snapshot em `Dispatchers.Default`, verificando segurança de acesso aos objetos Jetpack Ink e seus recursos nativos.
- Persistir arquivos e banco em `Dispatchers.IO`.
- Controlar versão e ID da página para impedir sobrescritas por salvamentos antigos.
- Consolidar pedidos de salvamento; testar debounce inicial de 1.000 ms sem comprometer durabilidade.
- Revisar `flushCurrentPageSave()` para evitar bloqueios e perda de dados em troca de página e encerramento.
- Separar miniaturas do caminho crítico de persistência.

**Aceitação:** nenhuma serialização pesada ou operação de disco na main thread; dados íntegros após troca rápida de página, cancelamento de tarefas e reabertura do app.

### P0.4 — Integrar MotionEventPredictor

**Arquivo:** `JetpackInkEngine.kt`.

**Tecnologias:** `androidx.input:input-motionprediction`, `MotionEventPredictor`, Jetpack Ink `InProgressStrokesView`.

- Registrar a sequência de eventos reais de entrada de maneira consistente, inclusive início, movimento, fim e cancelamento.
- Obter um evento previsto por atualização e encaminhá-lo ao parâmetro de predição do `addToStroke()` compatível com a versão instalada.
- Respeitar coordenadas, IDs de ponteiro, ciclo de vida, propriedade dos eventos e regras de descarte da API.
- Manter fallback quando não houver previsão válida.
- Evitar dupla predição e extrapolação manual adicional.
- Adicionar opção de diagnóstico para comparação A/B com predição desligada.

**Aceitação:** redução mensurável da distância ponta-traço sem ganchos, overshoot perceptível, pontos fantasmas ou alterações no conteúdo persistido.

### P0.5 — Otimizar onStrokesFinished

**Arquivos:** `JetpackInkEngine.kt`, `StrokeSmoother.kt`, `FinishedStrokesView.kt`.

**Tecnologias:** `InProgressStrokesFinishedListener`, renderização incremental, Kotlin Coroutines, tracing.

- Transferir o traço ativo para a camada permanente conforme o contrato de handoff do Jetpack Ink.
- Evitar desaparecimento temporário, duplicação e mudança visual brusca do traço.
- Medir separadamente suavização, reconhecimento de formas, preparação da borracha, histórico e renderização.
- Adiar cálculos não essenciais quando a API e a segurança de concorrência permitirem.
- Aplicar resultados assíncronos somente se o traço e a revisão do documento ainda forem válidos.
- Preservar a ordem dos traços e a semântica de Undo/Redo.

**Aceitação:** finalização sem lacunas visuais ou microtravamentos perceptíveis entre letras; meta inicial de trabalho síncrono P95 inferior a 4 ms, sujeita a medição.

### P1.1 — Implementar índice espacial

**Tecnologias:** Spatial Hash Grid em Kotlin; avaliar R-tree ou Quadtree se benchmarks justificarem.

Criar `StrokeSpatialIndex.kt` para armazenar IDs e bounding boxes em coordenadas do documento.

- Atualizar o índice incrementalmente ao inserir, remover, mover ou redimensionar traços.
- Consultar candidatos para borracha, laço, regiões sujas e viewport.
- Realizar testes geométricos exatos somente nos candidatos retornados.
- Preservar ordem de desenho e incluir a extensão efetiva do pincel nos limites.
- Comparar resultados com busca linear como referência de correção.

**Aceitação:** resultados equivalentes à busca linear, com redução de candidatos e tempo de consulta em páginas densas.

### P1.2 — Implementar cache por tiles com limite de RAM

**Tecnologias:** Bitmap/Canvas, `android.util.LruCache`, dirty regions, índice espacial; avaliar RenderNode apenas após profiling.

Criar `TileRenderCache.kt`, inicialmente com:

| Parâmetro | Valor inicial experimental |
|---|---|
| Tile | 256 × 256 pixels |
| Formato | ARGB_8888 |
| Orçamento de cache | 32 MiB |
| Limite experimental | 64 MiB, condicionado à memória disponível |

- Separar fundo/PDF, tinta permanente e overlays quando isso reduzir reconstruções.
- Carregar prioritariamente tiles visíveis; limitar prefetch.
- Usar chave com página, posição, escala e revisão relevante.
- Invalidar apenas tiles intersectados pela alteração, incluindo margens de antialiasing e espessura.
- Durante pan/zoom, reutilizar cache válido quando possível e refinar a resolução depois.
- Controlar memória real com `allocationByteCount`, descarte LRU e callbacks de pressão de memória.
- Evitar costuras, piscadas e alocações repetitivas; não reciclar bitmaps ainda em uso.

**Aceitação:** cache dentro do orçamento, sem crescimento ilimitado, costuras ou regressão de fluidez. Só substituir o cache incremental simples se o ganho for demonstrado.

### P1.3 — Calibrar pressão da Precision Pen 2

**Tecnologias:** MotionEvent, InputDevice.MotionRange, Jetpack Ink Brush/BrushFamily.

Criar `PressureCurve.kt` e, se necessário, `StylusDeviceProfile.kt`.

- Medir pressão mínima útil, faixa real e comportamento perto do contato.
- Detectar suporte a pressão, inclinação e orientação em runtime.
- Testar curva normalizada `p' = p^gamma`, com gamma inicialmente configurável entre 0,7 e 1,3.
- Usar parâmetros de pincel compatíveis com a versão do Jetpack Ink; não distorcer a geometria para simular pressão.
- Oferecer prévia e opção de restaurar o padrão.

**Aceitação:** escrita confortável com pouca força, variação contínua de espessura e consistência entre traço ativo e finalizado.

### P1.4 — Aprimorar palm rejection

**Tecnologias:** MotionEvent, `TOOL_TYPE_STYLUS`, `TOOL_TYPE_ERASER`, `TOOL_TYPE_FINGER`, `ACTION_CANCEL`, `FLAG_CANCELED` quando disponível.

- Priorizar caneta e respeitar cancelamentos emitidos pelo Android.
- Impedir que contatos involuntários dos dedos iniciem tinta ou transformações durante escrita ativa.
- Preservar gestos intencionais de navegação e zoom quando a caneta não está escrevendo.
- Oferecer modo somente caneta para escrita, sem bloquear desnecessariamente a navegação por toque.
- Tratar hover, múltiplos ponteiros e botões da caneta conforme eventos efetivamente fornecidos pelo firmware.

**Aceitação:** nenhum traço fantasma ou deslocamento involuntário em testes de escrita com a palma apoiada; gestos cancelados não permanecem no documento.

### P2.1 — Implementar suavização adaptativa

**Tecnologias:** modelagem nativa do Jetpack Ink, One Euro Filter experimental, filtros de pressão e suavização geométrica.

**Arquivo:** `StrokeSmoother.kt`; criar `AdaptiveStrokeFilter.kt` somente se necessário.

- Avaliar primeiro a suavização nativa do Jetpack Ink para evitar filtragem redundante.
- O filtro fixo existente no motor atua em pontos geométricos auxiliares; não presumir que ele altera diretamente a tinta ativa.
- Testar estabilização dependente da velocidade com timestamps reais.
- Preservar cantos, letras pequenas, pontos, acentos, pressão e inclinação.
- Evitar atraso excessivo e alterações perceptíveis após levantar a caneta.
- Manter configuração reversível e comparar com amostras sem filtro adicional.

**Aceitação:** menor tremor sem perda de detalhes ou aumento perceptível da latência.

### P2.2 — Otimizar PDFs, OCR e miniaturas

**Arquivos:** `PdfPageRenderer.kt`, `DigitalInkManager.kt`, `PageExporter.kt`, `InkPlaygroundViewModel.kt`.

**Tecnologias:** Android PdfRenderer, ML Kit Digital Ink Recognition, Kotlin Coroutines, LruCache; WorkManager para trabalho persistente que realmente precise sobreviver ao processo.

- Renderizar PDFs sob demanda com resolução adaptada ao zoom e cache limitado.
- Cancelar solicitações obsoletas ao mudar de página; respeitar o contrato de concorrência do PdfRenderer.
- Adiar OCR e miniaturas durante escrita ativa, com debounce e controle de revisão da página.
- Limitar concorrência para reduzir competição por CPU e RAM.
- Não confundir adiar miniaturas/OCR com adiar indefinidamente a persistência dos dados do usuário.

**Aceitação:** escrever sobre PDFs grandes sem regressão significativa no frame time; resultados assíncronos nunca aplicados à página errada.

### P2.3 — Realizar testes prolongados de memória

**Tecnologias:** Android Studio Memory Profiler, Perfetto/heapprofd, LeakCanary em builds de diagnóstico, `adb shell dumpsys meminfo`, testes instrumentados AndroidX.

- Testar sessões de 30 e 60 minutos no tablet físico.
- Alternar escrita, borracha, zoom, páginas, PDFs, Undo/Redo e segundo plano.
- Monitorar heap Java, memória nativa, bitmaps, GPU quando disponível, GC e temperatura.
- Verificar descarte de Views, listeners, jobs, caches e recursos PDF.
- Testar recuperação após pressão de memória e encerramento do processo.

**Aceitação:** ausência de OOM/ANR e vazamentos reproduzíveis, memória limitada após aquecimento e recuperação adequada de recursos descartáveis.

## 5. Stack recomendada

| Tecnologia | Decisão | Finalidade |
|---|---|---|
| Kotlin | Manter | Motor e lógica do aplicativo |
| Jetpack Compose | Manter | Interface, toolbar e navegação |
| Android Views / AndroidView | Manter | Integração da superfície de escrita |
| Jetpack Ink 1.0.0 (baseline observado) | Manter inicialmente | Traço ativo, pincéis, geometria e renderização |
| AndroidX Motion Prediction | Adicionar | Predição temporária da caneta |
| CanvasStrokeRenderer | Manter | Renderização dos traços finalizados |
| Bitmap + Canvas + LruCache | Evoluir | Cache incremental e tiles |
| Spatial Hash Grid | Implementar | Consultas espaciais eficientes |
| Kotlin Coroutines | Otimizar | Processamento assíncrono e cancelamento |
| Room | Manter | Metadados e banco de dados |
| Android PdfRenderer | Otimizar | Renderização de PDFs |
| ML Kit Digital Ink | Manter | Reconhecimento de escrita |
| One Euro Filter | Avaliar | Estabilização complementar |
| Perfetto + JankStats + Macrobenchmark | Adicionar/ampliar | Medição de desempenho |
| LeakCanary | Avaliar em debug | Detecção de vazamentos |
| OpenGL ES / Vulkan / C++ próprio | Não priorizar | Somente se profiling demonstrar necessidade |

Selecionar versões estáveis compatíveis com o projeto e confirmar as assinaturas das APIs antes de implementar. Não adicionar bibliotecas redundantes.

## 6. Organização sugerida

```text
ink/
  JetpackInkEngine.kt          # existente
  FinishedStrokesView.kt       # existente
  StrokeSmoother.kt            # existente
  MotionPredictionManager.kt   # novo, se útil
  StrokeSpatialIndex.kt        # novo
  TileRenderCache.kt           # novo
  PressureCurve.kt              # novo
  StylusDeviceProfile.kt        # opcional
  PalmRejectionManager.kt       # opcional
  AdaptiveStrokeFilter.kt       # experimental
  InkPerformanceMonitor.kt     # novo

data/
  PageStrokeStorage.kt          # existente
  StrokeSnapshotSerializer.kt   # novo

ui/
  InkCanvas.kt                  # existente
  InkPlaygroundViewModel.kt     # existente
```

A separação em arquivos é uma proposta, não uma obrigação. Evitar abstrações que não tragam benefício mensurável.

## 7. Metas de desempenho

| Indicador | Meta inicial |
|---|---|
| Atualização em escrita | 60 FPS estáveis quando aplicável |
| Frame time P95 | Até 16,67 ms no cenário de referência |
| Processamento síncrono do evento | P95 inferior a 1 ms |
| Finalização síncrona | P95 inferior a 4 ms |
| Reconstruções globais desnecessárias por traço | 0 |
| Operações de disco na main thread | 0 |
| Teste de escalabilidade | 10.000 traços por página |
| Escrita prolongada | 60 minutos sem OOM/ANR |
| Cache de tiles | Inicialmente 32 MiB; ajustar após medição |
| Latência ponta-a-pixel | Melhorar em relação ao baseline medido |

As metas são hipóteses de engenharia. O período de atualização da tela não equivale à latência total ponta-a-pixel. Não prometer latência de 2,8 ms ou 120 Hz nesse hardware.

## 8. Estratégia de implementação e validação

1. Registrar baseline e testes de regressão antes de alterar o motor.
2. Implementar P0 em mudanças pequenas e independentes, medindo cada uma.
3. Validar a transferência de traços, persistência e predição no tablet físico.
4. Implementar P1 conforme os gargalos remanescentes demonstrados pelas medições.
5. Executar P2 e repetir os benchmarks e testes prolongados.
6. Documentar métricas antes/depois, limitações e eventuais regressões por alteração.

Nenhuma tarefa deve ser marcada como concluída somente porque o código foi escrito. Exigir testes de correção e evidência de desempenho para as alterações relevantes. Manter compatibilidade com notas existentes e evitar mudanças destrutivas no formato de armazenamento.

## 9. Referências técnicas

- [Repositório LeNote](https://github.com/LirielC/LeNote)
- [Android — Ink API](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/ink-api-modules)
- [Android — Advanced stylus features](https://developer.android.com/develop/ui/views/touch-and-input/stylus-input/advanced-stylus-features)
- [Android — Motion Prediction](https://developer.android.com/jetpack/androidx/releases/input)
- [Android — JankStats](https://developer.android.com/topic/performance/jankstats)
- [Android — Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [Perfetto](https://perfetto.dev/docs/)
- [One Euro Filter](https://gery.casiez.net/1euro/)
- [LeakCanary](https://square.github.io/leakcanary/)

## 10. Limitações

Este roadmap parte da análise estática anterior do repositório e das tarefas solicitadas. Não representa alterações já implementadas nem medições feitas no TB128FU. A disponibilidade das APIs, o comportamento da ROM, a pressão, a taxa de amostragem e os ganhos reais devem ser confirmados no dispositivo. A arquitetura proprietária do Samsung Notes não é integralmente pública; o objetivo é equivalência de experiência, não reprodução exata de sua implementação.

## 11. GitHub Issues

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


## Roadmaps complementares
- [Smart Handwriting](SMART_HANDWRITING_ROADMAP.md) — Issues #14–#23.
- [Smart Math](SMART_MATH_ROADMAP.md) — Issues #24–#33.
Priorizar P0 antes de habilitar transformação automática. Reconhecimento e renderização inteligente nunca devem bloquear a tinta ativa.
