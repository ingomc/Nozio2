package de.ingomc.nozio.ui.supplements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.ingomc.nozio.MainActivity
import de.ingomc.nozio.NozioApplication
import de.ingomc.nozio.data.local.SupplementAmountUnit
import de.ingomc.nozio.data.local.SupplementDayPart
import de.ingomc.nozio.data.local.SupplementPlanItemEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SupplementsNavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun cleanupSupplements() = runBlocking {
        val app = composeRule.activity.application as NozioApplication
        app.database.supplementIntakeDao().deleteAll()
        app.database.supplementDao().deleteAll()
    }

    @Test
    fun dashboardSupplementEditButtonNavigatesToSupplementsScreen() {
        val app = composeRule.activity.application as NozioApplication
        runBlocking {
            app.database.supplementIntakeDao().deleteAll()
            app.database.supplementDao().deleteAll()
            app.database.supplementDao().upsert(
                SupplementPlanItemEntity(
                    name = "Magnesium",
                    dayPart = SupplementDayPart.EVENING,
                    scheduledMinutesOfDay = 20 * 60,
                    amountValue = 1.0,
                    amountUnit = SupplementAmountUnit.TABLET
                )
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Supplements")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Supplements").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Supplements bearbeiten").performClick()
        composeRule.onNodeWithText("Supplements bearbeiten").assertIsDisplayed()
    }
}
