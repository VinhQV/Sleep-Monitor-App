package com.example.sleepmonitor

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercise the real dock and NavHost without creating or changing diary entries. */
@RunWith(AndroidJUnit4::class)
class TabNavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun tab(label: String) = ui.onNode(hasText(label) and hasClickAction() and isSelectable())
    private fun assertHome() {
        tab("Tổng quan").assertIsSelected()
        ui.onNodeWithText("NHẬT KÝ GIẤC NGỦ").assertIsDisplayed()
    }

    @Test fun everyTabCanReturnHomeAndRepeatedHomeTapStaysHome() {
        assertHome()
        repeat(2) {
            for (label in listOf("Ghi ngủ", "Lịch sử", "Thống kê")) {
                tab(label).performClick().assertIsSelected()
                tab("Tổng quan").performClick()
                assertHome()
                tab("Tổng quan").performClick()
                assertHome()
            }
        }
        ui.onNodeWithContentDescription("Mở Sleep AI").performClick()
        ui.onNodeWithContentDescription("Quay lại").performClick()
        assertHome()
    }

    @Test fun returningHomeKeepsTheSelectedStatisticsPeriod() {
        tab("Thống kê").performClick()
        ui.onNodeWithText("Theo tháng").performClick().assertIsSelected()
        tab("Lịch sử").performClick()
        tab("Tổng quan").performClick()
        assertHome()
        tab("Thống kê").performClick().assertIsSelected()
        ui.onNodeWithText("Theo tháng").assertIsSelected()
        tab("Tổng quan").performClick()
        assertHome()
    }

    @Test fun tabOpenedFromAssistantCanReturnHome() {
        ui.onNodeWithContentDescription("Mở Sleep AI").performClick()
        ui.onNodeWithText("Gợi ý cho tôi").performClick()
        ui.onNodeWithText("Giờ ngủ").performClick()
        ui.onNode(hasText("Mở Ghi ngủ") or hasText("Mở Lịch sử")).performClick()
        tab("Thống kê").performClick().assertIsSelected()
        tab("Tổng quan").performClick()
        assertHome()
    }

    @Test fun homeCardAndDockUseTheSameStatisticsDestination() {
        ui.onNodeWithText("Nhịp ngủ tuần này").performScrollTo().performClick()
        tab("Thống kê").assertIsSelected()
        tab("Lịch sử").performClick()
        tab("Ghi ngủ").performClick()
        tab("Tổng quan").performClick()
        assertHome()
    }

    @Test fun rapidTouchesDuringLiquidMotionKeepDockAndAssistantClickable() {
        // Use physical hit targets, not accessibility actions, while the lens is moving.
        ui.mainClock.autoAdvance = false
        try {
            for (label in listOf("Thống kê", "Ghi ngủ", "Lịch sử", "Tổng quan")) {
                tab(label).performTouchInput { click(center) }
                ui.mainClock.advanceTimeBy(80)
            }
            ui.mainClock.advanceTimeBy(1200)
            assertHome()
            ui.onNodeWithContentDescription("Mở Sleep AI").performTouchInput { click(center) }
            ui.mainClock.advanceTimeBy(600)
            ui.onNodeWithContentDescription("Quay lại").performTouchInput { click(center) }
            ui.mainClock.advanceTimeBy(600)
            assertHome()
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }
}
