package com.example.sleepmonitor

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.runner.RunWith
import java.io.File

/** Screenshots come from the running app. This review does not change diary records. */
@RunWith(AndroidJUnit4::class)
class VisualReviewTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String){ui.waitForIdle();android.os.SystemClock.sleep(500);File(ui.activity.filesDir,"$name.png").outputStream().use{InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)}}
    @Test fun reviewAllScreensAndKeyboard(){
        ui.waitUntil(15000){ui.onAllNodesWithText("Trong ngưỡng đã đặt").fetchSemanticsNodes().isNotEmpty()}
        shot("01-home")
        ui.onNodeWithText("Trò chuyện với trợ lý").performScrollTo();shot("01-home-assistant-card")
        ui.onNodeWithContentDescription("Môi trường",useUnmergedTree=true).performClick();shot("04-environment")
        ui.onNodeWithContentDescription("Thống kê",useUnmergedTree=true).performClick();shot("03-statistics")
        ui.onNodeWithContentDescription("Lịch sử",useUnmergedTree=true).performClick();shot("02-history")
        ui.onNodeWithContentDescription("Ghi ngủ",useUnmergedTree=true).performClick();shot("05-sleep-form")
        ui.onNodeWithContentDescription("Cài đặt",useUnmergedTree=true).performClick();shot("07-settings")
        ui.onNodeWithContentDescription("Trợ lý AI",useUnmergedTree=true).performClick()
        ui.waitUntil(15000){ui.onAllNodesWithText("Đang kiểm tra kết nối…").fetchSemanticsNodes().isEmpty()}
        shot("08-assistant")
        ui.onNode(hasSetTextAction()).performClick().performTextInput("Tôi muốn ngủ đều giờ hơn")
        ui.onNodeWithContentDescription("Gửi câu hỏi").assertIsDisplayed()
        shot("10-assistant-keyboard")
    }
}
