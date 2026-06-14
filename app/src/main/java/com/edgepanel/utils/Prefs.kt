package com.edgepanel.utils

import android.content.Context
import android.content.SharedPreferences
import com.edgepanel.model.HandleConfig
import com.edgepanel.model.HandlePosition
import com.edgepanel.model.PanelConfig
import com.edgepanel.model.PanelType
import com.edgepanel.model.TaskItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Prefs {

    private const val PREF_NAME = "edge_panel_prefs"
    private val gson = Gson()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Handle Config ─────────────────────────────────────────────────────────

    fun saveHandleConfig(context: Context, config: HandleConfig) {
        prefs(context).edit()
            .putString("handle_position", config.position.name)
            .putBoolean("handle_locked", config.isLocked)
            .putString("handle_color", config.colorHex)
            .putFloat("handle_transparency", config.transparency)
            .putFloat("handle_size", config.size)
            .putFloat("handle_width", config.width)
            .putInt("handle_offset_y", config.offsetY)
            .apply()
    }

    fun loadHandleConfig(context: Context): HandleConfig {
        val p = prefs(context)
        return HandleConfig(
            position = HandlePosition.valueOf(p.getString("handle_position", HandlePosition.RIGHT.name)!!),
            isLocked = p.getBoolean("handle_locked", false),
            colorHex = p.getString("handle_color", "#3D5AFE")!!,
            transparency = p.getFloat("handle_transparency", 0.3f),
            size = p.getFloat("handle_size", 0.5f),
            width = p.getFloat("handle_width", 0.5f),
            offsetY = p.getInt("handle_offset_y", 400)
        )
    }

    // ── Panel Configs ─────────────────────────────────────────────────────────

    fun savePanelConfigs(context: Context, panels: List<PanelConfig>) {
        prefs(context).edit()
            .putString("panel_configs", gson.toJson(panels))
            .apply()
    }

    fun loadPanelConfigs(context: Context): List<PanelConfig> {
        val json = prefs(context).getString("panel_configs", null)
        return if (json != null) {
            val type = object : TypeToken<List<PanelConfig>>() {}.type
            gson.fromJson(json, type)
        } else {
            defaultPanels()
        }
    }

    private fun defaultPanels() = listOf(
        PanelConfig("apps", PanelType.APPS, "Apps", true, 0),
        PanelConfig("tasks", PanelType.TASKS, "Tasks", true, 1),
        PanelConfig("people", PanelType.PEOPLE, "People", true, 2),
        PanelConfig("weather", PanelType.WEATHER, "Weather", true, 3)
    )

    // ── Selected Apps ─────────────────────────────────────────────────────────

    fun saveSelectedApps(context: Context, packages: List<String>) {
        prefs(context).edit()
            .putString("selected_apps", gson.toJson(packages))
            .apply()
    }

    fun loadSelectedApps(context: Context): List<String> {
        val json = prefs(context).getString("selected_apps", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // ── Selected People ───────────────────────────────────────────────────────

    fun saveSelectedContacts(context: Context, contactIds: List<String>) {
        prefs(context).edit()
            .putString("selected_contacts", gson.toJson(contactIds))
            .apply()
    }

    fun loadSelectedContacts(context: Context): List<String> {
        val json = prefs(context).getString("selected_contacts", null) ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    fun saveTasks(context: Context, tasks: List<TaskItem>) {
        prefs(context).edit()
            .putString("tasks", gson.toJson(tasks))
            .apply()
    }

    fun loadTasks(context: Context): MutableList<TaskItem> {
        val json = prefs(context).getString("tasks", null) ?: return mutableListOf()
        val type = object : TypeToken<List<TaskItem>>() {}.type
        return gson.fromJson(json, type)
    }

    // ── Service enabled ───────────────────────────────────────────────────────

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("service_enabled", enabled).apply()
    }

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean("service_enabled", false)
}
