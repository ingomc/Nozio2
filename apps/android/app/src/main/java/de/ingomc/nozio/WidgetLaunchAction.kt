package de.ingomc.nozio

enum class WidgetLaunchAction(val extraValue: String) {
    NONE("none"),
    SEARCH_FOCUS("search_focus"),
    BARCODE_SCANNER("barcode_scanner");

    companion object {
        const val EXTRA_WIDGET_LAUNCH_ACTION = "de.ingomc.nozio.extra.WIDGET_LAUNCH_ACTION"

        fun fromExtraValue(value: String?): WidgetLaunchAction {
            return entries.firstOrNull { it.extraValue == value } ?: NONE
        }
    }
}
