package de.ingomc.nozio

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetLaunchActionTest {
    @Test
    fun fromExtraValue_returnsExpectedAction() {
        assertEquals(WidgetLaunchAction.SEARCH_FOCUS, WidgetLaunchAction.fromExtraValue("search_focus"))
        assertEquals(WidgetLaunchAction.BARCODE_SCANNER, WidgetLaunchAction.fromExtraValue("barcode_scanner"))
    }

    @Test
    fun fromExtraValue_defaultsToNoneWhenUnknownOrMissing() {
        assertEquals(WidgetLaunchAction.NONE, WidgetLaunchAction.fromExtraValue("unexpected"))
        assertEquals(WidgetLaunchAction.NONE, WidgetLaunchAction.fromExtraValue(null))
    }
}
