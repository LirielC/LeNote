package com.example.lenote.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.lenote.data.AppLanguage
import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.HighlighterSize
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PaperColors
import com.example.lenote.ink.PenStrokeSize
import java.util.Locale

/**
 * Idioma efetivo resolvido para a interface do LeNote (PT, EN ou ES).
 */
enum class ResolvedLanguage(
    val badgeCode: String,
    val nativeName: String,
    val locale: Locale
) {
    PT("PT", "Português", Locale("pt", "BR")),
    EN("EN", "English", Locale.ENGLISH),
    ES("ES", "Español", Locale("es", "ES"))
}

/**
 * Resolve o [AppLanguage] configurado pelo usuário para um dos 3 idiomas suportados (PT, EN, ES).
 * Quando configurado como [AppLanguage.SYSTEM], detecta automaticamente o idioma do Android.
 */
fun resolveAppLanguage(appLanguage: AppLanguage, systemLocale: Locale = Locale.getDefault()): ResolvedLanguage {
    return when (appLanguage) {
        AppLanguage.PT -> ResolvedLanguage.PT
        AppLanguage.EN -> ResolvedLanguage.EN
        AppLanguage.ES -> ResolvedLanguage.ES
        AppLanguage.SYSTEM -> {
            val lang = systemLocale.language.lowercase(Locale.ROOT)
            when {
                lang.startsWith("pt") -> ResolvedLanguage.PT
                lang.startsWith("es") -> ResolvedLanguage.ES
                lang.startsWith("en") -> ResolvedLanguage.EN
                else -> ResolvedLanguage.EN
            }
        }
    }
}

/**
 * Dicionário completo e fortemente tipado de strings da interface do LeNote.
 */
data class LeNoteStrings(
    val language: ResolvedLanguage,

    // Seletor de Idioma
    val languagePickerOverline: String,
    val languagePickerTitle: String,
    val languageSystemOption: String,
    val languageSystemSubtitle: (ResolvedLanguage) -> String,
    val languagePortuguese: String,
    val languageEnglish: String,
    val languageSpanish: String,

    // HomeScreen — Masthead & Navegação
    val homeOverline: String,
    val homeTitle: String,
    val homeStatsSummary: (notebooks: Int, pages: Int) -> String,
    val actionPdf: String,
    val actionBackup: String,
    val lightModeDesc: String,
    val darkModeDesc: String,
    val languageButtonDesc: String,
    val searchNotebooksPlaceholder: String,
    val tabLibrary: String,
    val tabFavorites: String,
    val tabTrash: String,
    val fabNewNotebook: String,
    val fabEmptyTrash: String,

    // HomeScreen — Biblioteca & Cards
    val sectionContinueWriting: String,
    val sectionContinueWritingSubtitle: String,
    val sectionNotebookCollection: String,
    val sectionMainShelfSubtitle: String,
    val sectionActiveFilterSubtitle: String,
    val cardNotebookOverline: String,
    val cardNotebookStats: (sections: Int, pages: Int) -> String,
    val menuNotebookOptionsDesc: String,
    val menuExportLenote: String,
    val menuMoveToTrash: String,
    val openNotebookDesc: String,

    // HomeScreen — Estados Vazios
    val emptySearchOverline: String,
    val emptySearchTitle: (query: String) -> String,
    val emptySearchDescription: String,
    val actionClearSearch: String,
    val emptyLibraryOverline: String,
    val emptyLibraryTitle: String,
    val emptyLibraryDescription: String,
    val actionCreateFirstNotebook: String,
    val emptyFavoritesOverline: String,
    val emptyFavoritesTitle: String,
    val emptyFavoritesDescription: String,
    val sectionFavoritePages: String,
    val sectionFavoritePagesSubtitle: String,
    val emptyTrashOverline: String,
    val emptyTrashTitle: String,
    val emptyTrashDescription: String,
    val sectionDeletedNotebooks: String,
    val sectionDeletedPages: String,
    val deletedOnDate: (date: String) -> String,
    val unknownDate: String,
    val actionRestore: String,
    val deletePermanentlyDesc: String,

    // HomeScreen — Banners de Feedback (PDF / .lenote / Lixeira)
    val bannerImportingPdfTitle: String,
    val bannerImportingPdfMessage: (fileName: String) -> String,
    val bannerPdfImportedTitle: String,
    val bannerPdfImportedMessage: (title: String) -> String,
    val bannerPdfImportFailedTitle: String,
    val bannerPdfImportFailedDefaultMsg: String,
    val bannerImportingLenoteTitle: String,
    val bannerImportingLenoteMessage: String,
    val bannerLenoteImportedTitle: String,
    val bannerLenoteImportedMessage: (title: String) -> String,
    val bannerLenoteInvalidTitle: String,
    val bannerLenoteInvalidMessage: String,
    val bannerImportErrorTitle: String,
    val bannerImportErrorDefaultMsg: String,
    val bannerExportingNotebookTitle: (title: String) -> String,
    val bannerExportingNotebookMessage: String,
    val bannerExportSuccessTitle: String,
    val bannerExportSuccessMessage: (title: String) -> String,
    val bannerExportWriteErrorTitle: String,
    val bannerExportFailedTitle: String,
    val bannerExportFailedMessage: String,
    val bannerMovedToTrashTitle: String,
    val bannerMovedToTrashMessage: (title: String) -> String,
    val bannerNotebookRestoredTitle: String,
    val bannerNotebookRestoredMessage: (title: String) -> String,
    val bannerPageRestoredTitle: String,
    val bannerPageRestoredMessage: (title: String) -> String,
    val bannerNotebookCreatedTitle: String,
    val bannerNotebookCreatedMessage: (title: String) -> String,
    val bannerTrashEmptiedTitle: String,
    val bannerTrashEmptiedMessage: String,

    // Diálogos da HomeScreen
    val createNotebookOverline: String,
    val createNotebookTitle: String,
    val notebookTitleLabel: String,
    val notebookTitlePlaceholder: String,
    val notebookBindingColorLabel: String,
    val defaultNewNotebookName: String,
    val actionCreateNotebook: String,
    val actionCancel: String,
    val actionConfirm: String,
    val actionClose: String,
    val actionSave: String,
    val actionDelete: String,
    val emptyTrashDialogTitle: String,
    val emptyTrashDialogMessage: String,
    val actionEmptyPermanently: String,
    val deleteNotebookDialogTitle: String,
    val deleteNotebookDialogMessage: (title: String) -> String,
    val deletePageDialogTitle: String,
    val deletePageDialogMessage: (title: String) -> String,

    // InkPlaygroundScreen — Header & Toolbar
    val backToLibraryDesc: String,
    val notebookIndexDesc: String,
    val indexDefaultLabel: String,
    val defaultPageTitle: (index: Int) -> String,
    val renamePageDesc: String,
    val favoritePageDesc: String,
    val unfavoritePageDesc: String,
    val statusSaving: String,
    val statusSaved: String,
    val sheetTemplateAndColorDesc: String,
    val morePageOptionsDesc: String,
    val toolPen: String,
    val toolHighlighter: String,
    val toolEraser: String,
    val toolLasso: String,
    val toolTextBox: String,
    val toolInsertImage: String,
    val toolTouchInking: String,
    val toolUndo: String,
    val toolRedo: String,
    val lassoSelectedCount: (count: Int) -> String,
    val lassoBeautifyDesc: String,
    val lassoAlignBaselineDesc: String,
    val lassoDuplicateDesc: String,
    val lassoDeleteSelectionDesc: String,
    val lassoClearSelectionDesc: String,
    val lassoHint: String,

    // InkPlaygroundScreen — Diálogos & Opções da Página
    val currentSheetOverline: String,
    val renamePageTitle: String,
    val pageTitlePlaceholder: String,
    val clearStrokesDialogTitle: String,
    val clearStrokesDialogMessage: String,
    val actionClearSheet: String,
    val movePageToTrashDialogTitle: String,
    val movePageToTrashDialogMessage: String,
    val sheetOptionsTitle: String,
    val strokeSmoothingLabel: String,
    val pageOptionsOverline: String,
    val currentSheetFallback: String,
    val sectionOrganization: String,
    val actionRenameSheet: String,
    val actionDuplicateSheet: String,
    val actionDuplicateSheetSubtitle: String,
    val actionMoveUp: String,
    val actionMoveDown: String,
    val actionAddFavorite: String,
    val actionRemoveFavorite: String,
    val sectionPaperAndRuling: String,
    val actionSheetTemplateTitle: String,
    val actionSheetTemplateSubtitle: String,
    val sectionExportAndShare: String,
    val actionExportPdfTitle: String,
    val actionExportPdfSubtitle: String,
    val actionExportPngTitle: String,
    val actionExportPngSubtitle: String,
    val sectionEngineering: String,
    val actionTelemetryTitle: String,
    val telemetryVisibleSubtitle: String,
    val telemetryHiddenSubtitle: String,
    val sectionDangerZone: String,
    val actionClearAllStrokesTitle: String,
    val actionClearAllStrokesSubtitle: String,
    val actionMoveSheetToTrashTitle: String,
    val actionMoveSheetToTrashSubtitle: String,

    // Exportação & Zoom
    val exportPdfSuccessTitle: String,
    val exportPdfErrorTitle: String,
    val exportPngSuccessTitle: String,
    val exportPngErrorTitle: String,
    val exportOpenPdfButton: String,
    val exportViewImageButton: String,
    val exportNoAppFoundError: (type: String) -> String,
    val exportOpenFailedError: (type: String) -> String,
    val zoomOutDesc: String,
    val zoomInDesc: String,
    val zoomFitToPageDesc: String,
    val zoomCloseDesc: String,

    // CollapsibleNavigationSidebar
    val sidebarOverline: String,
    val sidebarTitle: String,
    val sidebarSearchPlaceholder: String,
    val sidebarSearchResultsHeader: (count: Int) -> String,
    val sidebarSearchEmptyOverline: String,
    val sidebarSearchEmptyTitle: String,
    val sidebarSearchEmptyDesc: (query: String) -> String,
    val sidebarNotebooksHeader: (count: Int) -> String,
    val sidebarSectionsHeader: (count: Int) -> String,
    val sidebarPagesHeader: (count: Int) -> String,
    val sidebarFavoritesHeader: (count: Int) -> String,
    val sidebarFilterAll: String,
    val sidebarFilterStars: String,
    val sidebarNewSheetButton: String,
    val sidebarTrashButton: (count: Int) -> String,
    val sidebarTrashDialogTitle: String,
    val sidebarEmptyAction: String,
    val sidebarRenameNotebookTitle: String,
    val sidebarNewSectionTitle: String,
    val sidebarRenameSectionTitle: String,
    val sidebarEnterNamePlaceholder: String,

    // PageTemplatePickerDialog
    val templateNewSheetOverline: String,
    val templateAttributesOverline: String,
    val templateNewSheetTitle: String,
    val templateAttributesTitle: String,
    val templateSheetTitleLabel: String,
    val templateSheetTitlePlaceholder: String,
    val templatePaperStockHeader: String,
    val templateRulingHeader: String,
    val actionCreateSheet: String,
    val actionApplyToSheet: String,

    // Enums Traduzidos (Fundos de Página, Cores de Papel, Espessuras)
    val pageBackgroundLabel: (PageBackground) -> String,
    val paperColorLabel: (Long) -> String,
    val penStrokeSizeLabel: (PenStrokeSize) -> String,
    val highlighterSizeLabel: (HighlighterSize) -> String,
    val eraserSizeLabel: (EraserSize) -> String
)

