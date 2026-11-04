# Política de Privacidade e Verificação Técnica — LeNote (`PRIVACY.md`)

**Última atualização:** Novembro de 2026  
**Aplicativo:** LeNote (`io.github.lirielc.lenote`)  
**Repositório Oficial:** [https://github.com/LirielC/LeNote](https://github.com/LirielC/LeNote)

Esta Política de Privacidade explica de forma transparente e tecnicamente verificável como o **LeNote** armazena seus dados, quais permissões do sistema Android estão presentes no APK compilado, por que elas existem e qual é o comportamento exato de rede do aplicativo.

---

## Sumário
1. [Resumo Executivo](#1-resumo-executivo)
2. [Quais Dados Permanecem no Dispositivo](#2-quais-dados-permanecem-no-dispositivo)
3. [Transmissão de Informações e Serviços Externos](#3-transmissão-de-informações-e-serviços-externos)
4. [Reconhecimento de Escrita (Google ML Kit) e Download de Modelos](#4-reconhecimento-de-escrita-google-ml-kit-e-download-de-modelos)
5. [Como Funcionam os Backups e Exportações](#5-como-funcionam-os-backups-e-exportações)
6. [Auditoria de Permissões do Manifesto Compilado](#6-auditoria-de-permissões-do-manifesto-compilado)
7. [Como Verificar Você Mesmo (Auditoria Independente)](#7-como-verificar-você-mesmo-auditoria-independente)

---

## 1. Resumo Executivo

- **Zero Servidores Próprios ou Contas**: O LeNote não possui servidores de backend, não exige criação de conta, login, e-mail ou número de telefone.
- **Armazenamento Local-First**: Todos os seus cadernos, seções, páginas, traços manuscritos (`.ink`), caixas de texto, imagens e PDFs importados são armazenados exclusivamente na memória interna do seu dispositivo.
- **Uso de Rede Restrito ao Google ML Kit**: O código autoral do LeNote não contém clientes HTTP nem envia suas notas para a internet. Contudo, o aplicativo inclui a biblioteca **Google ML Kit Digital Ink Recognition** (`com.google.mlkit:digital-ink-recognition:19.0.0`) para reconhecimento de escrita à mão no dispositivo (*on-device*). Essa biblioteca utiliza conexão com a internet para **baixar o pacote de modelo de idioma** (ex.: `pt-BR` ou `en-US`, ~20 MB) dos servidores do Google no primeiro uso e inclui os serviços internos padrão do SDK do ML Kit/GMS.

---

## 2. Quais Dados Permanecem no Dispositivo

Todos os dados criados por você permanecem sob controle local no diretório privado do aplicativo (`/data/data/io.github.lirielc.lenote/`):

| Tipo de Dado | Localização no Dispositivo | Descrição |
|---|---|---|
| **Metadados de Cadernos, Seções e Páginas** | `databases/lenote.db` (SQLite) | Títulos, cores, datas de criação/edição, pautas escolhidas, posição de zoom/pan e índice local de busca FTS5. |
| **Traços Vetoriais Manuscritos** | `files/pages/<pageId>.ink` | Pontos de coordenadas `(x, y)`, pressão, inclinação, tempo relativo e cor de cada traço desenhado. |
| **Miniaturas de Páginas** | `files/thumbs/<pageId>.png` | Imagens `240 × 320 px` geradas localmente para exibição rápida na barra lateral. |
| **Imagens e PDFs Importados** | `files/images/` e `files/pdfs/` | Cópias locais dos arquivos selecionados por você via seletor de arquivos do sistema Android. |
| **Preferências de Interface** | `shared_prefs/lenote_prefs.xml` | Posição da barra de ferramentas, modo escuro/claro, idioma selecionado (`PT/EN/ES`) e opção de escrita com o dedo. |

### Backup Automático do Sistema Android (`allowBackup="true"`)
O `AndroidManifest.xml` define `android:allowBackup="true"` com as regras em `@xml/backup_rules` e `@xml/data_extraction_rules`. Se você tiver o backup do sistema Android ativado nas configurações do seu aparelho, o próprio sistema operacional Android pode incluir os dados locais do aplicativo nas rotinas de backup/restauração do seu dispositivo.

---

## 3. Transmissão de Informações e Serviços Externos

### O que o código do LeNote NÃO faz:
- O código-fonte do LeNote (`app/src/main/java/com/example/lenote/**`) **não** instancia `HttpURLConnection`, `OkHttpClient`, `Retrofit`, `WebView` nem qualquer socket de rede.
- O conteúdo das suas notas, seus arquivos `.ink`, seus PDFs e suas imagens **nunca** são enviados para servidores da autora do projeto nem para serviços de nuvem de terceiros para processamento.

### O que a dependência Google ML Kit faz:
Para oferecer conversão de caligrafia em fonte tipográfica e busca por palavras manuscritas, o LeNote utiliza a biblioteca oficial `com.google.mlkit:digital-ink-recognition:19.0.0` ([`DigitalInkManager.kt`](app/src/main/java/com/example/lenote/ink/DigitalInkManager.kt)).
1. **Download do Modelo**: Na inicialização, `DigitalInkManager.ensureModelDownloaded()` consulta `RemoteModelManager.getInstance().isModelDownloaded(model)`. Se o modelo neural do idioma (ex.: `pt-BR` ou `en-US`) ainda não estiver no aparelho, o ML Kit faz o download do arquivo de modelo a partir dos servidores do Google.
2. **Inferência no Dispositivo (*On-Device*)**: Uma vez que o modelo está baixado no dispositivo, o reconhecimento dos seus traços (`recognizer.recognize(ink)`) roda localmente na CPU do seu tablet/celular através da biblioteca nativa `libdigitalink.so`, sem enviar os traços da página para a nuvem.
3. **Telemetria Transitiva do SDK do ML Kit**: Como documentado em [`docs/DEPENDENCIES.md`](docs/DEPENDENCIES.md), o SDK proprietário do Google ML Kit embute transitivamente o componente `com.google.android.datatransport`, que segundo a documentação oficial do Google ML Kit pode reportar métricas técnicas de uso/diagnóstico do modelo (como versão do SDK, sucesso/falha de download e latência de inferência) aos servidores do Google.

---

## 4. Reconhecimento de Escrita (Google ML Kit) e Download de Modelos

- **Posso usar o LeNote sem internet?**
  - **Sim.** Todas as funções de criação de cadernos, escrita com caneta stylus, borracha parcial, seleção por laço, refinamento geométrico de traços, pautas, importação/exportação de PDF, exportação de PNG e backup `.lenote` funcionam **100% offline**, mesmo em Modo Avião.
  - Apenas a **conversão de escrita para fonte tipográfica via OCR** e a **busca por palavras manuscritas** exigem que o modelo de idioma do ML Kit tenha sido baixado ao menos uma vez com conexão à internet. Após esse primeiro download, o reconhecimento também funciona offline.

---

## 5. Como Funcionam os Backups e Exportações

Todas as exportações e backups são iniciados exclusivamente por ação explícita do usuário:
- **Exportação de PDF e PNG**: Utiliza a API `MediaStore` do Android (`Download/LeNote` e `Pictures/LeNote`) e o `androidx.core.content.FileProvider` (`io.github.lirielc.lenote.fileprovider`) com permissões temporárias de URI (`FLAG_GRANT_READ_URI_PERMISSION`). O LeNote não solicita permissão ampla de leitura de todo o armazenamento externo (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`).
- **Importação de PDF, Imagens e `.lenote`**: Utiliza o **Storage Access Framework (SAF)** nativo do sistema Android (`ActivityResultContracts.OpenDocument` / `GetContent`). O aplicativo recebe acesso apenas ao arquivo específico que você escolheu na tela do sistema.

---

## 6. Auditoria de Permissões do Manifesto Compilado

No arquivo [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) do código-fonte do LeNote, **nenhuma tag `<uses-permission>` é declarada diretamente**.

Entretanto, durante a compilação do APK, o **Manifest Merger** do Android mescla as permissões exigidas pelas bibliotecas transitivas do **Google ML Kit** e do **AndroidX WorkManager**. No APK final compilado (`AndroidManifest.xml` mesclado), constam exatamente as seguintes 6 permissões:

| Permissão no APK Compilado | Origem (Biblioteca que a injeta) | Finalidade Técnica |
|---|---|---|
| `android.permission.INTERNET` | `com.google.mlkit:common` | Necessária para o `RemoteModelManager` baixar o modelo offline de reconhecimento de escrita do idioma (`pt-BR` / `en-US`). |
| `android.permission.ACCESS_NETWORK_STATE` | `com.google.mlkit:common` / `androidx.work` | Verifica se há conexão ativa antes de tentar baixar o modelo de idioma do ML Kit. |
| `android.permission.WAKE_LOCK` | `androidx.work:work-runtime` (transitiva do ML Kit) | Mantém o job de download do modelo ativo até concluir a transferência em segundo plano. |
| `android.permission.FOREGROUND_SERVICE` | `androidx.work:work-runtime` (transitiva do ML Kit) | Declarada pelo `SystemForegroundService` do WorkManager para tarefas longas em background. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | `androidx.work:work-runtime` (transitiva do ML Kit) | Declarada pelo `RescheduleReceiver` do WorkManager para reagendar jobs pendentes após reinicialização. |
| `io.github.lirielc.lenote.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `androidx.core:core` (Android 13+) | Permissão interna de proteção por assinatura (`protectionLevel="signature"`) gerada automaticamente pelo AndroidX para isolar `BroadcastReceiver`s internos do próprio aplicativo. |

**Permissões Sensíveis NÃO presentes no LeNote:**
- ❌ Sem acesso à Câmera (`CAMERA`)
- ❌ Sem acesso ao Microfone (`RECORD_AUDIO`)
- ❌ Sem acesso à Localização (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)
- ❌ Sem acesso a Contatos, Telefone ou SMS (`READ_CONTACTS`, `READ_PHONE_STATE`)
- ❌ Sem acesso irrestrito aos arquivos do aparelho (`MANAGE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`)

---

## 7. Como Verificar Você Mesmo (Auditoria Independente)

Qualquer desenvolvedor ou usuário pode inspecionar o APK compilado usando a ferramenta `aapt` do Android SDK:

```bash
# Listar todas as permissões do APK compilado
aapt dump permissions app/build/outputs/apk/release/app-release.apk

# Inspecionar a árvore completa do AndroidManifest.xml mesclado dentro do APK
aapt dump xmltree app/build/outputs/apk/release/app-release.apk AndroidManifest.xml
```
