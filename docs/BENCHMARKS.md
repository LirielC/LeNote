# LeNote — Benchmarks

Nenhuma medição registrada. Não preencher com valores hipotéticos.

## Ambiente de referência
TB128FU + Lenovo Precision Pen 2; confirmar firmware, Android, RAM, refresh rate, build, commit e temperatura. Referência nominal de 60 Hz, 16,67 ms por frame; confirmar no aparelho.

## Protocolo
Usar build release/profileable; repetir cenários idênticos antes/depois com páginas de 100, 1.000, 5.000 e 10.000 traços. Registrar escrita rápida/lenta, borracha, zoom/pan, PDF, autosave e sessões de 30/60 min. Comparar predição ligada/desligada. Medir ponta-a-pixel por câmera de alta velocidade, não apenas timestamps de input.
Ferramentas: Perfetto, JankStats, Macrobenchmark, FrameMetrics, Android Studio Profiler, adb shell dumpsys meminfo. Não coletar conteúdo privado das notas.

| Métrica | Baseline | Depois | Meta experimental |
|---|---|---|---|
| Frame time P95 | A medir | A medir | <= 16,67 ms |
| Input síncrono P95 | A medir | A medir | < 1 ms |
| Commit síncrono P95 | A medir | A medir | < 4 ms |
| Reconstruções globais desnecessárias por traço | A medir | A medir | 0 |
| I/O na main thread | A medir | A medir | 0 |
| Latência ponta-a-pixel | A medir | A medir | Reduzir vs baseline |
| Jank e frames perdidos | A medir | A medir | Reduzir vs baseline |
| Heap/nativa/gráfica e GC | A medir | A medir | Estáveis após aquecimento |
| OOM/ANR em 60 min | A medir | A medir | 0 |

Metas não são resultados medidos nem garantias.

## Registro de execução
Data, commit, aparelho/ROM, build, cenário, número de repetições, métricas P50/P95/P99, artefatos de trace, observações e limitações.


## Escrita inteligente — métricas adicionais (a medir)
Comparar toggles desligados/ligados no TB128FU. Corpus pt-BR com acentos, cursiva, letra de forma e escrita mista; medir CER/WER, tempo pausa→conversão P50/P95, taxa de fallback, alterações indevidas, jank e memória. Matemática: precisão de estrutura e símbolos contra AST/LaTeX de referência, sem resolver expressões. Testar reversão, troca de página, cancelamento e reabertura. Nenhum resultado registrado.