val PtStrings = LeNoteStrings(
    language = ResolvedLanguage.PT,
    languagePickerOverline = "IDIOMA DA INTERFACE",
    languagePickerTitle = "Escolher Idioma",
    languageSystemOption = "Automático (Sistema Android)",
    languageSystemSubtitle = { resolved -> "Seguindo o idioma do dispositivo (${resolved.nativeName})" },
    languagePortuguese = "Português",
    languageEnglish = "English",
    languageSpanish = "Español",

    homeOverline = "ATELIER DE NOTAS",
    homeTitle = "Meus Cadernos",
    homeStatsSummary = { nb, pg -> "$nb cadernos  •  $pg folhas" },
    actionPdf = "PDF",
    actionBackup = "Backup",
    lightModeDesc = "Modo Claro",
    darkModeDesc = "Modo Noturno",
    languageButtonDesc = "Alterar Idioma",
    searchNotebooksPlaceholder = "Buscar cadernos, notas ou tópicos...",
    tabLibrary = "Biblioteca",
    tabFavorites = "Favoritas",
    tabTrash = "Lixeira",
    fabNewNotebook = "Novo Caderno",
    fabEmptyTrash = "Esvaziar Lixeira",

    sectionContinueWriting = "Continuar Escrevendo",
    sectionContinueWritingSubtitle = "Folhas recentes",
    sectionNotebookCollection = "Coleção de Cadernos",
    sectionMainShelfSubtitle = "Estante principal",
    sectionActiveFilterSubtitle = "Filtro ativo",
    cardNotebookOverline = "CADERNO",
    cardNotebookStats = { sec, pg -> "$sec seções  •  $pg pág." },
    menuNotebookOptionsDesc = "Opções do caderno",
    menuExportLenote = "Exportar (.lenote)",
    menuMoveToTrash = "Mover para Lixeira",
    openNotebookDesc = "Abrir caderno",

    emptySearchOverline = "Sem resultados",
    emptySearchTitle = { q -> "Nenhum caderno para \"$q\"" },
    emptySearchDescription = "Verifique a grafia ou limpe o filtro de busca para visualizar toda a sua coleção.",
    actionClearSearch = "Limpar Busca",
    emptyLibraryOverline = "Acervo Vazio",
    emptyLibraryTitle = "Comece seu primeiro caderno",
    emptyLibraryDescription = "Crie um caderno em branco para manuscritos e esboços ou importe um PDF para leitura e anotação.",
    actionCreateFirstNotebook = "Criar Primeiro Caderno",
    emptyFavoritesOverline = "Coleção de Destaques",
    emptyFavoritesTitle = "Nenhuma página favorita",
    emptyFavoritesDescription = "Toque no ícone de estrela no topo do canvas ou na barra lateral para fixar suas folhas mais importantes aqui.",
    sectionFavoritePages = "Folhas Favoritas",
    sectionFavoritePagesSubtitle = "Acesso rápido",
    emptyTrashOverline = "Arquivo de Exclusão",
    emptyTrashTitle = "A lixeira está vazia",
    emptyTrashDescription = "Cadernos e páginas removidos ficam arquivados aqui temporariamente antes da exclusão definitiva.",
    sectionDeletedNotebooks = "Cadernos Arquivados na Lixeira",
    sectionDeletedPages = "Folhas Soltas na Lixeira",
    deletedOnDate = { d -> "Excluído em $d" },
    unknownDate = "Data desconhecida",
    actionRestore = "Restaurar",
    deletePermanentlyDesc = "Excluir definitivamente",

    bannerImportingPdfTitle = "Importando documento PDF...",
    bannerImportingPdfMessage = { name -> "Renderizando páginas de '$name' para o seu caderno." },
    bannerPdfImportedTitle = "PDF importado com sucesso",
    bannerPdfImportedMessage = { title -> "Caderno '$title' pronto para anotação." },
    bannerPdfImportFailedTitle = "Falha ao importar PDF",
    bannerPdfImportFailedDefaultMsg = "Verifique se o arquivo PDF é válido.",
    bannerImportingLenoteTitle = "Importando pacote .lenote...",
    bannerImportingLenoteMessage = "Restaurando seções, páginas e traços vetoriais.",
    bannerLenoteImportedTitle = "Caderno importado",
    bannerLenoteImportedMessage = { title -> "'$title' adicionado à biblioteca." },
    bannerLenoteInvalidTitle = "Pacote .lenote inválido",
    bannerLenoteInvalidMessage = "Não foi possível ler a estrutura do arquivo selecionado.",
    bannerImportErrorTitle = "Erro na importação",
    bannerImportErrorDefaultMsg = "Falha de leitura do pacote.",
    bannerExportingNotebookTitle = { title -> "Exportando '$title'..." },
    bannerExportingNotebookMessage = "Compactando traços, mídias e metadados.",
    bannerExportSuccessTitle = "Backup .lenote concluído",
    bannerExportSuccessMessage = { title -> "O caderno '$title' foi salvo com sucesso." },
    bannerExportWriteErrorTitle = "Erro ao gravar arquivo",
    bannerExportFailedTitle = "Falha na exportação",
    bannerExportFailedMessage = "Não foi possível gerar o pacote do caderno.",
    bannerMovedToTrashTitle = "Movido para a lixeira",
    bannerMovedToTrashMessage = { title -> "'$title' pode ser restaurado na aba Lixeira." },
    bannerNotebookRestoredTitle = "Caderno restaurado",
    bannerNotebookRestoredMessage = { title -> "'$title' voltou para a sua biblioteca." },
    bannerPageRestoredTitle = "Página restaurada",
    bannerPageRestoredMessage = { title -> "'$title' foi restaurada ao caderno." },
    bannerNotebookCreatedTitle = "Caderno criado",
    bannerNotebookCreatedMessage = { title -> "Abrindo '$title'..." },
    bannerTrashEmptiedTitle = "Lixeira esvaziada",
    bannerTrashEmptiedMessage = "Todos os itens foram removidos permanentemente.",

    createNotebookOverline = "NOVO VOLUME",
    createNotebookTitle = "Criar Caderno",
    notebookTitleLabel = "Título do Caderno",
    notebookTitlePlaceholder = "Ex: Arquitetura, Diário de Campo...",
    notebookBindingColorLabel = "TONALIDADE DA ENCADERNAÇÃO",
    defaultNewNotebookName = "Novo Caderno",
    actionCreateNotebook = "Criar Caderno",
    actionCancel = "Cancelar",
    actionConfirm = "Confirmar",
    actionClose = "Fechar",
    actionSave = "Salvar",
    actionDelete = "Excluir",
    emptyTrashDialogTitle = "Esvaziar Lixeira?",
    emptyTrashDialogMessage = "Todos os cadernos, seções, páginas e mídias da lixeira serão excluídos definitivamente do dispositivo. Esta ação não pode ser desfeita.",
    actionEmptyPermanently = "Esvaziar Definitivamente",
    deleteNotebookDialogTitle = "Excluir Caderno Definitivamente?",
    deleteNotebookDialogMessage = { title -> "O caderno '$title' e todas as suas páginas serão removidos permanentemente do armazenamento." },
    deletePageDialogTitle = "Excluir Página Definitivamente?",
    deletePageDialogMessage = { title -> "A página '$title' será apagada permanentemente." },

    backToLibraryDesc = "Voltar para Biblioteca",
    notebookIndexDesc = "Sumário do Caderno",
    indexDefaultLabel = "Sumário",
    defaultPageTitle = { idx -> "Página $idx" },
    renamePageDesc = "Renomear página",
    favoritePageDesc = "Favoritar página",
    unfavoritePageDesc = "Remover dos favoritos",
    statusSaving = "Salvando...",
    statusSaved = "Salvo",
    sheetTemplateAndColorDesc = "Modelo e Cor da Folha",
    morePageOptionsDesc = "Mais opções da página",
    toolPen = "Caneta",
    toolHighlighter = "Marca-texto",
    toolEraser = "Borracha",
    toolLasso = "Laço de Seleção",
    toolTextBox = "Inserir Caixa de Texto",
    toolInsertImage = "Inserir Imagem",
    toolTouchInking = "Escrita com o Dedo",
    toolUndo = "Desfazer",
    toolRedo = "Refazer",
    lassoSelectedCount = { c -> "$c sel.:" },
    lassoBeautifyDesc = "Embelezar Caligrafia",
    lassoAlignBaselineDesc = "Alinhar à Pauta",
    lassoDuplicateDesc = "Duplicar",
    lassoDeleteSelectionDesc = "Excluir traços selecionados",
    lassoClearSelectionDesc = "Desmarcar",
    lassoHint = "Circule os traços para selecionar, mover ou refinar caligrafia",

    currentSheetOverline = "FOLHA ATUAL",
    renamePageTitle = "Renomear Página",
    pageTitlePlaceholder = "Título da página",
    clearStrokesDialogTitle = "Limpar todos os traços?",
    clearStrokesDialogMessage = "Todos os traços manuscritos desta folha serão apagados. Você pode reverter imediatamente usando o botão Desfazer.",
    actionClearSheet = "Limpar Folha",
    movePageToTrashDialogTitle = "Mover página para a lixeira?",
    movePageToTrashDialogMessage = "Esta página será movida para a lixeira da biblioteca e poderá ser restaurada caso necessário.",
    sheetOptionsTitle = "Opções da Folha",
    strokeSmoothingLabel = "SUAVIZAÇÃO DO TRAÇO",
    pageOptionsOverline = "OPÇÕES DE PÁGINA",
    currentSheetFallback = "Folha Atual",
    sectionOrganization = "ORGANIZAÇÃO",
    actionRenameSheet = "Renomear folha",
    actionDuplicateSheet = "Duplicar folha",
    actionDuplicateSheetSubtitle = "Cria uma cópia idêntica no caderno",
    actionMoveUp = "Mover para cima",
    actionMoveDown = "Mover para baixo",
    actionAddFavorite = "Marcar como favorita",
    actionRemoveFavorite = "Remover dos favoritos",
    sectionPaperAndRuling = "PAPEL & PAUTA",
    actionSheetTemplateTitle = "Modelo e cor do papel",
    actionSheetTemplateSubtitle = "Cornell, pautado, pontilhado, música e tons de papel",
    sectionExportAndShare = "EXPORTAR & COMPARTILHAR",
    actionExportPdfTitle = "Exportar como PDF",
    actionExportPdfSubtitle = "Documento vetorial pronto para impressão",
    actionExportPngTitle = "Exportar como Imagem (PNG)",
    actionExportPngSubtitle = "Imagem de alta resolução da folha",
    sectionEngineering = "ENGENHARIA",
    actionTelemetryTitle = "Telemetria Jetpack Ink",
    telemetryVisibleSubtitle = "Visível no canvas",
    telemetryHiddenSubtitle = "Oculto",
    sectionDangerZone = "ZONA DE EXCLUSÃO",
    actionClearAllStrokesTitle = "Limpar todos os traços",
    actionClearAllStrokesSubtitle = "Apaga o conteúdo mantendo a folha",
    actionMoveSheetToTrashTitle = "Mover folha para a lixeira",
    actionMoveSheetToTrashSubtitle = "Remove esta folha do caderno",

    exportPdfSuccessTitle = "PDF exportado com sucesso",
    exportPdfErrorTitle = "Não foi possível exportar o PDF.",
    exportPngSuccessTitle = "Imagem exportada com sucesso",
    exportPngErrorTitle = "Não foi possível exportar a imagem.",
    exportOpenPdfButton = "Abrir PDF",
    exportViewImageButton = "Ver imagem",
    exportNoAppFoundError = { type -> "Não foi encontrado um aplicativo para abrir $type." },
    exportOpenFailedError = { type -> "$type salvo com sucesso, mas não foi possível abrir o arquivo." },
    zoomOutDesc = "Diminuir zoom",
    zoomInDesc = "Aumentar zoom",
    zoomFitToPageDesc = "Ajustar à página",
    zoomCloseDesc = "Fechar controle de zoom",

    sidebarOverline = "ÍNDICE & SUMÁRIO",
    sidebarTitle = "Cadernos & Folhas",
    sidebarSearchPlaceholder = "Buscar folhas ou texto manuscrito...",
    sidebarSearchResultsHeader = { c -> "RESULTADOS ($c)" },
    sidebarSearchEmptyOverline = "Busca no Caderno",
    sidebarSearchEmptyTitle = "Nenhuma folha encontrada",
    sidebarSearchEmptyDesc = { q -> "Nenhum título ou texto manuscrito corresponde a \"$q\"." },
    sidebarNotebooksHeader = { c -> "CADERNOS ($c)" },
    sidebarSectionsHeader = { c -> "SEÇÕES ($c)" },
    sidebarPagesHeader = { c -> "FOLHAS ($c)" },
    sidebarFavoritesHeader = { c -> "FAVORITAS ($c)" },
    sidebarFilterAll = "Todas",
    sidebarFilterStars = "Estrelas",
    sidebarNewSheetButton = "Nova Folha",
    sidebarTrashButton = { c -> "Lixeira ($c)" },
    sidebarTrashDialogTitle = "Lixeira do Aplicativo",
    sidebarEmptyAction = "Esvaziar",
    sidebarRenameNotebookTitle = "Renomear Caderno",
    sidebarNewSectionTitle = "Nova Seção",
    sidebarRenameSectionTitle = "Renomear Seção",
    sidebarEnterNamePlaceholder = "Digite o nome...",

    templateNewSheetOverline = "NOVA FOLHA DE CADERNO",
    templateAttributesOverline = "ATRIBUTOS DO PAPEL",
    templateNewSheetTitle = "Escolher Pauta & Papel",
    templateAttributesTitle = "Modelo & Cor da Folha",
    templateSheetTitleLabel = "Título da Folha",
    templateSheetTitlePlaceholder = "Ex: Notas de Leitura, Esboço...",
    templatePaperStockHeader = "GRAMATURA & TONALIDADE DO PAPEL",
    templateRulingHeader = "PAUTA & GEOMETRIA DA FOLHA",
    actionCreateSheet = "Criar Folha",
    actionApplyToSheet = "Aplicar à Folha",

    pageBackgroundLabel = { bg ->
        when (bg) {
            PageBackground.BLANK -> "Branca"
            PageBackground.RULED -> "Pautada"
            PageBackground.GRID -> "Quadriculada"
            PageBackground.DOTS -> "Pontilhada"
            PageBackground.MATHEMATICS -> "Matemática"
            PageBackground.CORNELL -> "Cornell"
            PageBackground.WEEKLY_PLANNER -> "Planner"
            PageBackground.ENGINEERING_GRAPH -> "Engenharia"
            PageBackground.MUSIC_STAFF -> "Partitura"
        }
    },
    paperColorLabel = { colorLong ->
        when (colorLong) {
            PaperColors.CLASSIC_WHITE -> "Branco"
            PaperColors.LEGAL_YELLOW -> "Amarelo"
            PaperColors.SEPIA_CREAM -> "Sépia Suave"
            PaperColors.OLED_BLACK -> "Grafite"
            else -> "Branco"
        }
    },
    penStrokeSizeLabel = { sz ->
        when (sz) {
            PenStrokeSize.FINE -> "Fina"
            PenStrokeSize.MEDIUM -> "Média"
            PenStrokeSize.THICK -> "Grossa"
            PenStrokeSize.EXTRA_THICK -> "Extra"
        }
    },
    highlighterSizeLabel = { sz ->
        when (sz) {
            HighlighterSize.NARROW -> "Fino"
            HighlighterSize.MEDIUM -> "Médio"
            HighlighterSize.WIDE -> "Largo"
        }
    },
    eraserSizeLabel = { sz ->
        when (sz) {
            EraserSize.SMALL -> "P"
            EraserSize.MEDIUM -> "M"
            EraserSize.LARGE -> "G"
        }
    }
)

