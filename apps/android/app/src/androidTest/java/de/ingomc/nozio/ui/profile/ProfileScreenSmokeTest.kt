package de.ingomc.nozio.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.ingomc.nozio.MainActivity
import org.junit.Rule
import org.junit.Test

class ProfileScreenSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun profileShowsSummaryProgressGoalsAndEditableSection() {
        composeRule.onNodeWithText("Profil").performClick()

        composeRule.onNodeWithText("Mein Fortschritt").assertIsDisplayed()
        composeRule.onNodeWithText("Meine Ziele").assertIsDisplayed()

        composeRule.onNodeWithText("BEARBEITEN").performClick()
        composeRule.onNodeWithText("Meine Ziele bearbeiten").assertIsDisplayed()
    }
}
