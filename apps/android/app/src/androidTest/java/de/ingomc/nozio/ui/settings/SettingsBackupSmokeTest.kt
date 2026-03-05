package de.ingomc.nozio.ui.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.ingomc.nozio.MainActivity
import org.junit.Rule
import org.junit.Test

class SettingsBackupSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsShowsBackupSectionAndRestoreDialogCanBeDismissed() {
        composeRule.onNodeWithText("Einstellungen").performClick()
        composeRule.onNodeWithText("Backup & Wiederherstellung").assertIsDisplayed()
        composeRule.onNodeWithText("Wiederherstellen").performClick()
        composeRule.onNodeWithText("Backup wiederherstellen").assertIsDisplayed()
        composeRule.onNodeWithText("Abbrechen").performClick()
        composeRule.onAllNodesWithText("Backup wiederherstellen").assertCountEquals(0)
    }
}
