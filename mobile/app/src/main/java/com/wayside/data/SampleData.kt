package com.wayside.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.wayside.ui.theme.CategoryCulture
import com.wayside.ui.theme.CategoryFood
import com.wayside.ui.theme.CategoryHiddenGem
import com.wayside.ui.theme.CategoryNature
import com.wayside.ui.theme.CategoryViewpoints

/** [icon] is what the category draws in the centre of its map pin — see [MapPin]. */
enum class PlaceCategory(val label: String, val color: Color, val icon: ImageVector) {
    Food("Food", CategoryFood, Icons.Rounded.Restaurant),
    Nature("Nature", CategoryNature, Icons.Rounded.Park),
    Culture("Culture", CategoryCulture, Icons.Rounded.AccountBalance),
    Viewpoints("Viewpoints", CategoryViewpoints, Icons.Rounded.Visibility),
    HiddenGem("Hidden gem", CategoryHiddenGem, Icons.Rounded.Diamond),
}

data class ReviewTheme(val label: String, val count: Int)

data class Place(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val rating: Double,
    val ratingCount: Int,
    val detourMinutes: Int,
    val distanceKm: Double,
    /** Where the place actually is. The real map pins by these. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /**
     * Fields below come from `/places/{id}` only — a place built from a suggestion list has
     * them blank until the detail screen fetches it.
     */
    val openUntil: String = "",
    val highlight: String = "",
    val aiSummary: String = "",
    val tags: List<String> = emptyList(),
    val reviewThemes: List<ReviewTheme> = emptyList(),
    val photoCount: Int = 0,
    /** Normalised position on the stylised map, 0f..1f. */
    val mapX: Float,
    val mapY: Float,
    /** How far along the Alfama → Sintra drive this place sits, 0f..1f. */
    val routeProgress: Float,
)

enum class Units(val label: String) {
    Kilometres("Kilometres"),
    Miles("Miles"),
}

object Route {
    // Kept for previews only — at runtime the drive comes from the search screen and the
    // departure from the phone's clock.
    const val ORIGIN = "Alfama, Lisbon"
    const val DESTINATION = "Sintra"
    const val DRIVE_MINUTES = 52
    const val DISTANCE_KM = 28

    val BUDGETS = listOf(0, 15, 30, 45)

    fun budgetHint(budget: Int): String = when (budget) {
        0 -> "fastest route only"
        15 -> "a quick stop or two"
        30 -> "a proper wander"
        else -> "make a day of it"
    }
}

/**
 * A fixed set of places along the Alfama → Sintra drive, kept for `@Preview` composables and
 * nothing else. At runtime the app draws its places from `/routes/suggestions`.
 */
