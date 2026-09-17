package com.wayside.models.ui

/**
 * One row of the search dropdown. [placeId] is what routing uses — the text is for the person
 * reading it, and re-resolving that text later can land somewhere else entirely.
 */
data class PlaceSuggestion(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
) {
    val label: String get() = if (secondaryText.isBlank()) primaryText else "$primaryText, $secondaryText"
}
