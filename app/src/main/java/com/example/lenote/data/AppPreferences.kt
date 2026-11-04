package com.example.lenote.data

import android.content.Context
import android.content.SharedPreferences

enum class ToolbarDockEdge {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String) {
    SYSTEM("system"),
    PT("pt"),
    EN("en"),
    ES("es")
}

/**
 * Gerenciador de preferências do usuário (Toolbar, Tema, Idioma, Visualização de Páginas).
 * Utiliza SharedPreferences para persistência rápida e síncrona sem overhead.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var toolbarDockEdge: ToolbarDockEdge
        get() {
            val name = prefs.getString(KEY_TOOLBAR_DOCK_EDGE, ToolbarDockEdge.TOP.name)
            return runCatching { ToolbarDockEdge.valueOf(name ?: ToolbarDockEdge.TOP.name) }
                .getOrDefault(ToolbarDockEdge.TOP)
        }
        set(value) {
            prefs.edit().putString(KEY_TOOLBAR_DOCK_EDGE, value.name).apply()
        }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var appLanguage: AppLanguage
        get() {
            val name = prefs.getString(KEY_APP_LANGUAGE, AppLanguage.SYSTEM.name)
            return runCatching { AppLanguage.valueOf(name ?: AppLanguage.SYSTEM.name) }
                .getOrDefault(AppLanguage.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_APP_LANGUAGE, value.name).apply()
        }

    var isTouchInkingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TOUCH_INKING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TOUCH_INKING_ENABLED, value).apply()
        }

    var isSidebarGridView: Boolean
        get() = prefs.getBoolean(KEY_SIDEBAR_GRID_VIEW, false)
        set(value) {
            prefs.edit().putBoolean(KEY_SIDEBAR_GRID_VIEW, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "lenote_user_preferences"
        private const val KEY_TOOLBAR_DOCK_EDGE = "toolbar_dock_edge"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_TOUCH_INKING_ENABLED = "touch_inking_enabled"
        private const val KEY_SIDEBAR_GRID_VIEW = "sidebar_grid_view"
    }
}
