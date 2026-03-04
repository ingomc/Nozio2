package de.ingomc.nozio.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateCustomFoodBottomSheetTest {

    @Test
    fun parseDecimalInput_acceptsCommaAndDot() {
        assertEquals(12.5, parseDecimalInput("12,5"))
        assertEquals(12.5, parseDecimalInput("12.5"))
    }

    @Test
    fun parseDecimalInput_returnsNullForBlankOrInvalid() {
        assertNull(parseDecimalInput(""))
        assertNull(parseDecimalInput("abc"))
    }
}
