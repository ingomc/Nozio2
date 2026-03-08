package de.ingomc.nozio.data.local

enum class SupplementDayPart(val displayName: String, val sortOrder: Int) {
    PRE_BREAKFAST("Vorfrüh", 0),
    MIDDAY("Mittag", 1),
    EVENING("Abend", 2),
    LATE("Spät", 3)
}
