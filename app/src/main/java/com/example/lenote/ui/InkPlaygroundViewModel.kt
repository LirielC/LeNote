package com.example.lenote.ui

import android.content.Context
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lenote.data.AppPreferences
import com.example.lenote.data.HandwritingSearchResult
import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.data.RecognizedWordBox
import com.example.lenote.data.SectionEntity
import com.example.lenote.data.ThemeMode
import com.example.lenote.data.ToolbarDockEdge
import com.example.lenote.data.WordBoxJsonSerializer
import com.example.lenote.export.ExportResult
import com.example.lenote.export.PageExporter
import com.example.lenote.ink.DigitalInkManager
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ink.PageBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel responsável por manter:
 * 1. A instância do [JetpackInkEngine] e todo o seu estado (strokes finalizados, Undo/Redo,
 *    ferramenta, cor, espessura, suavização, laço e câmera);
 * 2. A hierarquia persistente de Cadernos (`Notebook`), Seções (`Section`) e Páginas (`Page`)
 *    via [NoteRepository] (SQLite + arquivos atômicos `.ink` + miniaturas `.png`);
 * 3. O Auto-Save assíncrono com debounce de `400ms` em `Dispatchers.IO` e flush síncrono ao
 *    trocar de página ou pausar o app;
 * 4. Preferências de usuário (Dock da Toolbar, Dark Mode, Visualização Grid/Lista);
 * 5. Indexação e busca de escrita manual (Digital Ink Recognition + SQLite FTS5);
 * 6. Exportação para PDF, PNG e pacotes de backup `.lenote`.
 */
class InkPlaygroundViewModel : ViewModel() {

    val engine: JetpackInkEngine = JetpackInkEngine()

    private var appContext: Context? = null
    private var repository: NoteRepository? = null
    private var isInitialized: Boolean = false

    private val _savedStrokesData = MutableStateFlow<ByteArray?>(null)
    val savedStrokesData: StateFlow<ByteArray?> = _savedStrokesData.asStateFlow()

    private val _saveStatusMessage = MutableStateFlow<String?>(null)
    val saveStatusMessage: StateFlow<String?> = _saveStatusMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Estado da hierarquia de navegação (Cadernos -> Seções -> Páginas)
    private val _notebooks = MutableStateFlow<List<NotebookEntity>>(emptyList())
    val notebooks: StateFlow<List<NotebookEntity>> = _notebooks.asStateFlow()

    private val _sections = MutableStateFlow<List<SectionEntity>>(emptyList())
    val sections: StateFlow<List<SectionEntity>> = _sections.asStateFlow()

    private val _pages = MutableStateFlow<List<PageEntity>>(emptyList())
    val pages: StateFlow<List<PageEntity>> = _pages.asStateFlow()

    private val _allPages = MutableStateFlow<List<PageEntity>>(emptyList())
    val allPages: StateFlow<List<PageEntity>> = _allPages.asStateFlow()

    private val _favoritePages = MutableStateFlow<List<PageEntity>>(emptyList())
    val favoritePages: StateFlow<List<PageEntity>> = _favoritePages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<PageEntity>>(emptyList())
    val searchResults: StateFlow<List<PageEntity>> = _searchResults.asStateFlow()

    private val _handwritingSearchResults = MutableStateFlow<List<HandwritingSearchResult>>(emptyList())
    val handwritingSearchResults: StateFlow<List<HandwritingSearchResult>> = _handwritingSearchResults.asStateFlow()

    private val _currentSearchHighlights = MutableStateFlow<List<RectF>>(emptyList())
    val currentSearchHighlights: StateFlow<List<RectF>> = _currentSearchHighlights.asStateFlow()

    private val _selectedNotebookId = MutableStateFlow<String?>(null)
    val selectedNotebookId: StateFlow<String?> = _selectedNotebookId.asStateFlow()

    private val _selectedSectionId = MutableStateFlow<String?>(null)
    val selectedSectionId: StateFlow<String?> = _selectedSectionId.asStateFlow()

