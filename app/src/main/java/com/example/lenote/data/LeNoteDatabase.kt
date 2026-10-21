package com.example.lenote.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.lenote.ink.PageBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * DAO reativo para gerenciamento de Cadernos ([NotebookEntity]).
 */
interface NotebookDao {
    fun observeNotebooks(): Flow<List<NotebookEntity>>
    fun getAllNotebooks(): List<NotebookEntity>
    fun getNotebookById(id: String): NotebookEntity?
    fun upsertNotebook(notebook: NotebookEntity)
    fun deleteNotebook(id: String)
}

/**
 * DAO reativo para gerenciamento de Seções / Abas ([SectionEntity]).
 */
interface SectionDao {
    fun observeSectionsByNotebook(notebookId: String): Flow<List<SectionEntity>>
    fun getSectionsByNotebook(notebookId: String): List<SectionEntity>
    fun getAllSections(): List<SectionEntity>
    fun getSectionById(id: String): SectionEntity?
    fun upsertSection(section: SectionEntity)
    fun deleteSection(id: String)
}

/**
 * DAO reativo para gerenciamento de Páginas ([PageEntity]).
 */
interface PageDao {
    fun observePagesBySection(sectionId: String): Flow<List<PageEntity>>
    fun observeAllPages(): Flow<List<PageEntity>>
    fun observeFavoritePages(): Flow<List<PageEntity>>
    fun searchPages(query: String): Flow<List<PageEntity>>
    fun getPagesBySection(sectionId: String): List<PageEntity>
    fun getAllPages(): List<PageEntity>
    fun getPageById(id: String): PageEntity?
    fun upsertPage(page: PageEntity)
    fun deletePage(id: String)
    fun indexPageHandwriting(pageId: String, recognizedText: String, boundingBoxesJson: String)
    fun searchHandwriting(query: String): List<HandwritingSearchResult>
}

/**
 * Banco de dados SQLite local do LeNote com suporte a chaves estrangeiras em cascata (`ON DELETE CASCADE`)
 * e emissão reativa via [Flow] para atualização instantânea da UI.
 */

interface MediaBlockDao {
    fun getTextBlocksByPage(pageId: String): List<CanvasTextBlock>
    fun getImageBlocksByPage(pageId: String): List<CanvasImageBlock>
    fun upsertTextBlock(block: CanvasTextBlock)
    fun deleteTextBlock(id: String)
    fun upsertImageBlock(block: CanvasImageBlock)
    fun deleteImageBlock(id: String)
}