val EnStrings = LeNoteStrings(
    language = ResolvedLanguage.EN,
    languagePickerOverline = "INTERFACE LANGUAGE",
    languagePickerTitle = "Select Language",
    languageSystemOption = "Automatic (Android System)",
    languageSystemSubtitle = { resolved -> "Following device language (${resolved.nativeName})" },
    languagePortuguese = "Português",
    languageEnglish = "English",
    languageSpanish = "Español",

    homeOverline = "ATELIER",
    homeTitle = "My Notebooks",
    homeStatsSummary = { nb, pg -> "$nb notebooks  •  $pg sheets" },
    actionPdf = "PDF",
    actionBackup = "Backup",
    lightModeDesc = "Light Mode",
    darkModeDesc = "Dark Mode",
    languageButtonDesc = "Change Language",
    searchNotebooksPlaceholder = "Search notebooks, notes, or topics...",
    tabLibrary = "Library",
    tabFavorites = "Favorites",
    tabTrash = "Trash",
    fabNewNotebook = "New Notebook",
    fabEmptyTrash = "Empty Trash",

    sectionContinueWriting = "Continue Writing",
    sectionContinueWritingSubtitle = "Recent sheets",
    sectionNotebookCollection = "Notebook Collection",
    sectionMainShelfSubtitle = "Main bookshelf",
    sectionActiveFilterSubtitle = "Active filter",
    cardNotebookOverline = "NOTEBOOK",
    cardNotebookStats = { sec, pg -> "$sec sections  •  $pg p." },
    menuNotebookOptionsDesc = "Notebook options",
    menuExportLenote = "Export (.lenote)",
    menuMoveToTrash = "Move to Trash",
    openNotebookDesc = "Open notebook",

    emptySearchOverline = "No results",
    emptySearchTitle = { q -> "No notebooks for \"$q\"" },
    emptySearchDescription = "Check your spelling or clear the search filter to browse your entire collection.",
    actionClearSearch = "Clear Search",
    emptyLibraryOverline = "Empty Archive",
    emptyLibraryTitle = "Start your first notebook",
    emptyLibraryDescription = "Create a blank notebook for handwritten notes and sketches, or import a PDF document to annotate.",
    actionCreateFirstNotebook = "Create First Notebook",
    emptyFavoritesOverline = "Featured Collection",
    emptyFavoritesTitle = "No favorite sheets yet",
    emptyFavoritesDescription = "Tap the star icon in the canvas header or navigation sidebar to pin your most important sheets here.",
    sectionFavoritePages = "Favorite Sheets",
    sectionFavoritePagesSubtitle = "Quick access",
    emptyTrashOverline = "Archive Bin",
    emptyTrashTitle = "Trash is empty",
    emptyTrashDescription = "Deleted notebooks and sheets are kept here temporarily before permanent removal.",
    sectionDeletedNotebooks = "Archived Notebooks in Trash",
    sectionDeletedPages = "Loose Sheets in Trash",
    deletedOnDate = { d -> "Deleted on $d" },
    unknownDate = "Unknown date",
    actionRestore = "Restore",
    deletePermanentlyDesc = "Delete permanently",

    bannerImportingPdfTitle = "Importing PDF document...",
    bannerImportingPdfMessage = { name -> "Rendering pages from '$name' into your notebook." },
    bannerPdfImportedTitle = "PDF imported successfully",
    bannerPdfImportedMessage = { title -> "Notebook '$title' is ready for annotation." },
    bannerPdfImportFailedTitle = "Failed to import PDF",
    bannerPdfImportFailedDefaultMsg = "Please verify that the selected PDF file is valid.",
    bannerImportingLenoteTitle = "Importing .lenote package...",
    bannerImportingLenoteMessage = "Restoring sections, sheets, and vector strokes.",
    bannerLenoteImportedTitle = "Notebook imported",
    bannerLenoteImportedMessage = { title -> "'$title' was added to your library." },
    bannerLenoteInvalidTitle = "Invalid .lenote package",
    bannerLenoteInvalidMessage = "Could not read the structure of the selected archive.",
    bannerImportErrorTitle = "Import error",
    bannerImportErrorDefaultMsg = "Failed to read package file.",
    bannerExportingNotebookTitle = { title -> "Exporting '$title'..." },
    bannerExportingNotebookMessage = "Compressing vector strokes, media, and metadata.",
    bannerExportSuccessTitle = ".lenote backup complete",
    bannerExportSuccessMessage = { title -> "Notebook '$title' was saved successfully." },
    bannerExportWriteErrorTitle = "Error writing file",
    bannerExportFailedTitle = "Export failed",
    bannerExportFailedMessage = "Could not generate the notebook package.",
    bannerMovedToTrashTitle = "Moved to trash",
    bannerMovedToTrashMessage = { title -> "'$title' can be restored from the Trash tab." },
    bannerNotebookRestoredTitle = "Notebook restored",
    bannerNotebookRestoredMessage = { title -> "'$title' is back in your library." },
    bannerPageRestoredTitle = "Sheet restored",
    bannerPageRestoredMessage = { title -> "'$title' was restored to its notebook." },
    bannerNotebookCreatedTitle = "Notebook created",
    bannerNotebookCreatedMessage = { title -> "Opening '$title'..." },
    bannerTrashEmptiedTitle = "Trash emptied",
    bannerTrashEmptiedMessage = "All items were permanently removed.",

    createNotebookOverline = "NEW VOLUME",
    createNotebookTitle = "Create Notebook",
    notebookTitleLabel = "Notebook Title",
    notebookTitlePlaceholder = "e.g., Architecture, Field Notes...",
    notebookBindingColorLabel = "CLOTH BINDING COLOR",
    defaultNewNotebookName = "New Notebook",
    actionCreateNotebook = "Create Notebook",
    actionCancel = "Cancel",
    actionConfirm = "Confirm",
    actionClose = "Close",
    actionSave = "Save",
    actionDelete = "Delete",
    emptyTrashDialogTitle = "Empty Trash?",
    emptyTrashDialogMessage = "All notebooks, sections, sheets, and media in the trash will be permanently deleted from this device. This action cannot be undone.",
    actionEmptyPermanently = "Empty Permanently",
    deleteNotebookDialogTitle = "Delete Notebook Permanently?",
    deleteNotebookDialogMessage = { title -> "Notebook '$title' and all of its sheets will be permanently removed from storage." },
    deletePageDialogTitle = "Delete Sheet Permanently?",
    deletePageDialogMessage = { title -> "Sheet '$title' will be permanently erased." },

    backToLibraryDesc = "Back to Library",
    notebookIndexDesc = "Notebook Index",
    indexDefaultLabel = "Index",
    defaultPageTitle = { idx -> "Page $idx" },
    renamePageDesc = "Rename sheet",
    favoritePageDesc = "Favorite sheet",
    unfavoritePageDesc = "Remove from favorites",
    statusSaving = "Saving...",
    statusSaved = "Saved",
    sheetTemplateAndColorDesc = "Sheet Template & Color",
    morePageOptionsDesc = "More sheet options",
    toolPen = "Pen",
    toolHighlighter = "Highlighter",
    toolEraser = "Eraser",
    toolLasso = "Lasso Selection",
    toolTextBox = "Insert Text Box",
    toolInsertImage = "Insert Image",
    toolTouchInking = "Finger Writing",
    toolUndo = "Undo",
    toolRedo = "Redo",
    lassoSelectedCount = { c -> "$c sel.:" },
    lassoBeautifyDesc = "Beautify Handwriting",
    lassoAlignBaselineDesc = "Align to Ruling",
    lassoDuplicateDesc = "Duplicate",
    lassoDeleteSelectionDesc = "Delete selected strokes",
    lassoClearSelectionDesc = "Clear selection",
    lassoHint = "Circle strokes to select, move, or refine handwriting",

    currentSheetOverline = "CURRENT SHEET",
    renamePageTitle = "Rename Sheet",
    pageTitlePlaceholder = "Sheet title",
    clearStrokesDialogTitle = "Clear all strokes?",
    clearStrokesDialogMessage = "All handwritten strokes on this sheet will be cleared. You can immediately revert this using Undo.",
    actionClearSheet = "Clear Sheet",
    movePageToTrashDialogTitle = "Move sheet to trash?",
    movePageToTrashDialogMessage = "This sheet will be moved to the library trash and can be restored later if needed.",
    sheetOptionsTitle = "Sheet Options",
    strokeSmoothingLabel = "STROKE SMOOTHING",
    pageOptionsOverline = "SHEET OPTIONS",
    currentSheetFallback = "Current Sheet",
    sectionOrganization = "ORGANIZATION",
    actionRenameSheet = "Rename sheet",
    actionDuplicateSheet = "Duplicate sheet",
    actionDuplicateSheetSubtitle = "Creates an identical copy in this notebook",
    actionMoveUp = "Move up",
    actionMoveDown = "Move down",
    actionAddFavorite = "Mark as favorite",
    actionRemoveFavorite = "Remove from favorites",
    sectionPaperAndRuling = "PAPER & RULING",
    actionSheetTemplateTitle = "Paper template & color",
    actionSheetTemplateSubtitle = "Cornell, ruled, dotted, music staff, and paper tones",
    sectionExportAndShare = "EXPORT & SHARE",
    actionExportPdfTitle = "Export as PDF",
    actionExportPdfSubtitle = "Print-ready vector document",
    actionExportPngTitle = "Export as Image (PNG)",
    actionExportPngSubtitle = "High-resolution sheet image",
    sectionEngineering = "ENGINEERING",
    actionTelemetryTitle = "Jetpack Ink Telemetry",
    telemetryVisibleSubtitle = "Visible on canvas",
    telemetryHiddenSubtitle = "Hidden",
    sectionDangerZone = "DANGER ZONE",
    actionClearAllStrokesTitle = "Clear all strokes",
    actionClearAllStrokesSubtitle = "Erases content while keeping the sheet",
    actionMoveSheetToTrashTitle = "Move sheet to trash",
    actionMoveSheetToTrashSubtitle = "Removes this sheet from the notebook",

    exportPdfSuccessTitle = "PDF exported successfully",
    exportPdfErrorTitle = "Could not export PDF.",
    exportPngSuccessTitle = "Image exported successfully",
    exportPngErrorTitle = "Could not export image.",
    exportOpenPdfButton = "Open PDF",
    exportViewImageButton = "View image",
    exportNoAppFoundError = { type -> "No application found to open $type." },
    exportOpenFailedError = { type -> "$type saved successfully, but the file could not be opened." },
    zoomOutDesc = "Zoom out",
    zoomInDesc = "Zoom in",
    zoomFitToPageDesc = "Fit to page",
    zoomCloseDesc = "Close zoom controls",

    sidebarOverline = "INDEX & CONTENTS",
    sidebarTitle = "Notebooks & Sheets",
    sidebarSearchPlaceholder = "Search sheets or handwritten text...",
    sidebarSearchResultsHeader = { c -> "RESULTS ($c)" },
    sidebarSearchEmptyOverline = "Notebook Search",
    sidebarSearchEmptyTitle = "No sheets found",
    sidebarSearchEmptyDesc = { q -> "No sheet title or handwritten text matches \"$q\"." },
    sidebarNotebooksHeader = { c -> "NOTEBOOKS ($c)" },
    sidebarSectionsHeader = { c -> "SECTIONS ($c)" },
    sidebarPagesHeader = { c -> "SHEETS ($c)" },
    sidebarFavoritesHeader = { c -> "FAVORITES ($c)" },
    sidebarFilterAll = "All",
    sidebarFilterStars = "Starred",
    sidebarNewSheetButton = "New Sheet",
    sidebarTrashButton = { c -> "Trash ($c)" },
    sidebarTrashDialogTitle = "Application Trash",
    sidebarEmptyAction = "Empty",
    sidebarRenameNotebookTitle = "Rename Notebook",
    sidebarNewSectionTitle = "New Section",
    sidebarRenameSectionTitle = "Rename Section",
    sidebarEnterNamePlaceholder = "Enter name...",

    templateNewSheetOverline = "NEW NOTEBOOK SHEET",
    templateAttributesOverline = "PAPER ATTRIBUTES",
    templateNewSheetTitle = "Choose Ruling & Paper",
    templateAttributesTitle = "Sheet Template & Color",
    templateSheetTitleLabel = "Sheet Title",
    templateSheetTitlePlaceholder = "e.g., Reading Notes, Sketch...",
    templatePaperStockHeader = "PAPER STOCK & TONE",
    templateRulingHeader = "RULING & SHEET GEOMETRY",
    actionCreateSheet = "Create Sheet",
    actionApplyToSheet = "Apply to Sheet",

    pageBackgroundLabel = { bg ->
        when (bg) {
            PageBackground.BLANK -> "Blank"
            PageBackground.RULED -> "Ruled"
            PageBackground.GRID -> "Grid"
            PageBackground.DOTS -> "Dotted"
            PageBackground.MATHEMATICS -> "Mathematics"
            PageBackground.CORNELL -> "Cornell"
            PageBackground.WEEKLY_PLANNER -> "Planner"
            PageBackground.ENGINEERING_GRAPH -> "Engineering"
            PageBackground.MUSIC_STAFF -> "Music Staff"
        }
    },
    paperColorLabel = { colorLong ->
        when (colorLong) {
            PaperColors.CLASSIC_WHITE -> "White"
            PaperColors.LEGAL_YELLOW -> "Legal Yellow"
            PaperColors.SEPIA_CREAM -> "Soft Sepia"
            PaperColors.OLED_BLACK -> "Graphite"
            else -> "White"
        }
    },
    penStrokeSizeLabel = { sz ->
        when (sz) {
            PenStrokeSize.FINE -> "Fine"
            PenStrokeSize.MEDIUM -> "Medium"
            PenStrokeSize.THICK -> "Thick"
            PenStrokeSize.EXTRA_THICK -> "Extra"
        }
    },
    highlighterSizeLabel = { sz ->
        when (sz) {
            HighlighterSize.NARROW -> "Narrow"
            HighlighterSize.MEDIUM -> "Medium"
            HighlighterSize.WIDE -> "Wide"
        }
    },
    eraserSizeLabel = { sz ->
        when (sz) {
            EraserSize.SMALL -> "S"
            EraserSize.MEDIUM -> "M"
            EraserSize.LARGE -> "L"
        }
    }
)