    private val _currentPage = MutableStateFlow<PageEntity?>(null)
    val currentPage: StateFlow<PageEntity?> = _currentPage.asStateFlow()

    private val _textBlocks = MutableStateFlow<List<CanvasTextBlock>>(emptyList())
    val textBlocks: StateFlow<List<CanvasTextBlock>> = _textBlocks.asStateFlow()

    private val _imageBlocks = MutableStateFlow<List<CanvasImageBlock>>(emptyList())
    val imageBlocks: StateFlow<List<CanvasImageBlock>> = _imageBlocks.asStateFlow()

    private val _isTextToolActive = MutableStateFlow(false)
    val isTextToolActive: StateFlow<Boolean> = _isTextToolActive.asStateFlow()

    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen.asStateFlow()

    private val _isDiagnosticsVisible = MutableStateFlow(false)
    val isDiagnosticsVisible: StateFlow<Boolean> = _isDiagnosticsVisible.asStateFlow()

    private val _thumbnailVersion = MutableStateFlow(0L)
    val thumbnailVersion: StateFlow<Long> = _thumbnailVersion.asStateFlow()

    // Preferências UI/UX
    private var appPreferences: AppPreferences? = null

    private val _toolbarDockEdge = MutableStateFlow(ToolbarDockEdge.TOP)
    val toolbarDockEdge: StateFlow<ToolbarDockEdge> = _toolbarDockEdge.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isSidebarGridView = MutableStateFlow(false)
    val isSidebarGridView: StateFlow<Boolean> = _isSidebarGridView.asStateFlow()

    private var digitalInkManager: DigitalInkManager? = null
    private var handwritingOcrJob: Job? = null

    private var sectionsObserverJob: Job? = null
    private var pagesObserverJob: Job? = null
    private var searchObserverJob: Job? = null
    private var autoSaveDebounceJob: Job? = null
    private var suppressAutoSaveUntilContentVersion: Long = -1L

    fun initializeIfNeeded(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appCtx = context.applicationContext
        appContext = appCtx

        val prefs = AppPreferences(appCtx)
        appPreferences = prefs
        _toolbarDockEdge.value = prefs.toolbarDockEdge
        _themeMode.value = prefs.themeMode
        _isSidebarGridView.value = prefs.isSidebarGridView

        val inkMgr = DigitalInkManager(appCtx)
        digitalInkManager = inkMgr
        viewModelScope.launch {
            inkMgr.ensureModelDownloaded()
        }

        val db = LeNoteDatabase.getInstance(appCtx)
        val storage = PageStrokeStorage(appCtx.filesDir)
        val repo = NoteRepository(db, storage)
        repository = repo

        val (defaultNb, defaultSec, defaultPage) = repo.ensureDefaultHierarchy()
        _selectedNotebookId.value = defaultNb.id
        _selectedSectionId.value = defaultSec.id
        loadPageIntoEngine(defaultPage)

        // Observa a lista de cadernos
        viewModelScope.launch {
            repo.observeNotebooks().collectLatest { list ->
                _notebooks.value = list
            }
        }

        // Observa páginas favoritas
        viewModelScope.launch {
            repo.observeFavoritePages().collectLatest { favs ->
                _favoritePages.value = favs
            }
        }

        // Observa todas as páginas para a tela inicial / recentes
        viewModelScope.launch {
            repo.observeAllPages().collectLatest { all ->
                _allPages.value = all
            }
        }

        observeSectionsForNotebook(defaultNb.id)
        observePagesForSection(defaultSec.id)

        // Observa alterações de conteúdo no motor (contentVersion) para disparar Auto-Save (debounce 400ms)
        viewModelScope.launch {
            engine.state
                .map { it.contentVersion }
                .distinctUntilChanged()
                .collectLatest { version ->
                    if (version <= suppressAutoSaveUntilContentVersion) {
                        return@collectLatest
                    }
                    scheduleAutoSave()
                }
        }
    }

    private fun observeSectionsForNotebook(notebookId: String) {
        val repo = repository ?: return
        sectionsObserverJob?.cancel()
        sectionsObserverJob = viewModelScope.launch {
            repo.observeSections(notebookId).collectLatest { list ->
                _sections.value = list
            }
        }
    }

