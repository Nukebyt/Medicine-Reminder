package com.nukebyt.medicinereminder.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nukebyt.medicinereminder.ui.components.SwipeToStop
import com.nukebyt.medicinereminder.ui.theme.MedicineReminderTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwipeToStopTest {
    @get:Rule val rule = createComposeRule()

    private var stops = 0

    private fun setUp() {
        stops = 0
        rule.setContent {
            MedicineReminderTheme(darkTheme = true) {
                SwipeToStop(label = "Swipe to stop", stopActionLabel = "Stop alarm", onStop = { stops++ })
            }
        }
    }

    @Test fun fullSwipeStopsTheAlarmOnce() {
        setUp()
        rule.onNodeWithContentDescription("Swipe to stop").assertIsDisplayed()
        rule.onNodeWithContentDescription("Swipe to stop").performTouchInput {
            // Start on the thumb (left end of the track) and drag across the whole track.
            down(Offset(60f, centerY))
            moveBy(Offset(width.toFloat(), 0f))
            up()
        }
        rule.waitForIdle()
        assertEquals(1, stops)
    }

    @Test fun shortSwipeSpringsBackWithoutStopping() {
        setUp()
        rule.onNodeWithContentDescription("Swipe to stop").performTouchInput {
            down(Offset(60f, centerY))
            moveBy(Offset(width * 0.3f, 0f))
            up()
        }
        rule.waitForIdle()
        assertEquals(0, stops)
    }
}
