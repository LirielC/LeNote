# LeNote — Instruções para agentes de programação

## Objetivo
Aproximar a fluidez de escrita do Samsung Notes no Lenovo Xiaoxin Pad 2022 (TB128FU) com Precision Pen 2, preservando as funcionalidades e notas existentes.

## Contexto obrigatório
Antes de editar código, leia:
- [Roadmap](docs/PERFORMANCE_ROADMAP.md)
- [Status](docs/IMPLEMENTATION_STATUS.md)
- [Benchmarks](docs/BENCHMARKS.md)
- [Arquitetura](docs/ARCHITECTURE.md)

## Fluxo
1. Inspecione git status e mudanças existentes; nunca descarte trabalho do usuário.
2. Consulte o status e, se disponível, as Issues com gh issue list --state open e gh issue view N. Caso não haja acesso ao GitHub, use os documentos locais.
3. Selecione a próxima tarefa elegível na ordem P0, P1, P2; respeite dependências e registre bloqueios. Não confunda implementação com validação física.
4. Inspecione a implementação real e versões das APIs antes de editar. Faça alterações pequenas e focadas.
5. Execute os testes aplicáveis (por exemplo ./gradlew :app:testDebugUnitTest, :app:assembleDebug, e :app:connectedDebugAndroidTest quando houver dispositivo). No Windows use gradlew.bat. Registre resultados reais e limitações.
6. Atualize IMPLEMENTATION_STATUS.md com arquivos alterados, testes, evidências, pendências e próxima tarefa; atualize BENCHMARKS.md com medições efetivas. Marque o checklist apenas quando os critérios forem cumpridos.
7. Se autorizado a trabalhar continuamente, avance para a próxima tarefa elegível. Se uma validação física impedir conclusão, registre aguardando validação e avance apenas em tarefas independentes.
8. Atualize Issues quando houver autorização e acesso; mantenha consistência entre Issues e documentos.

## Restrições
- Manter Kotlin, Compose, Jetpack Ink e armazenamento compatível. Não reescrever todo o app.
- Preservar Undo/Redo, borracha, laço, zoom, PDFs, OCR, formas, navegação e notas existentes.
- Não persistir eventos previstos. Não presumir segurança de acesso concorrente às APIs nativas.
- Não bloquear a main thread com I/O ou serialização pesada. Não perder dados ao adiar tarefas.
- Não adicionar dependências sem justificativa; confirmar assinaturas e versões.
- Não inventar resultados de benchmarks ou afirmar validação física sem dispositivo.
- Não fazer push para main, merge, release ou publicar APK sem autorização específica. Não alterar arquivos alheios à tarefa.
- Não incluir segredos, dados de notas pessoais ou informações sensíveis em logs ou Issues.

## Conclusão
Só fechar tarefa quando critérios de aceitação e testes necessários tiverem evidência. Caso contrário, manter aberta e registrar o que falta.


## Roadmaps de escrita inteligente
Leia também [Smart Handwriting](docs/SMART_HANDWRITING_ROADMAP.md) e [Smart Math](docs/SMART_MATH_ROADMAP.md). Issues #14–#33 complementam #1–#12. Priorizar P0 e dependências antes das tarefas elegíveis P1/P2. Modos automáticos por toggle, sem laço; tinta imediata, reconhecimento assíncrono, revisão validada, originais recuperáveis após reabertura e Undo/Redo atômico. Desligar cancela trabalhos pendentes. Ambiguidades preservam tinta. Matemática apenas formata, não resolve nem corrige. Não adotar SDK pago nem transmitir notas sem autorização.