    private fun observePagesForSection(sectionId: String) {
        val repo = repository ?: return
        pagesObserverJob?.cancel()
        pagesObserverJob = viewModelScope.launch {
            repo.observePages(sectionId).collectLatest { list ->
                _pages.value = list
                val curId = _currentPage.value?.id
                if (curId != null) {
                    list.find { it.id == curId }?.let { _currentPage.value = it }
                }
            }
        }
    }

    private fun loadPageIntoEngine(page: PageEntity) {
        engine.clearSearchHighlights()
        _currentSearchHighlights.value = emptyList()
        val repo = repository ?: return
        val bytes = repo.loadPageStrokes(page.id)
        engine.setPageBackground(page.pageBackground)

        // Se houver PDF associado, renderiza o bitmap de alta resolução em segundo plano
        if (!page.pdfPath.isNullOrBlank() && page.pdfPageIndex != null) {
            val cachedBmp = com.example.lenote.pdf.PdfPageRenderer.getCachedBitmap(page.pdfPath, page.pdfPageIndex, 2.0f)
            if (cachedBmp != null) {
                engine.setPdfBackgroundBitmap(cachedBmp)
            } else {
                engine.setPdfBackgroundBitmap(null)
                viewModelScope.launch {
                    val bmp = com.example.lenote.pdf.PdfPageRenderer.renderPage(page.pdfPath, page.pdfPageIndex, 2.0f)
                    if (_currentPage.value?.id == page.id) {
                        engine.setPdfBackgroundBitmap(bmp)
                    }
                }
            }
        } else {
            engine.setPdfBackgroundBitmap(null)
        }

        if (bytes.isNotEmpty()) {
            engine.loadStrokes(bytes)
        } else {
            engine.clear()
        }
        engine.setCameraTransform(page.zoom, page.panX, page.panY)
        suppressAutoSaveUntilContentVersion = engine.state.value.contentVersion
        _currentPage.value = page
        _savedStrokesData.value = if (bytes.isNotEmpty()) bytes else null

        // Carrega blocos de mídia da página
        _textBlocks.value = repo.getTextBlocks(page.id)
        _imageBlocks.value = repo.getImageBlocks(page.id)
    }

    private fun scheduleAutoSave() {
        _isSaving.value = true
        autoSaveDebounceJob?.cancel()
        autoSaveDebounceJob = viewModelScope.launch {
            delay(400L)
            persistCurrentPageAsync()
        }
        scheduleHandwritingIndexing()
    }

    private fun scheduleHandwritingIndexing() {
        handwritingOcrJob?.cancel()
        handwritingOcrJob = viewModelScope.launch {
            delay(3000L) // Debounce de 3 segundos após a última edição
            indexCurrentPageHandwriting()
        }
    }

    private suspend fun indexCurrentPageHandwriting() {
        val page = _currentPage.value ?: return
        val inkMgr = digitalInkManager ?: return
        val repo = repository ?: return
        val strokes = engine.getFinishedStrokesSnapshot()
        if (strokes.isEmpty()) {
            withContext(Dispatchers.IO) {
                repo.indexPageHandwriting(page.id, "", "")
            }
            return
        }
        val ocrResult = inkMgr.recognizeStrokes(strokes).getOrNull() ?: return
        val boxesJson = WordBoxJsonSerializer.serialize(ocrResult.wordBoxes)
        withContext(Dispatchers.IO) {
            repo.indexPageHandwriting(page.id, ocrResult.fullText, boxesJson)
        }
    }