val SAMPLE_PLACES = listOf(
    Place(
        id = "sacolinha",
        name = "Pastelaria Sacolinha",
        category = PlaceCategory.Food,
        rating = 4.6,
        ratingCount = 312,
        detourMinutes = 6,
        distanceKm = 1.8,
        latitude = 38.757,
        longitude = -9.22,
        openUntil = "19:00",
        highlight = "The custard tarts come out of the oven at 10:00 and sell out by noon.",
        aiSummary = "Reviewers keep coming back for the pastéis de nata straight from the oven, " +
            "and note the counter staff will tell you when the next tray is due.",
        tags = listOf("Pastéis de nata", "Quick stop", "Cash preferred"),
        reviewThemes = listOf(
            ReviewTheme("Fresh pastries", 184),
            ReviewTheme("Fast service", 96),
            ReviewTheme("Small seating area", 41),
        ),
        mapX = 0.22f,
        mapY = 0.72f,
        routeProgress = 0.14f,
    ),
    Place(
        id = "queluz",
        name = "Palácio de Queluz",
        category = PlaceCategory.Culture,
        rating = 4.7,
        ratingCount = 1841,
        detourMinutes = 14,
        distanceKm = 2.6,
        latitude = 38.7503,
        longitude = -9.2586,
        openUntil = "18:00",
        highlight = "A rococo summer palace with formal gardens you can walk in 30 minutes.",
        aiSummary = "Visitors describe the tiled canal and the throne room as the two things " +
            "worth the ticket, and most finish the gardens in about half an hour.",
        tags = listOf("Rococo palace", "Formal gardens", "Ticketed entry"),
        reviewThemes = listOf(
            ReviewTheme("Gardens", 742),
            ReviewTheme("Tiled canal", 410),
            ReviewTheme("Queues at midday", 188),
        ),
        mapX = 0.36f,
        mapY = 0.633f,
        routeProgress = 0.30f,
    ),
    Place(
        id = "poetas",
        name = "Parque dos Poetas",
        category = PlaceCategory.Nature,
        rating = 4.5,
        ratingCount = 2204,
        detourMinutes = 9,
        distanceKm = 3.1,
        latitude = 38.7075,
        longitude = -9.2822,
        openUntil = "20:00",
        highlight = "Twenty sculptures of Portuguese poets scattered across open lawns.",
        aiSummary = "People treat this as a leg-stretch stop: flat paths, shade in the upper " +
            "terrace, and sculptures signposted with the poems they belong to.",
        tags = listOf("Sculpture walk", "Shade", "Free entry"),
        reviewThemes = listOf(
            ReviewTheme("Sculptures", 908),
            ReviewTheme("Good for walking", 615),
            ReviewTheme("Limited parking", 142),
        ),
        mapX = 0.30f,
        mapY = 0.573f,
        routeProgress = 0.38f,
    ),
    Place(
        id = "agualva",
        name = "Mercado de Agualva",
        category = PlaceCategory.Food,
        rating = 4.3,
        ratingCount = 428,
        detourMinutes = 12,
        distanceKm = 2.2,
        latitude = 38.77,
        longitude = -9.305,
        openUntil = "14:00",
        highlight = "A working produce market where the fish stalls close first, around 13:00.",
        aiSummary = "Regulars say the cheese and cured meat counters at the back are the reason " +
            "to come, and that most stalls have packed up by early afternoon.",
        tags = listOf("Produce market", "Local cheese", "Mornings only"),
        reviewThemes = listOf(
            ReviewTheme("Cheese counter", 201),
            ReviewTheme("Fair prices", 133),
            ReviewTheme("Closes early", 78),
        ),
        mapX = 0.50f,
        mapY = 0.553f,
        routeProgress = 0.46f,
    ),
    Place(
        id = "sabuga",
        name = "Fonte da Sabuga",
        category = PlaceCategory.HiddenGem,
        rating = 4.4,
        ratingCount = 96,
        detourMinutes = 21,
        distanceKm = 5.0,
        latitude = 38.793,
        longitude = -9.383,
        openUntil = "22:00",
        highlight = "A tiled spring fountain on a lane most people drive straight past.",
        aiSummary = "The few reviews agree on the same thing: it takes five minutes, the lane " +
            "is narrow enough that parking means walking the last 200 metres.",
        tags = listOf("Tiled fountain", "Five-minute stop", "Narrow lane"),
        reviewThemes = listOf(
            ReviewTheme("Azulejo tilework", 48),
            ReviewTheme("Quiet", 31),
            ReviewTheme("Hard to park", 22),
        ),
        mapX = 0.62f,
        mapY = 0.467f,
        routeProgress = 0.62f,
    ),
    Place(
        id = "capuchos",
        name = "Convento dos Capuchos",
        category = PlaceCategory.Culture,
        rating = 4.8,
        ratingCount = 1207,
        detourMinutes = 34,
        distanceKm = 9.2,
        latitude = 38.7847,
        longitude = -9.4278,
        openUntil = "18:30",
        highlight = "Cork-lined monk cells built into the rock of the Sintra hills.",
        aiSummary = "Almost every review mentions the cork-lined cells and how low the doorways " +
            "are; the walk between them is uneven and damp after rain.",
        tags = listOf("Cork cells", "Forest setting", "Uneven paths"),
        reviewThemes = listOf(
            ReviewTheme("Cork architecture", 561),
            ReviewTheme("Peaceful", 344),
            ReviewTheme("Slippery when wet", 129),
        ),
        mapX = 0.74f,
        mapY = 0.447f,
        routeProgress = 0.74f,
    ),
    Place(
        id = "peninha",
        name = "Miradouro da Peninha",
        category = PlaceCategory.Viewpoints,
        rating = 4.8,
        ratingCount = 486,
        detourMinutes = 43,
        distanceKm = 12.6,
        latitude = 38.7822,
        longitude = -9.4561,
        openUntil = "20:30",
        highlight = "On a clear afternoon you can see Cabo da Roca and the whole coastline.",
        aiSummary = "Reviewers say the view reaches Cabo da Roca when the sea fog lifts, and " +
            "that the last stretch of road is single-lane with passing places.",
        tags = listOf("Coastal view", "Sunset spot", "Single-lane road"),
        reviewThemes = listOf(
            ReviewTheme("Coastline view", 298),
            ReviewTheme("Windy", 121),
            ReviewTheme("Rough final road", 74),
        ),
        mapX = 0.82f,
        mapY = 0.36f,
        routeProgress = 0.86f,
    ),
)

/** Previews only — at runtime places come from the API. */
fun placeById(id: String): Place = SAMPLE_PLACES.first { it.id == id }

/** 570 → "09:30" */
fun formatClock(minutesFromMidnight: Int): String {
    val wrapped = ((minutesFromMidnight % 1440) + 1440) % 1440
    val h = wrapped / 60
    val m = wrapped % 60
    return "%02d:%02d".format(h, m)
}

fun formatDistance(km: Double, units: Units): String = when (units) {
    Units.Kilometres -> "%.1f km".format(km)
    Units.Miles -> "%.1f mi".format(km * 0.621371)
}

fun formatDistance(km: Int, units: Units): String = when (units) {
    Units.Kilometres -> "$km km"
    Units.Miles -> "${(km * 0.621371).toInt()} mi"
}

val INTERESTS = listOf(
    "Coffee",
    "Food",
    "Nature",
    "History",
    "Art",
    "Viewpoints",
    "Hidden gems",
    "Markets",
    "Architecture",
)

val SAVED_FILTERS = listOf("All", "Food", "Nature", "Culture", "Viewpoints", "Hidden gems")