val EsStrings = LeNoteStrings(
    language = ResolvedLanguage.ES,
    languagePickerOverline = "IDIOMA DE LA INTERFAZ",
    languagePickerTitle = "Seleccionar Idioma",
    languageSystemOption = "Automático (Sistema Android)",
    languageSystemSubtitle = { resolved -> "Siguiendo el idioma del dispositivo (${resolved.nativeName})" },
    languagePortuguese = "Português",
    languageEnglish = "English",
    languageSpanish = "Español",

    homeOverline = "ATELIER DE NOTAS",
    homeTitle = "Mis Cuadernos",
    homeStatsSummary = { nb, pg -> "$nb cuadernos  •  $pg hojas" },
    actionPdf = "PDF",
    actionBackup = "Copia",
    lightModeDesc = "Modo Claro",
    darkModeDesc = "Modo Nocturno",
    languageButtonDesc = "Cambiar Idioma",
    searchNotebooksPlaceholder = "Buscar cuadernos, notas o temas...",
    tabLibrary = "Biblioteca",
    tabFavorites = "Favoritas",
    tabTrash = "Papelera",
    fabNewNotebook = "Nuevo Cuaderno",
    fabEmptyTrash = "Vaciar Papelera",

    sectionContinueWriting = "Continuar Escribiendo",
    sectionContinueWritingSubtitle = "Hojas recientes",
    sectionNotebookCollection = "Colección de Cuadernos",
    sectionMainShelfSubtitle = "Estantería principal",
    sectionActiveFilterSubtitle = "Filtro activo",
    cardNotebookOverline = "CUADERNO",
    cardNotebookStats = { sec, pg -> "$sec secc.  •  $pg pág." },
    menuNotebookOptionsDesc = "Opciones del cuaderno",
    menuExportLenote = "Exportar (.lenote)",
    menuMoveToTrash = "Mover a la Papelera",
    openNotebookDesc = "Abrir cuaderno",

    emptySearchOverline = "Sin resultados",
    emptySearchTitle = { q -> "Ningún cuaderno para \"$q\"" },
    emptySearchDescription = "Verifica la ortografía o limpia el filtro de búsqueda para ver toda tu colección.",
    actionClearSearch = "Limpiar Búsqueda",
    emptyLibraryOverline = "Archivo Vacío",
    emptyLibraryTitle = "Comienza tu primer cuaderno",
    emptyLibraryDescription = "Crea un cuaderno en blanco para notas manuscritas y bocetos o importa un PDF para anotar.",
    actionCreateFirstNotebook = "Crear Primer Cuaderno",
    emptyFavoritesOverline = "Colección Destacada",
    emptyFavoritesTitle = "Ninguna hoja favorita",
    emptyFavoritesDescription = "Toca el icono de estrella en el lienzo o en la barra lateral para fijar tus hojas más importantes aquí.",
    sectionFavoritePages = "Hojas Favoritas",
    sectionFavoritePagesSubtitle = "Acceso rápido",
    emptyTrashOverline = "Archivo de Eliminación",
    emptyTrashTitle = "La papelera está vacía",
    emptyTrashDescription = "Los cuadernos y hojas eliminados se conservan aquí temporalmente antes de borrarse definitivamente.",
    sectionDeletedNotebooks = "Cuadernos en la Papelera",
    sectionDeletedPages = "Hojas Sueltas en la Papelera",
    deletedOnDate = { d -> "Eliminado el $d" },
    unknownDate = "Fecha desconocida",
    actionRestore = "Restaurar",
    deletePermanentlyDesc = "Eliminar definitivamente",

    bannerImportingPdfTitle = "Importando documento PDF...",
    bannerImportingPdfMessage = { name -> "Renderizando páginas de '$name' en tu cuaderno." },
    bannerPdfImportedTitle = "PDF importado con éxito",
    bannerPdfImportedMessage = { title -> "Cuaderno '$title' listo para anotar." },
    bannerPdfImportFailedTitle = "Error al importar PDF",
    bannerPdfImportFailedDefaultMsg = "Verifica que el archivo PDF sea válido.",
    bannerImportingLenoteTitle = "Importando paquete .lenote...",
    bannerImportingLenoteMessage = "Restaurando secciones, hojas y trazos vectoriales.",
    bannerLenoteImportedTitle = "Cuaderno importado",
    bannerLenoteImportedMessage = { title -> "'$title' fue añadido a la biblioteca." },
    bannerLenoteInvalidTitle = "Paquete .lenote inválido",
    bannerLenoteInvalidMessage = "No se pudo leer la estructura del archivo seleccionado.",
    bannerImportErrorTitle = "Error de importación",
    bannerImportErrorDefaultMsg = "Error al leer el paquete.",
    bannerExportingNotebookTitle = { title -> "Exportando '$title'..." },
    bannerExportingNotebookMessage = "Comprimiendo trazos, medios y metadatos.",
    bannerExportSuccessTitle = "Copia .lenote completada",
    bannerExportSuccessMessage = { title -> "El cuaderno '$title' se guardó correctamente." },
    bannerExportWriteErrorTitle = "Error al guardar archivo",
    bannerExportFailedTitle = "Error en la exportación",
    bannerExportFailedMessage = "No se pudo generar el paquete del cuaderno.",
    bannerMovedToTrashTitle = "Movido a la papelera",
    bannerMovedToTrashMessage = { title -> "'$title' se puede restaurar en la pestaña Papelera." },
    bannerNotebookRestoredTitle = "Cuaderno restaurado",
    bannerNotebookRestoredMessage = { title -> "'$title' volvió a tu biblioteca." },
    bannerPageRestoredTitle = "Hoja restaurada",
    bannerPageRestoredMessage = { title -> "'$title' fue restaurada al cuaderno." },
    bannerNotebookCreatedTitle = "Cuaderno creado",
    bannerNotebookCreatedMessage = { title -> "Abriendo '$title'..." },
    bannerTrashEmptiedTitle = "Papelera vaciada",
    bannerTrashEmptiedMessage = "Todos los elementos fueron eliminados permanentemente.",

    createNotebookOverline = "NUEVO VOLUMEN",
    createNotebookTitle = "Crear Cuaderno",
    notebookTitleLabel = "Título del Cuaderno",
    notebookTitlePlaceholder = "Ej: Arquitectura, Notas de Campo...",
    notebookBindingColorLabel = "TONALIDAD DE LA ENCUADERNACIÓN",
    defaultNewNotebookName = "Nuevo Cuaderno",
    actionCreateNotebook = "Crear Cuaderno",
    actionCancel = "Cancelar",
    actionConfirm = "Confirmar",
    actionClose = "Cerrar",
    actionSave = "Guardar",
    actionDelete = "Eliminar",
    emptyTrashDialogTitle = "¿Vaciar Papelera?",
    emptyTrashDialogMessage = "Todos los cuadernos, secciones, hojas y medios en la papelera se eliminarán definitivamente del dispositivo. Esta acción no se puede deshacer.",
    actionEmptyPermanently = "Vaciar Definitivamente",
    deleteNotebookDialogTitle = "¿Eliminar Cuaderno Definitivamente?",
    deleteNotebookDialogMessage = { title -> "El cuaderno '$title' y todas sus hojas se eliminarán permanentemente del almacenamiento." },
    deletePageDialogTitle = "¿Eliminar Hoja Definitivamente?",
    deletePageDialogMessage = { title -> "La hoja '$title' se borrará permanentemente." },

    backToLibraryDesc = "Volver a la Biblioteca",
    notebookIndexDesc = "Índice del Cuaderno",
    indexDefaultLabel = "Índice",
    defaultPageTitle = { idx -> "Página $idx" },
    renamePageDesc = "Renombrar hoja",
    favoritePageDesc = "Marcar como favorita",
    unfavoritePageDesc = "Quitar de favoritas",
    statusSaving = "Guardando...",
    statusSaved = "Guardado",
    sheetTemplateAndColorDesc = "Plantilla y Color de Hoja",
    morePageOptionsDesc = "Más opciones de la hoja",
    toolPen = "Pluma",
    toolHighlighter = "Resaltador",
    toolEraser = "Borrador",
    toolLasso = "Lazo de Selección",
    toolTextBox = "Insertar Cuadro de Texto",
    toolInsertImage = "Insertar Imagen",
    toolTouchInking = "Escritura con el Dedo",
    toolUndo = "Deshacer",
    toolRedo = "Rehacer",
    lassoSelectedCount = { c -> "$c sel.:" },
    lassoBeautifyDesc = "Embellecer Caligrafía",
    lassoAlignBaselineDesc = "Alinear a la Pauta",
    lassoDuplicateDesc = "Duplicar",
    lassoDeleteSelectionDesc = "Eliminar trazos seleccionados",
    lassoClearSelectionDesc = "Deseleccionar",
    lassoHint = "Rodea los trazos para seleccionar, mover o refinar la caligrafía",

    currentSheetOverline = "HOJA ACTUAL",
    renamePageTitle = "Renombrar Hoja",
    pageTitlePlaceholder = "Título de la hoja",
    clearStrokesDialogTitle = "¿Borrar todos los trazos?",
    clearStrokesDialogMessage = "Se borrarán todos los trazos manuscritos de esta hoja. Puedes revertirlo inmediatamente con Deshacer.",
    actionClearSheet = "Limpiar Hoja",
    movePageToTrashDialogTitle = "¿Mover hoja a la papelera?",
    movePageToTrashDialogMessage = "Esta hoja se moverá a la papelera de la biblioteca y podrá restaurarse si es necesario.",
    sheetOptionsTitle = "Opciones de la Hoja",
    strokeSmoothingLabel = "SUAVIZADO DEL TRAZO",
    pageOptionsOverline = "OPCIONES DE HOJA",
    currentSheetFallback = "Hoja Actual",
    sectionOrganization = "ORGANIZACIÓN",
    actionRenameSheet = "Renombrar hoja",
    actionDuplicateSheet = "Duplicar hoja",
    actionDuplicateSheetSubtitle = "Crea una copia idéntica en el cuaderno",
    actionMoveUp = "Mover arriba",
    actionMoveDown = "Mover abajo",
    actionAddFavorite = "Marcar como favorita",
    actionRemoveFavorite = "Quitar de favoritas",
    sectionPaperAndRuling = "PAPEL Y PAUTA",
    actionSheetTemplateTitle = "Plantilla y color del papel",
    actionSheetTemplateSubtitle = "Cornell, rayado, punteado, pentagrama y tonos de papel",
    sectionExportAndShare = "EXPORTAR Y COMPARTIR",
    actionExportPdfTitle = "Exportar como PDF",
    actionExportPdfSubtitle = "Documento vectorial listo para imprimir",
    actionExportPngTitle = "Exportar como Imagen (PNG)",
    actionExportPngSubtitle = "Imagen de alta resolución de la hoja",
    sectionEngineering = "INGENIERÍA",
    actionTelemetryTitle = "Telemetría Jetpack Ink",
    telemetryVisibleSubtitle = "Visible en el lienzo",
    telemetryHiddenSubtitle = "Oculto",
    sectionDangerZone = "ZONA DE ELIMINACIÓN",
    actionClearAllStrokesTitle = "Borrar todos los trazos",
    actionClearAllStrokesSubtitle = "Borra el contenido manteniendo la hoja",
    actionMoveSheetToTrashTitle = "Mover hoja a la papelera",
    actionMoveSheetToTrashSubtitle = "Elimina esta hoja del cuaderno",

    exportPdfSuccessTitle = "PDF exportado con éxito",
    exportPdfErrorTitle = "No se pudo exportar el PDF.",
    exportPngSuccessTitle = "Imagen exportada con éxito",
    exportPngErrorTitle = "No se pudo exportar la imagen.",
    exportOpenPdfButton = "Abrir PDF",
    exportViewImageButton = "Ver imagen",
    exportNoAppFoundError = { type -> "No se encontró una aplicación para abrir $type." },
    exportOpenFailedError = { type -> "$type guardado con éxito, pero no se pudo abrir el archivo." },
    zoomOutDesc = "Alejar zoom",
    zoomInDesc = "Acercar zoom",
    zoomFitToPageDesc = "Ajustar a la página",
    zoomCloseDesc = "Cerrar control de zoom",

    sidebarOverline = "ÍNDICE Y SUMARIO",
    sidebarTitle = "Cuadernos y Hojas",
    sidebarSearchPlaceholder = "Buscar hojas o texto manuscrito...",
    sidebarSearchResultsHeader = { c -> "RESULTADOS ($c)" },
    sidebarSearchEmptyOverline = "Búsqueda en Cuaderno",
    sidebarSearchEmptyTitle = "No se encontraron hojas",
    sidebarSearchEmptyDesc = { q -> "Ningún título o texto manuscrito coincide con \"$q\"." },
    sidebarNotebooksHeader = { c -> "CUADERNOS ($c)" },
    sidebarSectionsHeader = { c -> "SECCIONES ($c)" },
    sidebarPagesHeader = { c -> "HOJAS ($c)" },
    sidebarFavoritesHeader = { c -> "FAVORITAS ($c)" },
    sidebarFilterAll = "Todas",
    sidebarFilterStars = "Estrellas",
    sidebarNewSheetButton = "Nueva Hoja",
    sidebarTrashButton = { c -> "Papelera" },
    sidebarTrashDialogTitle = "Papelera de la Aplicación",
    sidebarEmptyAction = "Vaciar",
    sidebarRenameNotebookTitle = "Renombrar Cuaderno",
    sidebarNewSectionTitle = "Nueva Sección",
    sidebarRenameSectionTitle = "Renombrar Sección",
    sidebarEnterNamePlaceholder = "Escribe el nombre...",

    templateNewSheetOverline = "NUEVA HOJA DE CUADERNO",
    templateAttributesOverline = "ATRIBUTOS DEL PAPEL",
    templateNewSheetTitle = "Elegir Pauta y Papel",
    templateAttributesTitle = "Plantilla y Color de Hoja",
    templateSheetTitleLabel = "Título de la Hoja",
    templateSheetTitlePlaceholder = "Ej: Notas de Lectura, Boceto...",
    templatePaperStockHeader = "GRAMAJE Y TONALIDAD DEL PAPEL",
    templateRulingHeader = "PAUTA Y GEOMETRÍA DE LA HOJA",
    actionCreateSheet = "Crear Hoja",
    actionApplyToSheet = "Aplicar a la Hoja",

    pageBackgroundLabel = { bg ->
        when (bg) {
            PageBackground.BLANK -> "Blanca"
            PageBackground.RULED -> "Rayada"
            PageBackground.GRID -> "Cuadriculada"
            PageBackground.DOTS -> "Punteada"
            PageBackground.MATHEMATICS -> "Matemáticas"
            PageBackground.CORNELL -> "Cornell"
            PageBackground.WEEKLY_PLANNER -> "Planificador"
            PageBackground.ENGINEERING_GRAPH -> "Ingeniería"
            PageBackground.MUSIC_STAFF -> "Pentagrama"
        }
    },
    paperColorLabel = { colorLong ->
        when (colorLong) {
            PaperColors.CLASSIC_WHITE -> "Blanco"
            PaperColors.LEGAL_YELLOW -> "Amarillo"
            PaperColors.SEPIA_CREAM -> "Sepia Suave"
            PaperColors.OLED_BLACK -> "Grafito"
            else -> "Blanco"
        }
    },
    penStrokeSizeLabel = { sz ->
        when (sz) {
            PenStrokeSize.FINE -> "Fina"
            PenStrokeSize.MEDIUM -> "Media"
            PenStrokeSize.THICK -> "Gruesa"
            PenStrokeSize.EXTRA_THICK -> "Extra"
        }
    },
    highlighterSizeLabel = { sz ->
        when (sz) {
            HighlighterSize.NARROW -> "Fino"
            HighlighterSize.MEDIUM -> "Medio"
            HighlighterSize.WIDE -> "Ancho"
        }
    },
    eraserSizeLabel = { sz ->
        when (sz) {
            EraserSize.SMALL -> "P"
            EraserSize.MEDIUM -> "M"
            EraserSize.LARGE -> "G"
        }
    }
)

fun stringsForLanguage(resolvedLanguage: ResolvedLanguage): LeNoteStrings {
    return when (resolvedLanguage) {
        ResolvedLanguage.PT -> PtStrings
        ResolvedLanguage.EN -> EnStrings
        ResolvedLanguage.ES -> EsStrings
    }
}

val LocalLeNoteStrings = staticCompositionLocalOf { PtStrings }

val currentStrings: LeNoteStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalLeNoteStrings.current