    private suspend fun persistCurrentPageAsync() {
        val repo = repository ?: run {
            _isSaving.value = false
            return
        }
        val page = _currentPage.value ?: run {
            _isSaving.value = false
            return
        }
        val engineState = engine.state.value
        val strokesSnapshot = engine.getFinishedStrokesSnapshot()
        val strokeBytes = engine.saveStrokes()

        withContext(Dispatchers.IO) {
            val thumbBmp = runCatching {
                PageExporter.generateThumbnailBitmap(engineState.pageBackground, strokesSnapshot)
            }.getOrNull()

            repo.savePageContent(
                pageId = page.id,
                strokesBytes = strokeBytes,
                pageBackground = engineState.pageBackground,
                zoom = engineState.zoomLevel,
                panX = engineState.panOffsetX,
                panY = engineState.panOffsetY,
                thumbnailBitmap = thumbBmp
            )
            thumbBmp?.recycle()
        }

        _savedStrokesData.value = strokeBytes
        _thumbnailVersion.value++
        _isSaving.value = false
        _saveStatusMessage.value = "Salvo"
    }

    /**
     * Força a gravação síncrona/imediata da página atual (chamada antes de trocar de página ou sair).
     */
    fun flushCurrentPageSave() {
        autoSaveDebounceJob?.cancel()
        autoSaveDebounceJob = null
        val repo = repository ?: return
        val page = _currentPage.value ?: return
        val engineState = engine.state.value
        val strokesSnapshot = engine.getFinishedStrokesSnapshot()
        val strokeBytes = engine.saveStrokes()

        val thumbBmp = runCatching {
            PageExporter.generateThumbnailBitmap(engineState.pageBackground, strokesSnapshot)
        }.getOrNull()

        repo.savePageContent(
            pageId = page.id,
            strokesBytes = strokeBytes,
            pageBackground = engineState.pageBackground,
            zoom = engineState.zoomLevel,
            panX = engineState.panOffsetX,
            panY = engineState.panOffsetY,
            thumbnailBitmap = thumbBmp
        )
        thumbBmp?.recycle()

        _savedStrokesData.value = strokeBytes
        _thumbnailVersion.value++
        _isSaving.value = false
        _saveStatusMessage.value = "Salvo"
    }

    // --- Navegação e Operações de Caderno / Seção / Página ---

    fun toggleSidebar() {
        _isSidebarOpen.value = !_isSidebarOpen.value
    }

    fun toggleDiagnostics() {
        _isDiagnosticsVisible.value = !_isDiagnosticsVisible.value
    }

    fun toggleDiagnosticsOverlay() {
        toggleDiagnostics()
    }

    fun setSidebarOpen(open: Boolean) {
        _isSidebarOpen.value = open
    }

    fun duplicateCurrentPage() {
        val curId = _currentPage.value?.id ?: return
        duplicatePage(curId)
    }

    fun getNotebookSectionCount(notebookId: String): Int {
        val repo = repository ?: return 0
        return repo.database.getSectionsByNotebook(notebookId).size
    }

    fun getNotebookPageCount(notebookId: String): Int {
        val repo = repository ?: return 0
        val secs = repo.database.getSectionsByNotebook(notebookId)
        return secs.sumOf { repo.database.getPagesBySection(it.id).size }
    }


    // --- Métodos de Manipulação de Blocos de Mídia (Texto & Imagem) ---

    fun setTextToolActive(active: Boolean) {
        _isTextToolActive.value = active
    }

    fun addTextBlock(worldX: Float, worldY: Float, initialText: String = "") {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        val block = CanvasTextBlock(
            id = java.util.UUID.randomUUID().toString(),
            pageId = curPage.id,
            text = initialText,
            x = worldX,
            y = worldY,
            width = 240f,
            height = 60f,
            textColor = 0xFF0F172A.toInt(),
            fontSize = 16f,
            zIndex = (_textBlocks.value.maxOfOrNull { it.zIndex } ?: 0f) + 1f
        )
        repo.upsertTextBlock(block)
        _textBlocks.value = repo.getTextBlocks(curPage.id)
    }

    fun updateTextBlock(block: CanvasTextBlock) {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        repo.upsertTextBlock(block)
        _textBlocks.value = repo.getTextBlocks(curPage.id)
    }

    fun deleteTextBlock(blockId: String) {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        repo.deleteTextBlock(blockId)
        _textBlocks.value = repo.getTextBlocks(curPage.id)
    }