class LeNoteDatabase private constructor(
    context: Context?,
    dbName: String? = "lenote.db"
) : NotebookDao, SectionDao, PageDao, MediaBlockDao {

    private val sqliteHelper: SQLiteOpenHelper? = if (context != null) {
        object : SQLiteOpenHelper(context.applicationContext, dbName, null, DATABASE_VERSION) {
            override fun onConfigure(db: SQLiteDatabase) {
                super.onConfigure(db)
                db.setForeignKeyConstraintsEnabled(true)
            }

            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE notebooks (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE sections (
                        id TEXT PRIMARY KEY NOT NULL,
                        notebookId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        colorHex INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        FOREIGN KEY(notebookId) REFERENCES notebooks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX idx_sections_notebookId ON sections(notebookId)")
                db.execSQL(
                    """
                    CREATE TABLE pages (
                        id TEXT PRIMARY KEY NOT NULL,
                        sectionId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        pageBackground TEXT NOT NULL,
                        zoom REAL NOT NULL,
                        panX REAL NOT NULL,
                        panY REAL NOT NULL,
                        strokesFilePath TEXT NOT NULL,
                        thumbnailPath TEXT,
                        isFavorite INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        pdf_path TEXT,
                        pdf_page_index INTEGER,
                        FOREIGN KEY(sectionId) REFERENCES sections(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX idx_pages_sectionId ON pages(sectionId)")
                try { db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS page_search_index USING fts5(page_id UNINDEXED, recognized_text, bounding_boxes_json UNINDEXED)") } catch (_: Exception) {}
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS page_text_blocks (
                        id TEXT PRIMARY KEY NOT NULL,
                        page_id TEXT NOT NULL,
                        text TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        width REAL NOT NULL,
                        height REAL NOT NULL,
                        textColor INTEGER NOT NULL,
                        fontSize REAL NOT NULL,
                        zIndex REAL NOT NULL,
                        FOREIGN KEY(page_id) REFERENCES pages(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_blocks_pageId ON page_text_blocks(page_id)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS page_image_blocks (
                        id TEXT PRIMARY KEY NOT NULL,
                        page_id TEXT NOT NULL,
                        imagePath TEXT NOT NULL,
                        x REAL NOT NULL,
                        y REAL NOT NULL,
                        width REAL NOT NULL,
                        height REAL NOT NULL,
                        rotationDegrees REAL NOT NULL,
                        zIndex REAL NOT NULL,
                        FOREIGN KEY(page_id) REFERENCES pages(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_image_blocks_pageId ON page_image_blocks(page_id)")

            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                if (oldVersion < 2) {
                    try {
                        db.execSQL("ALTER TABLE pages ADD COLUMN thumbnailPath TEXT")
                        db.execSQL("ALTER TABLE pages ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                    } catch (_: Exception) {}
                }
                if (oldVersion < 3) {
                    try {
                        db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS page_search_index USING fts5(page_id UNINDEXED, recognized_text, bounding_boxes_json UNINDEXED)")
                    } catch (_: Exception) {}
                }
                if (oldVersion < 4) {
                    try {
                        db.execSQL("ALTER TABLE pages ADD COLUMN pdf_path TEXT")
                        db.execSQL("ALTER TABLE pages ADD COLUMN pdf_page_index INTEGER")
                    } catch (_: Exception) {}
                }
                if (oldVersion < 5) {
                    try {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS page_text_blocks (
                                id TEXT PRIMARY KEY NOT NULL,
                                page_id TEXT NOT NULL,
                                text TEXT NOT NULL,
                                x REAL NOT NULL,
                                y REAL NOT NULL,
                                width REAL NOT NULL,
                                height REAL NOT NULL,
                                textColor INTEGER NOT NULL,
                                fontSize REAL NOT NULL,
                                zIndex REAL NOT NULL,
                                FOREIGN KEY(page_id) REFERENCES pages(id) ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_text_blocks_pageId ON page_text_blocks(page_id)")

                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS page_image_blocks (
                                id TEXT PRIMARY KEY NOT NULL,
                                page_id TEXT NOT NULL,
                                imagePath TEXT NOT NULL,
                                x REAL NOT NULL,
                                y REAL NOT NULL,
                                width REAL NOT NULL,
                                height REAL NOT NULL,
                                rotationDegrees REAL NOT NULL,
                                zIndex REAL NOT NULL,
                                FOREIGN KEY(page_id) REFERENCES pages(id) ON DELETE CASCADE
                            )
                            """.trimIndent()
                        )
                        db.execSQL("CREATE INDEX IF NOT EXISTS idx_image_blocks_pageId ON page_image_blocks(page_id)")
                    } catch (_: Exception) {}
                }
            }
        }
    } else {
        null
    }

    // Armazenamento espelho em memória (usado também em testes unitários JVM sem Context Android)
    private val memNotebooks = LinkedHashMap<String, NotebookEntity>()
    private val memSections = LinkedHashMap<String, SectionEntity>()
    private val memPages = LinkedHashMap<String, PageEntity>()
    private val memHandwritingIndex = LinkedHashMap<String, Pair<String, String>>()
    private val memTextBlocks = LinkedHashMap<String, CanvasTextBlock>()
    private val memImageBlocks = LinkedHashMap<String, CanvasImageBlock>()

    private val notebooksFlow = MutableStateFlow<List<NotebookEntity>>(emptyList())
    private val sectionsFlow = MutableStateFlow<List<SectionEntity>>(emptyList())
    private val pagesFlow = MutableStateFlow<List<PageEntity>>(emptyList())

    val notebookDao: NotebookDao get() = this
    val sectionDao: SectionDao get() = this
    val pageDao: PageDao get() = this

    init {
        reloadAllFromStorage()
    }

    @Synchronized
    private fun reloadAllFromStorage() {
        val helper = sqliteHelper
        if (helper != null) {
            val db = helper.writableDatabase
            memNotebooks.clear()
            db.rawQuery("SELECT id, title, colorHex, createdAt, updatedAt, sortOrder FROM notebooks ORDER BY sortOrder ASC, createdAt ASC", null).use { c ->
                while (c.moveToNext()) {
                    val item = NotebookEntity(
                        id = c.getString(0),
                        title = c.getString(1),
                        colorHex = c.getInt(2),
                        createdAt = c.getLong(3),
                        updatedAt = c.getLong(4),
                        sortOrder = c.getInt(5)
                    )
                    memNotebooks[item.id] = item
                }
            }

            memSections.clear()
            db.rawQuery("SELECT id, notebookId, title, colorHex, createdAt, updatedAt, sortOrder FROM sections ORDER BY sortOrder ASC, createdAt ASC", null).use { c ->
                while (c.moveToNext()) {
                    val item = SectionEntity(
                        id = c.getString(0),
                        notebookId = c.getString(1),
                        title = c.getString(2),
                        colorHex = c.getInt(3),
                        createdAt = c.getLong(4),
                        updatedAt = c.getLong(5),
                        sortOrder = c.getInt(6)
                    )
                    memSections[item.id] = item
                }
            }

            memPages.clear()
            db.rawQuery(
                "SELECT id, sectionId, title, pageBackground, zoom, panX, panY, strokesFilePath, thumbnailPath, isFavorite, createdAt, updatedAt, sortOrder, pdf_path, pdf_page_index FROM pages ORDER BY sortOrder ASC, createdAt ASC",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val bgName = c.getString(3)
                    val bg = try {
                        PageBackground.valueOf(bgName)
                    } catch (_: Exception) {
                        PageBackground.BLANK
                    }
                    val item = PageEntity(
                        id = c.getString(0),
                        sectionId = c.getString(1),
                        title = c.getString(2),
                        pageBackground = bg,
                        zoom = c.getFloat(4),
                        panX = c.getFloat(5),
                        panY = c.getFloat(6),
                        strokesFilePath = c.getString(7),
                        thumbnailPath = if (c.isNull(8)) null else c.getString(8),
                        isFavorite = c.getInt(9) != 0,
                        createdAt = c.getLong(10),
                        updatedAt = c.getLong(11),
                        sortOrder = c.getInt(12),
                        pdfPath = if (c.isNull(13)) null else c.getString(13),
                        pdfPageIndex = if (c.isNull(14)) null else c.getInt(14)
                    )
                    memPages[item.id] = item
                }
            }

            memTextBlocks.clear()
            try {
                db.rawQuery("SELECT id, page_id, text, x, y, width, height, textColor, fontSize, zIndex FROM page_text_blocks", null).use { c ->
                    while (c.moveToNext()) {
                        val tb = CanvasTextBlock(
                            id = c.getString(0),
                            pageId = c.getString(1),
                            text = c.getString(2),
                            x = c.getFloat(3),
                            y = c.getFloat(4),
                            width = c.getFloat(5),
                            height = c.getFloat(6),
                            textColor = c.getInt(7),
                            fontSize = c.getFloat(8),
                            zIndex = c.getFloat(9)
                        )
                        memTextBlocks[tb.id] = tb
                    }
                }
            } catch (_: Exception) {}

            memImageBlocks.clear()
            try {
                db.rawQuery("SELECT id, page_id, imagePath, x, y, width, height, rotationDegrees, zIndex FROM page_image_blocks", null).use { c ->
                    while (c.moveToNext()) {
                        val ib = CanvasImageBlock(
                            id = c.getString(0),
                            pageId = c.getString(1),
                            imagePath = c.getString(2),
                            x = c.getFloat(3),
                            y = c.getFloat(4),
                            width = c.getFloat(5),
                            height = c.getFloat(6),
                            rotationDegrees = c.getFloat(7),
                            zIndex = c.getFloat(8)
                        )
                        memImageBlocks[ib.id] = ib
                    }
                }
            } catch (_: Exception) {}
        }

        publishFlows()
    }

    private fun publishFlows() {
        notebooksFlow.value = memNotebooks.values.sortedWith(compareBy<NotebookEntity> { it.sortOrder }.thenBy { it.createdAt })
        sectionsFlow.value = memSections.values.sortedWith(compareBy<SectionEntity> { it.sortOrder }.thenBy { it.createdAt })
        pagesFlow.value = memPages.values.sortedWith(compareBy<PageEntity> { it.sortOrder }.thenBy { it.createdAt })
    }

    override fun observeNotebooks(): Flow<List<NotebookEntity>> = notebooksFlow

    @Synchronized
    override fun getAllNotebooks(): List<NotebookEntity> = notebooksFlow.value

    @Synchronized
    override fun getNotebookById(id: String): NotebookEntity? = memNotebooks[id]

    @Synchronized
    override fun upsertNotebook(notebook: NotebookEntity) {
        sqliteHelper?.writableDatabase?.let { db ->
            val cv = ContentValues().apply {
                put("id", notebook.id)
                put("title", notebook.title)
                put("colorHex", notebook.colorHex)
                put("createdAt", notebook.createdAt)
                put("updatedAt", notebook.updatedAt)
                put("sortOrder", notebook.sortOrder)
            }
            db.insertWithOnConflict("notebooks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        memNotebooks[notebook.id] = notebook
        publishFlows()
    }

    @Synchronized
    override fun deleteNotebook(id: String) {
        sqliteHelper?.writableDatabase?.delete("notebooks", "id = ?", arrayOf(id))
        memNotebooks.remove(id)
        val removedSectionIds = memSections.values.filter { it.notebookId == id }.map { it.id }.toSet()
        removedSectionIds.forEach { secId -> memSections.remove(secId) }
        val removedPageIds = memPages.values.filter { it.sectionId in removedSectionIds }.map { it.id }
        removedPageIds.forEach { pageId -> memPages.remove(pageId) }
        publishFlows()
    }

    override fun observeSectionsByNotebook(notebookId: String): Flow<List<SectionEntity>> =
        sectionsFlow.map { list -> list.filter { it.notebookId == notebookId } }

    @Synchronized
    override fun getSectionsByNotebook(notebookId: String): List<SectionEntity> =
        sectionsFlow.value.filter { it.notebookId == notebookId }

    @Synchronized
    override fun getAllSections(): List<SectionEntity> = sectionsFlow.value

    @Synchronized
    override fun getSectionById(id: String): SectionEntity? = memSections[id]

    @Synchronized
    override fun upsertSection(section: SectionEntity) {
        sqliteHelper?.writableDatabase?.let { db ->
            val cv = ContentValues().apply {
                put("id", section.id)
                put("notebookId", section.notebookId)
                put("title", section.title)
                put("colorHex", section.colorHex)
                put("createdAt", section.createdAt)
                put("updatedAt", section.updatedAt)
                put("sortOrder", section.sortOrder)
            }
            db.insertWithOnConflict("sections", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        memSections[section.id] = section
        publishFlows()
    }

    @Synchronized
    override fun deleteSection(id: String) {
        sqliteHelper?.writableDatabase?.delete("sections", "id = ?", arrayOf(id))
        memSections.remove(id)
        val removedPageIds = memPages.values.filter { it.sectionId == id }.map { it.id }
        removedPageIds.forEach { pageId -> memPages.remove(pageId) }
        publishFlows()
    }

    override fun observePagesBySection(sectionId: String): Flow<List<PageEntity>> =
        pagesFlow.map { list -> list.filter { it.sectionId == sectionId } }

    override fun observeAllPages(): Flow<List<PageEntity>> = pagesFlow

    override fun observeFavoritePages(): Flow<List<PageEntity>> =
        pagesFlow.map { list -> list.filter { it.isFavorite } }

    override fun searchPages(query: String): Flow<List<PageEntity>> {
        val trimmed = query.trim().lowercase()
        return pagesFlow.map { list ->
            if (trimmed.isEmpty()) list
            else list.filter { it.title.lowercase().contains(trimmed) }
        }
    }

    @Synchronized
    override fun getPagesBySection(sectionId: String): List<PageEntity> =
        pagesFlow.value.filter { it.sectionId == sectionId }

    @Synchronized
    override fun getAllPages(): List<PageEntity> = pagesFlow.value

    @Synchronized
    override fun getPageById(id: String): PageEntity? = memPages[id]

    @Synchronized
    override fun upsertPage(page: PageEntity) {
        sqliteHelper?.writableDatabase?.let { db ->
            val cv = ContentValues().apply {
                put("id", page.id)
                put("sectionId", page.sectionId)
                put("title", page.title)
                put("pageBackground", page.pageBackground.name)
                put("zoom", page.zoom)
                put("panX", page.panX)
                put("panY", page.panY)
                put("strokesFilePath", page.strokesFilePath)
                put("thumbnailPath", page.thumbnailPath)
                put("isFavorite", if (page.isFavorite) 1 else 0)
                put("createdAt", page.createdAt)
                put("updatedAt", page.updatedAt)
                put("sortOrder", page.sortOrder)
                put("pdf_path", page.pdfPath)
                if (page.pdfPageIndex != null) {
                    put("pdf_page_index", page.pdfPageIndex)
                } else {
                    putNull("pdf_page_index")
                }
            }
            db.insertWithOnConflict("pages", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        memPages[page.id] = page
        publishFlows()
    }

    @Synchronized
    override fun deletePage(id: String) {
        sqliteHelper?.writableDatabase?.let { db ->
            db.delete("pages", "id = ?", arrayOf(id))
            try {
                db.delete("page_search_index", "page_id = ?", arrayOf(id))
            } catch (_: Exception) {}
        }
        memPages.remove(id)
        memHandwritingIndex.remove(id)
        memTextBlocks.values.filter { it.pageId == id }.map { it.id }.forEach { memTextBlocks.remove(it) }
        memImageBlocks.values.filter { it.pageId == id }.map { it.id }.forEach { memImageBlocks.remove(it) }
        publishFlows()
    }

    @Synchronized
    override fun indexPageHandwriting(pageId: String, recognizedText: String, boundingBoxesJson: String) {
        sqliteHelper?.writableDatabase?.let { db ->
            try {
                db.delete("page_search_index", "page_id = ?", arrayOf(pageId))
                if (recognizedText.isNotBlank()) {
                    val cv = ContentValues().apply {
                        put("page_id", pageId)
                        put("recognized_text", recognizedText)
                        put("bounding_boxes_json", boundingBoxesJson)
                    }
                    db.insert("page_search_index", null, cv)
                }
            } catch (_: Exception) {}
        }
        if (recognizedText.isNotBlank()) {
            memHandwritingIndex[pageId] = Pair(recognizedText, boundingBoxesJson)
        } else {
            memHandwritingIndex.remove(pageId)
        }
    }

    @Synchronized
    override fun searchHandwriting(query: String): List<HandwritingSearchResult> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<HandwritingSearchResult>()
        val helper = sqliteHelper
        if (helper != null) {
            try {
                val db = helper.readableDatabase
                val sanitized = trimmed.replace('"', ' ').replace('\'', ' ').trim()
                val cursor = db.rawQuery(
                    "SELECT page_id, recognized_text, bounding_boxes_json FROM page_search_index WHERE page_search_index MATCH ? ORDER BY rank",
                    arrayOf("$sanitized*")
                )
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val pageId = c.getString(0)
                        val page = memPages[pageId] ?: continue
                        val boxesJson = c.getString(2)
                        val allBoxes = WordBoxJsonSerializer.deserialize(boxesJson)
                        val matched = allBoxes.filter { it.word.lowercase().contains(trimmed) }
                        results.add(HandwritingSearchResult(page, matched))
                    }
                }
                return results
            } catch (_: Exception) {
            }
        }

        for ((pageId, pair) in memHandwritingIndex) {
            val (recText, boxesJson) = pair
            if (recText.lowercase().contains(trimmed)) {
                val page = memPages[pageId] ?: continue
                val allBoxes = WordBoxJsonSerializer.deserialize(boxesJson)
                val matched = allBoxes.filter { it.word.lowercase().contains(trimmed) }
                results.add(HandwritingSearchResult(page, matched))
            }
        }
        return results
    }


    @Synchronized
    override fun getTextBlocksByPage(pageId: String): List<CanvasTextBlock> {
        return memTextBlocks.values.filter { it.pageId == pageId }.sortedBy { it.zIndex }
    }

    @Synchronized
    override fun getImageBlocksByPage(pageId: String): List<CanvasImageBlock> {
        return memImageBlocks.values.filter { it.pageId == pageId }.sortedBy { it.zIndex }
    }

    @Synchronized
    override fun upsertTextBlock(block: CanvasTextBlock) {
        sqliteHelper?.writableDatabase?.let { db ->
            val cv = ContentValues().apply {
                put("id", block.id)
                put("page_id", block.pageId)
                put("text", block.text)
                put("x", block.x)
                put("y", block.y)
                put("width", block.width)
                put("height", block.height)
                put("textColor", block.textColor)
                put("fontSize", block.fontSize)
                put("zIndex", block.zIndex)
            }
            db.insertWithOnConflict("page_text_blocks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        memTextBlocks[block.id] = block
    }

    @Synchronized
    override fun deleteTextBlock(id: String) {
        sqliteHelper?.writableDatabase?.let { db ->
            db.delete("page_text_blocks", "id = ?", arrayOf(id))
        }
        memTextBlocks.remove(id)
    }

    @Synchronized
    override fun upsertImageBlock(block: CanvasImageBlock) {
        sqliteHelper?.writableDatabase?.let { db ->
            val cv = ContentValues().apply {
                put("id", block.id)
                put("page_id", block.pageId)
                put("imagePath", block.imagePath)
                put("x", block.x)
                put("y", block.y)
                put("width", block.width)
                put("height", block.height)
                put("rotationDegrees", block.rotationDegrees)
                put("zIndex", block.zIndex)
            }
            db.insertWithOnConflict("page_image_blocks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
        memImageBlocks[block.id] = block
    }

    @Synchronized
    override fun deleteImageBlock(id: String) {
        sqliteHelper?.writableDatabase?.let { db ->
            db.delete("page_image_blocks", "id = ?", arrayOf(id))
        }
        memImageBlocks.remove(id)
    }

    @Synchronized
    fun close() {
        sqliteHelper?.close()
    }

    companion object {
        private const val DATABASE_VERSION = 5

        @Volatile
        private var INSTANCE: LeNoteDatabase? = null

        fun getInstance(context: Context): LeNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LeNoteDatabase(context.applicationContext, "lenote.db").also {
                    INSTANCE = it
                }
            }
        }

        fun createInMemory(): LeNoteDatabase = LeNoteDatabase(context = null, dbName = null)
    }
}
