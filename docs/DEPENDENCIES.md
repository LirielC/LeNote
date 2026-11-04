# Documentação de Dependências e Licenças — LeNote (`docs/DEPENDENCIES.md`)

Este documento detalha todas as bibliotecas diretas e transitivas utilizadas pelo **LeNote**, suas versões exatas definidas em [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) e [`app/build.gradle.kts`](../app/build.gradle.kts), suas funções na arquitetura e suas respectivas licenças e implicações de distribuição.

---

## Sumário
1. [Resumo de Conformidade e Licenciamento](#1-resumo-de-conformidade-e-licenciamento)
2. [Tabela de Dependências Diretas de Produção](#2-tabela-de-dependências-diretas-de-produção)
3. [Dependências Transitivas Importantes (ML Kit / GMS)](#3-dependências-transitivas-importantes-ml-kit--gms)
4. [Dependências de Testes (Unitários e Instrumentados)](#4-dependências-de-testes-unitários-e-instrumentados)
5. [Plugins de Compilação (Build System)](#5-plugins-de-compilação-build-system)

---

## 1. Resumo de Conformidade e Licenciamento

O código-fonte autoral do **LeNote** é distribuído sob a licença **MIT** ([`LICENSE`](../LICENSE)).

No entanto, o binário compilado (`.apk`) inclui diretamente a biblioteca **Google ML Kit Digital Ink Recognition** (`com.google.mlkit:digital-ink-recognition:19.0.0`) para reconhecimento de escrita à mão e embelezamento caligráfico:

> [!IMPORTANT]
> **Aviso sobre Distribuição FOSS (F-Droid / IzzyOnDroid):**
> Embora quase toda a pilha de interface e desenho (Jetpack Compose, Jetpack Ink, AndroidX e Kotlin) seja código aberto sob a licença **Apache License 2.0**, o pacote **`com.google.mlkit:digital-ink-recognition`** e suas dependências transitivas (`com.google.android.gms` e componentes internos `com.google.firebase` / `datatransport`) são bibliotecas **proprietárias** regidas pelos *Termos de Serviço das APIs do Google / ML Kit*. Por esse motivo, o APK padrão que inclui o ML Kit contém código binário proprietário (`libdigitalink.so`) e permissões de rede para download de modelos de idioma, não sendo elegível para repositórios estritamente 100% FOSS sem substituição ou remoção dessa dependência.

---

## 2. Tabela de Dependências Diretas de Produção

Todas as versões abaixo refletem exatamente o catálogo [`gradle/libs.versions.toml`](../gradle/libs.versions.toml) e o script [`app/build.gradle.kts`](../app/build.gradle.kts):

| Dependência | Artefato Maven (`group:name`) | Versão | Função no LeNote | Licença | Código Aberto (FOSS)? |
|---|---|---|---|---|---|
| **Jetpack Ink — Authoring** | `androidx.ink:ink-authoring` | `1.0.0` | Renderização de traços em progresso de baixíssima latência (`InProgressStrokesView`). | Apache-2.0 | Sim |
| **Jetpack Ink — Brush** | `androidx.ink:ink-brush` | `1.0.0` | Definição de pincéis sensíveis à pressão (`StockBrushes.pressurePen`) e marca-texto (`highlighter`). | Apache-2.0 | Sim |
| **Jetpack Ink — Geometry** | `androidx.ink:ink-geometry` | `1.0.0` | Primitivas matemáticas 2D (`Box`, `ImmutableVec`, `AffineTransform`, interseções geométricas). | Apache-2.0 | Sim |
| **Jetpack Ink — Rendering** | `androidx.ink:ink-rendering` | `1.0.0` | Renderizador vetorial em Canvas Android (`CanvasStrokeRenderer`) para tela, PDF e PNG. | Apache-2.0 | Sim |
| **Jetpack Ink — Strokes** | `androidx.ink:ink-strokes` | `1.0.0` | Estrutura de dados de traços finalizados (`Stroke`, `StrokeInput`, `ImmutableStrokeInputBatch`). | Apache-2.0 | Sim |
| **Jetpack Ink — Storage** | `androidx.ink:ink-storage` | `1.0.0` | Codificação e decodificação binária compacta de lotes de pontos (`StrokeInputBatchSerialization`). | Apache-2.0 | Sim |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | `2026.03.01` | Bill of Materials que alinha versões dos módulos do Jetpack Compose. | Apache-2.0 | Sim |
| **Compose UI & Graphics** | `androidx.compose.ui:ui`<br>`androidx.compose.ui:ui-graphics`<br>`androidx.compose.ui:ui-tooling-preview` | Gerenciado pelo BOM `2026.03.01` | Sistema de UI declarativa, canvas customizado, ícones vetoriais Phosphor e Design System `LeNoteTheme`. | Apache-2.0 | Sim |
| **Compose Material 3** | `androidx.compose.material3:material3` | Gerenciado pelo BOM `2026.03.01` | Componentes de base de interface, diálogos, sliders e superfícies. | Apache-2.0 | Sim |
| **AndroidX Activity Compose** | `androidx.activity:activity-compose` | `1.13.0` | Integração entre `ComponentActivity`, seletores de arquivos (`ActivityResultContracts`) e Compose. | Apache-2.0 | Sim |
| **AndroidX Activity KTX** | `androidx.activity:activity-ktx` | `1.13.0` | Extensões Kotlin para ciclo de vida de Activity e tratamento do botão Voltar (`OnBackPressedDispatcher`). | Apache-2.0 | Sim |
| **AndroidX Core KTX** | `androidx.core:core-ktx` | `1.15.0` | Utilitários centrais do Android, `FileProvider` para compartilhamento seguro de arquivos exportados. | Apache-2.0 | Sim |
| **AndroidX AppCompat** | `androidx.appcompat:appcompat` | `1.7.1` | Compatibilidade de recursos e temas base do sistema Android. | Apache-2.0 | Sim |
| **Material Components** | `com.google.android.material:material` | `1.14.0` | Temas XML base (`Theme.Material3.DayNight.NoActionBar`). | Apache-2.0 | Sim |
| **Google ML Kit Digital Ink** | `com.google.mlkit:digital-ink-recognition` | `19.0.0` | Reconhecimento óptico de escrita manuscrita (OCR de traços para texto) usado na busca FTS5 e na conversão de caligrafia para fonte. | **Proprietária** (Google APIs / ML Kit Terms of Service) | **Não** (Proprietária) |

---

## 3. Dependências Transitivas Importantes (ML Kit / GMS)

Ao incluir `com.google.mlkit:digital-ink-recognition:19.0.0`, o Gradle resolve automaticamente as seguintes dependências transitivas que passam a integrar o APK compilado e o `AndroidManifest.xml` mesclado:

| Pacote Transitivo | Origem | Função Transitiva | Licença | Impacto no Manifesto / APK |
|---|---|---|---|---|
| `com.google.mlkit:vision-common` / `common` | `mlkit:digital-ink-recognition` | Gerenciamento de download e armazenamento local dos modelos de idioma (`RemoteModelManager`) e biblioteca nativa JNI `libdigitalink.so`. | **Proprietária** (Google) | Registra `MlKitComponentDiscoveryService` e `MlKitInitProvider`. Adiciona permissões `INTERNET` e `ACCESS_NETWORK_STATE`. |
| `com.google.android.gms:play-services-base` / `tasks` | `mlkit:digital-ink-recognition` | API assíncrona `Task<T>` e infraestrutura base do Google Play Services. | **Proprietária** (Google) | Registra `GoogleApiActivity` e metadado `com.google.android.gms.version`. |
| `com.google.firebase:firebase-components` | `mlkit:digital-ink-recognition` | Injeção de dependência interna usada pelos registradores do ML Kit (`ComponentRegistrar`). | Apache-2.0 (código do container) | Usado internamente pelo `MlKitComponentDiscoveryService`. |
| `com.google.android.datatransport:*` | `mlkit:digital-ink-recognition` | Pipeline de transporte de diagnósticos/telemetria embutido no SDK do ML Kit. | Apache-2.0 | Registra `TransportBackendDiscovery`, `JobInfoSchedulerService` e `AlarmManagerSchedulerBroadcastReceiver`. |
| `androidx.work:work-runtime` | `mlkit:digital-ink-recognition` | Agendamento em segundo plano para download de modelos com restrições de rede. | Apache-2.0 | Adiciona `SystemJobService`, `SystemAlarmService` e permissões `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` e `FOREGROUND_SERVICE`. |
| `libandroidx.graphics.path.so`, `libgraphics-core.so`, `libink.so` | `androidx.ink:*` | Motores nativos C++ de malha poligonal, geometria e renderização de baixa latência do Jetpack Ink. | Apache-2.0 | Bibliotecas nativas empacotadas para `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`. |

---

## 4. Dependências de Testes (Unitários e Instrumentados)

Essas bibliotecas são compiladas exclusivamente nos source sets `test` e `androidTest` e **não** são incluídas no APK de produção:

| Dependência | Artefato Maven | Escopo | Versão | Função | Licença |
|---|---|---|---|---|---|
| **JUnit 4** | `junit:junit` | `testImplementation` | `4.13.2` | Execução da suíte de testes unitários JVM (storage, borracha parcial, suavização, laço e refinamento). | EPL-1.0 |
| **org.json** | `org.json:json` | `testImplementation` | `20240303` | Implementação real de `JSONObject` / `JSONArray` para testes de serialização na JVM local (no Android, faz parte do SDK do sistema). | JSON License (MIT-like) |
| **AndroidX Test Ext JUnit** | `androidx.test.ext:junit` | `androidTestImplementation` | `1.3.0` | Runner e asserções JUnit para testes instrumentados em dispositivos/emuladores Android. | Apache-2.0 |
| **Espresso Core** | `androidx.test.espresso:espresso-core` | `androidTestImplementation` | `3.7.0` | Automação e sincronização de UI em testes instrumentados. | Apache-2.0 |
| **Compose UI Tooling** | `androidx.compose.ui:ui-tooling` | `debugImplementation` | BOM `2026.03.01` | Inspeção de layout e renderização de `@Preview` apenas em builds `debug`. | Apache-2.0 |

---

## 5. Plugins de Compilação (Build System)

| Plugin | ID do Plugin | Versão | Licença |
|---|---|---|---|
| **Android Application Gradle Plugin (AGP)** | `com.android.application` | `9.2.1` | Apache-2.0 |
| **Kotlin Android / Compose Compiler** | `org.jetbrains.kotlin.plugin.compose` | `2.1.20` | Apache-2.0 |
| **Gradle Wrapper** | Distribuição `gradle-9.4.1-bin.zip` | `9.4.1` | Apache-2.0 |