    fun addImageBlock(imageFile: File, worldX: Float = 100f, worldY: Float = 100f) {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        val block = CanvasImageBlock(
            id = java.util.UUID.randomUUID().toString(),
            pageId = curPage.id,
            imagePath = imageFile.absolutePath,
            x = worldX,
            y = worldY,
            width = 300f,
            height = 300f,
            rotationDegrees = 0f,
            zIndex = (_imageBlocks.value.maxOfOrNull { it.zIndex } ?: 0f) + 1f
        )
        repo.upsertImageBlock(block)
        _imageBlocks.value = repo.getImageBlocks(curPage.id)
    }

    fun updateImageBlock(block: CanvasImageBlock) {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        repo.upsertImageBlock(block)
        _imageBlocks.value = repo.getImageBlocks(curPage.id)
    }

    fun deleteImageBlock(blockId: String) {
        val curPage = _currentPage.value ?: return
        val repo = repository ?: return
        repo.deleteImageBlock(blockId)
        _imageBlocks.value = repo.getImageBlocks(curPage.id)
    }

    fun importPdfDocument(title: String, pdfFile: File, onComplete: ((NotebookEntity) -> Unit)? = null) {
        val repo = repository ?: return
        flushCurrentPageSave()
        viewModelScope.launch {
            _saveStatusMessage.value = "Importando PDF..."
            val (notebook, section, pages) = withContext(Dispatchers.IO) {
                val pageCount = com.example.lenote.pdf.PdfPageRenderer.getPageCount(pdfFile)
                repo.importPdfDocument(title = title, pdfFile = pdfFile, pageCount = pageCount)
            }
            _selectedNotebookId.value = notebook.id
            _selectedSectionId.value = section.id
            observeSectionsForNotebook(notebook.id)
            observePagesForSection(section.id)
            val firstPage = pages.firstOrNull()
            if (firstPage != null) {
                loadPageIntoEngine(firstPage)
            }
            _saveStatusMessage.value = "PDF importado com sucesso (${pages.size} páginas)"
            onComplete?.invoke(notebook)
        }
    }

    fun openNotebook(notebookId: String) {
        selectNotebook(notebookId)
    }

    fun selectNotebook(notebookId: String) {
        if (_selectedNotebookId.value == notebookId) return
        flushCurrentPageSave()
        _selectedNotebookId.value = notebookId
        observeSectionsForNotebook(notebookId)

        val repo = repository ?: return
        val secs = repo.database.getSectionsByNotebook(notebookId)
        val firstSec = secs.firstOrNull() ?: repo.createSection(notebookId, "Geral")
        selectSection(firstSec.id)
    }

    fun selectSection(sectionId: String) {
        if (_selectedSectionId.value == sectionId) return
        flushCurrentPageSave()
        _selectedSectionId.value = sectionId
        observePagesForSection(sectionId)

        val repo = repository ?: return
        val secPages = repo.database.getPagesBySection(sectionId)
        val firstPage = secPages.firstOrNull() ?: repo.createPage(sectionId, "Página 1", PageBackground.RULED)
        loadPageIntoEngine(firstPage)
    }

    fun selectPage(page: PageEntity) {
        if (_currentPage.value?.id == page.id) return
        flushCurrentPageSave()
        val repo = repository
        val sec = repo?.database?.getSectionById(page.sectionId)
        if (sec != null && _selectedNotebookId.value != sec.notebookId) {
            _selectedNotebookId.value = sec.notebookId
            observeSectionsForNotebook(sec.notebookId)
        }
        if (_selectedSectionId.value != page.sectionId) {
            _selectedSectionId.value = page.sectionId
            observePagesForSection(page.sectionId)
        }
        val freshPage = repo?.database?.getPageById(page.id) ?: page
        loadPageIntoEngine(freshPage)
    }

    fun createNewNotebook(title: String, coverColorArgb: Int = 0xFF1565C0.toInt()) {
        val repo = repository ?: return
        flushCurrentPageSave()
        val nb = repo.createNotebook(title.ifBlank { "Novo Caderno" }, coverColorArgb)
        val sec = repo.createSection(nb.id, "Geral")
        val page = repo.createPage(sec.id, "Página 1", PageBackground.RULED)
        _selectedNotebookId.value = nb.id
        _selectedSectionId.value = sec.id
        observeSectionsForNotebook(nb.id)
        observePagesForSection(sec.id)
        loadPageIntoEngine(page)
    }

