package com.edgepanel.model

import android.graphics.drawable.Drawable

data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var isSelected: Boolean = false
)

data class TaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    var isDone: Boolean = false
)

data class PersonItem(
    val contactId: String,
    val name: String,
    val phone: String,
    val photoUri: String? = null
)

data class PanelConfig(
    val id: String,
    val type: PanelType,
    val title: String,
    var isEnabled: Boolean = true,
    var order: Int = 0
)

enum class PanelType {
    APPS, TASKS, PEOPLE, WEATHER
}

data class HandleConfig(
    var position: HandlePosition = HandlePosition.RIGHT,
    var isLocked: Boolean = false,
    var colorHex: String = "#3D5AFE",
    var transparency: Float = 0.3f,
    var size: Float = 0.5f,
    var width: Float = 0.5f,
    var offsetY: Int = 400
)

enum class HandlePosition {
    LEFT, RIGHT
}

enum class PanelState {
    HIDDEN, MINIMIZED, NORMAL, MAXIMIZED, FULLSCREEN
}
