package com.wolferdwolf.drop.data

enum class SavedSourceType(val label: String) {
    TEXT("Text"),
    LINK("Link"),
    IMAGE("Image"),
    PDF("PDF"),
    UNKNOWN("Unknown");

    companion object {
        fun fromStored(value: String?): SavedSourceType =
            values().firstOrNull { it.name == value } ?: UNKNOWN
    }
}