    fun renameNotebook(notebookId: String, newTitle: String) {
        repository?.renameNotebook(notebookId, newTitle.ifBlank { "Caderno" })
    }

    fun deleteNotebook(notebookId: String) {
        val repo = repository ?: return
        if (_notebooks.value.size <= 1) {
            _saveStatusMessage.value = "Mantenha ao menos 1 caderno ativo"
            return
        }
        repo.deleteNotebook(notebookId)
        val remaining = repo.database.getAllNotebooks().firstOrNull()
        if (remaining != null) {
            selectNotebook(remaining.id)
        }
    }

    fun createNewSection(title: String) {
        val repo = repository ?: return
        val nbId = _selectedNotebookId.value ?: return
        flushCurrentPageSave()
        val sec = repo.createSection(nbId, title.ifBlank { "Nova Seção" })
        val page = repo.createPage(sec.id, "Página 1", PageBackground.RULED)
        _selectedSectionId.value = sec.id
        observePagesForSection(sec.id)
        loadPageIntoEngine(page)
    }

    fun renameSection(sectionId: String, newTitle: String) {
        repository?.renameSection(sectionId, newTitle.ifBlank { "Seção" })
    }

    fun deleteSection(sectionId: String) {
        val repo = repository ?: return
        val nbId = _selectedNotebookId.value ?: return
        if (_sections.value.size <= 1) {
            _saveStatusMessage.value = "Mantenha ao menos 1 seção no caderno"
            return
        }
        repo.deleteSection(sectionId)
        val remaining = repo.database.getSectionsByNotebook(nbId).firstOrNull()
        if (remaining != null) {
            selectSection(remaining.id)
        }
    }

    fun createNewPage(title: String? = null) {
        val repo = repository ?: return
        val secId = _selectedSectionId.value ?: return
        flushCurrentPageSave()
        val page = repo.createPage(
            sectionId = secId,
            title = title?.ifBlank { null } ?: "Página ${_pages.value.size + 1}",
            pageBackground = engine.state.value.pageBackground
        )
        loadPageIntoEngine(page)
    }

    fun renameCurrentPage(newTitle: String) {
        val page = _currentPage.value ?: return
        val repo = repository ?: return
        repo.renamePage(page.id, newTitle)
        _currentPage.value = page.copy(title = newTitle.trim().ifEmpty { page.title })
    }

    fun renamePage(pageId: String, newTitle: String) {
        repository?.renamePage(pageId, newTitle)
        if (_currentPage.value?.id == pageId) {
            _currentPage.value = _currentPage.value?.copy(title = newTitle.trim().ifEmpty { "Página" })
        }
    }

    fun duplicatePage(pageId: String) {
        val repo = repository ?: return
        flushCurrentPageSave()
        val duplicated = repo.duplicatePage(pageId)
        if (duplicated != null) {
            loadPageIntoEngine(duplicated)
        }
    }

    fun changePageBackground(background: PageBackground) {
        engine.setPageBackground(background)
        _currentPage.value?.let { page ->
            _currentPage.value = page.copy(pageBackground = background)
        }
        scheduleAutoSave()
    }

    fun zoomIn() {
        engine.zoomIn()
    }

    fun zoomOut() {
        engine.zoomOut()
    }

    fun resetZoom() {
        engine.resetZoom()
    }

    fun fitToPage() {
        engine.fitToPage()
    }

    fun toggleCurrentPageFavorite() {
        val page = _currentPage.value ?: return
        togglePageFavorite(page.id)
    }

    fun togglePageFavorite(pageId: String) {
        val repo = repository ?: return
        val newFav = repo.togglePageFavorite(pageId)
        if (_currentPage.value?.id == pageId) {
            _currentPage.value = _currentPage.value?.copy(isFavorite = newFav)
        }
    }

