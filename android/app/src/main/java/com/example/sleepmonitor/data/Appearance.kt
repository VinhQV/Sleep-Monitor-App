package com.example.sleepmonitor.data

enum class AppearanceMode(val label: String) {
    AUTOMATIC("Tự động"), LIGHT("Sáng"), DARK("Tối");
    companion object {
        fun fromStored(value: String?) = entries.firstOrNull { it.name == value } ?: AUTOMATIC
    }
}

fun usesDarkTheme(mode: AppearanceMode, hour: Int): Boolean = when (mode) {
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
    AppearanceMode.AUTOMATIC -> hour !in 6..17
}
