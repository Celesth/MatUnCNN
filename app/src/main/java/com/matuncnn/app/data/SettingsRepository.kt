package com.matuncnn.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            selectCommand = prefs[SELECT_COMMAND] ?: 0,
            tileSize = prefs[TILE_SIZE] ?: 0,
            extraCommand = prefs[EXTRA_COMMAND] ?: "",
            defaultCommand = prefs[DEFAULT_COMMAND] ?: "",
            classicalFilters = prefs[CLASSICAL_FILTERS]
                ?: context.getString(com.matuncnn.app.R.string.default_classical_filters),
            magickFilters = prefs[MAGICK_FILTERS]
                ?: context.getString(com.matuncnn.app.R.string.default_magick_filters),
            threadCount = prefs[THREAD_COUNT] ?: "",
            keepScreen = prefs[KEEP_SCREEN] ?: false,
            useMultFiles = prefs[USE_MULT_FILES] ?: false,
            prePng = prefs[PRE_PNG] ?: true,
            preFrame = prefs[PRE_FRAME] ?: true,
            extraPath = prefs[EXTRA_PATH] ?: "",
            savePath = prefs[SAVE_PATH] ?: "",
            useCPU = prefs[USE_CPU] ?: false,
            autoSave = prefs[AUTO_SAVE] ?: false,
            showSearchView = prefs[SHOW_SEARCH_VIEW] ?: false,
            showFinalCommand = prefs[SHOW_FINAL_COMMAND] ?: false,
            useCustomLabel = prefs[USE_CUSTOM_LABEL] ?: false,
            format = prefs[FORMAT] ?: 0,
            dirOutputFormat = prefs[DIR_OUTPUT_FORMAT] ?: 0,
            name = prefs[NAME] ?: 0,
            name2 = prefs[NAME2] ?: 0,
            orientation = prefs[ORIENTATION] ?: 0,
            notify = prefs[NOTIFY] ?: 2,
            mnnBackend = prefs[MNN_BACKEND] ?: 3,
            hiddenPrograms = (prefs[HIDDEN_PROGRAMS] ?: "").split(",")
                .filter { it.isNotBlank() }.toSet(),
            customLabelsJson = prefs[CUSTOM_LABELS_JSON] ?: "",
            themeIndex = prefs[THEME_INDEX] ?: 0,
        )
    }

    suspend fun update(transform: suspend (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = settingsFromPrefs(prefs, context)
            val updated = transform(current)
            prefs[SELECT_COMMAND] = updated.selectCommand
            prefs[TILE_SIZE] = updated.tileSize
            prefs[EXTRA_COMMAND] = updated.extraCommand
            prefs[DEFAULT_COMMAND] = updated.defaultCommand
            prefs[CLASSICAL_FILTERS] = updated.classicalFilters
            prefs[MAGICK_FILTERS] = updated.magickFilters
            prefs[THREAD_COUNT] = updated.threadCount
            prefs[KEEP_SCREEN] = updated.keepScreen
            prefs[USE_MULT_FILES] = updated.useMultFiles
            prefs[PRE_PNG] = updated.prePng
            prefs[PRE_FRAME] = updated.preFrame
            prefs[EXTRA_PATH] = updated.extraPath
            prefs[SAVE_PATH] = updated.savePath
            prefs[USE_CPU] = updated.useCPU
            prefs[AUTO_SAVE] = updated.autoSave
            prefs[SHOW_SEARCH_VIEW] = updated.showSearchView
            prefs[SHOW_FINAL_COMMAND] = updated.showFinalCommand
            prefs[USE_CUSTOM_LABEL] = updated.useCustomLabel
            prefs[FORMAT] = updated.format
            prefs[DIR_OUTPUT_FORMAT] = updated.dirOutputFormat
            prefs[NAME] = updated.name
            prefs[NAME2] = updated.name2
            prefs[ORIENTATION] = updated.orientation
            prefs[NOTIFY] = updated.notify
            prefs[MNN_BACKEND] = updated.mnnBackend
            prefs[HIDDEN_PROGRAMS] = updated.hiddenPrograms.joinToString(",")
            prefs[CUSTOM_LABELS_JSON] = updated.customLabelsJson
            prefs[THEME_INDEX] = updated.themeIndex
        }
    }

    private fun settingsFromPrefs(prefs: Preferences, ctx: Context): AppSettings {
        return AppSettings(
            selectCommand = prefs[SELECT_COMMAND] ?: 0,
            tileSize = prefs[TILE_SIZE] ?: 0,
            extraCommand = prefs[EXTRA_COMMAND] ?: "",
            defaultCommand = prefs[DEFAULT_COMMAND] ?: "",
            classicalFilters = prefs[CLASSICAL_FILTERS]
                ?: ctx.getString(com.matuncnn.app.R.string.default_classical_filters),
            magickFilters = prefs[MAGICK_FILTERS]
                ?: ctx.getString(com.matuncnn.app.R.string.default_magick_filters),
            threadCount = prefs[THREAD_COUNT] ?: "",
            keepScreen = prefs[KEEP_SCREEN] ?: false,
            useMultFiles = prefs[USE_MULT_FILES] ?: false,
            prePng = prefs[PRE_PNG] ?: true,
            preFrame = prefs[PRE_FRAME] ?: true,
            extraPath = prefs[EXTRA_PATH] ?: "",
            savePath = prefs[SAVE_PATH] ?: "",
            useCPU = prefs[USE_CPU] ?: false,
            autoSave = prefs[AUTO_SAVE] ?: false,
            showSearchView = prefs[SHOW_SEARCH_VIEW] ?: false,
            showFinalCommand = prefs[SHOW_FINAL_COMMAND] ?: false,
            useCustomLabel = prefs[USE_CUSTOM_LABEL] ?: false,
            format = prefs[FORMAT] ?: 0,
            dirOutputFormat = prefs[DIR_OUTPUT_FORMAT] ?: 0,
            name = prefs[NAME] ?: 0,
            name2 = prefs[NAME2] ?: 0,
            orientation = prefs[ORIENTATION] ?: 0,
            notify = prefs[NOTIFY] ?: 2,
            mnnBackend = prefs[MNN_BACKEND] ?: 3,
            hiddenPrograms = (prefs[HIDDEN_PROGRAMS] ?: "").split(",")
                .filter { it.isNotBlank() }.toSet(),
            customLabelsJson = prefs[CUSTOM_LABELS_JSON] ?: "",
            themeIndex = prefs[THEME_INDEX] ?: 0,
        )
    }

    companion object {
        private val SELECT_COMMAND = intPreferencesKey("selectCommand")
        private val TILE_SIZE = intPreferencesKey("tileSize")
        private val EXTRA_COMMAND = stringPreferencesKey("extraCommand")
        private val DEFAULT_COMMAND = stringPreferencesKey("defaultCommand")
        private val CLASSICAL_FILTERS = stringPreferencesKey("classicalFilters")
        private val MAGICK_FILTERS = stringPreferencesKey("magickFilters")
        private val THREAD_COUNT = stringPreferencesKey("threadCount")
        private val KEEP_SCREEN = booleanPreferencesKey("keepScreen")
        private val USE_MULT_FILES = booleanPreferencesKey("useMultFiles")
        private val PRE_PNG = booleanPreferencesKey("PrePng")
        private val PRE_FRAME = booleanPreferencesKey("PreFrame")
        private val EXTRA_PATH = stringPreferencesKey("extraPath")
        private val SAVE_PATH = stringPreferencesKey("savePath")
        private val USE_CPU = booleanPreferencesKey("useCPU")
        private val AUTO_SAVE = booleanPreferencesKey("autoSave")
        private val SHOW_SEARCH_VIEW = booleanPreferencesKey("showSearchView")
        private val SHOW_FINAL_COMMAND = booleanPreferencesKey("showFinalCommand")
        private val USE_CUSTOM_LABEL = booleanPreferencesKey("useCustomLabel")
        private val FORMAT = intPreferencesKey("format")
        private val DIR_OUTPUT_FORMAT = intPreferencesKey("dirOutputFormat")
        private val NAME = intPreferencesKey("name")
        private val NAME2 = intPreferencesKey("name2")
        private val ORIENTATION = intPreferencesKey("ORIENTATION")
        private val NOTIFY = intPreferencesKey("notify")
        private val MNN_BACKEND = intPreferencesKey("mnnBackend")
        private val HIDDEN_PROGRAMS = stringPreferencesKey("hiddenPrograms")
        private val CUSTOM_LABELS_JSON = stringPreferencesKey("customLabelsJson")
        private val THEME_INDEX = intPreferencesKey("themeIndex")
    }
}

data class AppSettings(
    val selectCommand: Int = 0,
    val tileSize: Int = 0,
    val extraCommand: String = "",
    val defaultCommand: String = "",
    val classicalFilters: String = "nearest",
    val magickFilters: String = "Lanczos",
    val threadCount: String = "",
    val keepScreen: Boolean = false,
    val useMultFiles: Boolean = false,
    val prePng: Boolean = true,
    val preFrame: Boolean = true,
    val extraPath: String = "",
    val savePath: String = "",
    val useCPU: Boolean = false,
    val autoSave: Boolean = false,
    val showSearchView: Boolean = false,
    val showFinalCommand: Boolean = false,
    val useCustomLabel: Boolean = false,
    val format: Int = 0,
    val dirOutputFormat: Int = 0,
    val name: Int = 0,
    val name2: Int = 0,
    val orientation: Int = 0,
    val notify: Int = 2,
    val mnnBackend: Int = 3,
    val hiddenPrograms: Set<String> = emptySet(),
    val customLabelsJson: String = "",
    val themeIndex: Int = 0
)