    fun movePageOrder(pageId: String, moveUp: Boolean) {
        repository?.movePageOrder(pageId, moveUp)
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val secId = _selectedSectionId.value ?: return
        repository?.reorderPages(secId, fromIndex, toIndex)
    }

    fun setToolbarDockEdge(edge: ToolbarDockEdge) {
        _toolbarDockEdge.value = edge
        appPreferences?.toolbarDockEdge = edge
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        appPreferences?.themeMode = mode
    }

    fun toggleSidebarGridView() {
        val next = !_isSidebarGridView.value
        _isSidebarGridView.value = next
        appPreferences?.isSidebarGridView = next
    }

    fun deletePage(pageId: String) {
        val repo = repository ?: return
        val secId = _selectedSectionId.value ?: return
        if (_pages.value.size <= 1 && _currentPage.value?.id == pageId) {
            engine.clear()
            flushCurrentPageSave()
            _saveStatusMessage.value = "Página limpa"
            return
        }
        repo.deletePage(pageId)
        if (_currentPage.value?.id == pageId) {
            val next = repo.database.getPagesBySection(secId).firstOrNull()
            if (next != null) {
                loadPageIntoEngine(next)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val repo = repository ?: return
        searchObserverJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _handwritingSearchResults.value = emptyList()
            engine.clearSearchHighlights()
            _currentSearchHighlights.value = emptyList()
            return
        }
        searchObserverJob = viewModelScope.launch {
            // Busca por título de página
            launch {
                repo.searchPages(query).collectLatest { list ->
                    _searchResults.value = list
                }
            }
            // Busca por escrita manual via SQLite FTS5
            launch(Dispatchers.IO) {
                val hwResults = repo.searchHandwriting(query)
                withContext(Dispatchers.Main) {
                    _handwritingSearchResults.value = hwResults
                    // Se a página atual tiver correspondência, realça no canvas
                    val curId = _currentPage.value?.id
                    val matchForCurrent = hwResults.find { it.page.id == curId }
                    if (matchForCurrent != null) {
                        val rects = matchForCurrent.matchedWordBoxes.map { it.toRectF() }
                        _currentSearchHighlights.value = rects
                        engine.setSearchHighlights(rects)
                    } else {
                        _currentSearchHighlights.value = emptyList()
                        engine.clearSearchHighlights()
                    }
                }
            }
        }
    }

    fun selectSearchResult(page: PageEntity, highlightBoxes: List<RectF> = emptyList()) {
        selectPage(page)
        if (highlightBoxes.isNotEmpty()) {
            _currentSearchHighlights.value = highlightBoxes
            engine.setSearchHighlights(highlightBoxes)
        }
    }

    suspend fun convertSelectedStrokesToText(): Result<String> {
        val inkMgr = digitalInkManager ?: return Result.failure(IllegalStateException("DigitalInkManager não inicializado"))
        val selected = engine.getSelectedStrokesSnapshot()
        if (selected.isEmpty()) {
            return Result.failure(IllegalStateException("Nenhum traço selecionado com o Laço."))
        }
        val result = inkMgr.recognizeStrokes(selected)
        return result.map { it.fullText }
    }

    fun getThumbnailFile(pageId: String): File? {
        return repository?.strokeStorage?.getThumbnailFile(pageId)
    }

    // --- Exportação PDF / PNG / Backup .lenote ---

    fun exportCurrentPageToPng(exportDark: Boolean = false, onComplete: ((ExportResult) -> Unit)? = null) {
        val ctx = appContext ?: return
        val pageTitle = _currentPage.value?.title ?: "Pagina"
        val bg = engine.state.value.pageBackground
        val strokes = engine.getFinishedStrokesSnapshot()

        _saveStatusMessage.value = "Exportando PNG..."
        viewModelScope.launch {
            try {
                val cur = _currentPage.value
                val result = withContext(Dispatchers.IO) {
                    PageExporter.exportPageToPng(
                        ctx,
                        pageTitle,
                        bg,
                        strokes,
                        isDarkMode = exportDark,
                        pdfPath = cur?.pdfPath,
                        pdfPageIndex = cur?.pdfPageIndex,
                        textBlocks = _textBlocks.value,
                        imageBlocks = _imageBlocks.value
                    )
                }
                _saveStatusMessage.value = result.userMessage
                onComplete?.invoke(result)
            } catch (e: Exception) {
                _saveStatusMessage.value = "Não foi possível exportar a imagem."
                val dummyFile = File(ctx.cacheDir, "export_error.png")
                onComplete?.invoke(
                    ExportResult(
                        localFile = dummyFile,
                        uri = null,
                        fileName = "",
                        mimeType = "image/png",
                        displayPath = "",
                        userMessage = "Não foi possível exportar a imagem.",
                        isSuccess = false,
                        errorMessage = e.message
                    )
                )
            }
        }
    }

    fun exportCurrentPageToPdf(exportDark: Boolean = false, onComplete: ((ExportResult) -> Unit)? = null) {
        val ctx = appContext ?: return
        val pageTitle = _currentPage.value?.title ?: "Pagina"
        val bg = engine.state.value.pageBackground
        val strokes = engine.getFinishedStrokesSnapshot()

        _saveStatusMessage.value = "Exportando PDF..."
        viewModelScope.launch {
            try {
                val cur = _currentPage.value
                val result = withContext(Dispatchers.IO) {
                    PageExporter.exportPageToPdf(
                        ctx,
                        pageTitle,
                        bg,
                        strokes,
                        isDarkMode = exportDark,
                        pdfPath = cur?.pdfPath,
                        pdfPageIndex = cur?.pdfPageIndex,
                        textBlocks = _textBlocks.value,
                        imageBlocks = _imageBlocks.value
                    )
                }
                _saveStatusMessage.value = result.userMessage
                onComplete?.invoke(result)
            } catch (e: Exception) {
                _saveStatusMessage.value = "Não foi possível exportar o PDF."
                val dummyFile = File(ctx.cacheDir, "export_error.pdf")
                onComplete?.invoke(
                    ExportResult(
                        localFile = dummyFile,
                        uri = null,
                        fileName = "",
                        mimeType = "application/pdf",
                        displayPath = "",
                        userMessage = "Não foi possível exportar o PDF.",
                        isSuccess = false,
                        errorMessage = e.message
                    )
                )
            }
        }
    }

    fun exportBackupLenotePackage(onComplete: ((File?) -> Unit)? = null) {
        val ctx = appContext ?: return
        val repo = repository ?: return
        flushCurrentPageSave()

        _saveStatusMessage.value = "Criando backup .lenote..."
        viewModelScope.launch {
            val outFile = withContext(Dispatchers.IO) {
                val downloads = ctx.getExternalFilesDir(null) ?: ctx.filesDir
                File(downloads, "backup").apply { mkdirs() }
                val targetFile = File(downloads, "LeNote_Backup_${System.currentTimeMillis()}.lenote")
                try {
                    repo.exportBackupLenotePackage(targetFile)
                    targetFile
                } catch (_: Exception) {
                    null
                }
            }
            onComplete?.invoke(outFile)
            _saveStatusMessage.value = "Backup criado: ${outFile?.name}"
        }
    }

    // --- Compatibilidade com botões manuais Salvar/Carregar e testes existentes ---

    fun saveCurrentStrokes() {
        val bytes = engine.saveStrokes()
        _savedStrokesData.value = bytes
        flushCurrentPageSave()
        val count = engine.state.value.strokeCount
        _saveStatusMessage.value = "Salvo: ${bytes.size} bytes ($count traços)"
    }

    fun loadSavedStrokes() {
        val bytes = _savedStrokesData.value
        if (bytes != null) {
            engine.loadStrokes(bytes)
            _saveStatusMessage.value = "Carregado com sucesso!"
        } else {
            _saveStatusMessage.value = "Nenhum traço salvo em memória!"
        }
    }

    override fun onCleared() {
        flushCurrentPageSave()
        engine.detachView()
        digitalInkManager?.close()
        super.onCleared()
    }
}
