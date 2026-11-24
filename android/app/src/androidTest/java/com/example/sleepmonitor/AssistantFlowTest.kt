package com.example.sleepmonitor

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.viewmodel.AppState
import org.junit.*
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File

@RunWith(AndroidJUnit4::class)
class AssistantFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String){File(ui.activity.filesDir,"$name.png").outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
    private fun open(){
        ui.onNodeWithContentDescription("Trợ lý AI",useUnmergedTree=true).performClick()
        ui.waitUntil(15000){ui.onAllNodesWithText("Ollama · Sẵn sàng").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun send(text:String){
        ui.onNode(hasSetTextAction()).performTextInput(text)
        ui.onNodeWithContentDescription("Gửi câu hỏi").performClick()
    }
    @Test fun realHttpChatOptInRetryCancellationAndClear(){
        open();shot("08-assistant-welcome")
        send("Giúp tôi ngủ tốt hơn")
        ui.waitUntil(15000){ui.onAllNodesWithText("Không có dữ liệu nhật ký.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        ui.onNode(isToggleable()).performClick()
        ui.onNodeWithText("Cho phép").performClick()
        send("Nhìn lại giấc ngủ 7 ngày qua")
        ui.waitUntil(15000){ui.onAllNodesWithText("Có tóm tắt được cho phép.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        shot("09-assistant-http-test")
        send("TEST_ERROR")
        ui.waitUntil(15000){ui.onAllNodesWithText("Thử lại").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Thử lại").performScrollTo().performClick()
        ui.waitUntil(15000){ui.onAllNodesWithText("Thử lại").fetchSemanticsNodes().isNotEmpty()}
        send("TEST_CANCEL")
        ui.waitUntil(5000){ui.onAllNodesWithContentDescription("Dừng trả lời").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Dừng trả lời").performClick()
        ui.waitUntil(5000){ui.onAllNodesWithText("Đã dừng trả lời.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithContentDescription("Xóa hội thoại").performClick();ui.onNodeWithText("Xóa",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Gợi ý thói quen thư giãn trước khi ngủ").assertExists()
        ui.onNodeWithContentDescription("Xóa hội thoại").assertDoesNotExist()
        ui.onNode(isToggleable()).performClick();ui.onNodeWithText("Tắt chia sẻ").performClick()
    }
    @Test fun contextContainsOnlySevenDaysAndNeverPrivateNotesOrDeletedSessions(){
        val now=System.currentTimeMillis()
        val state=AppState(sessions=listOf(SleepSession("private-id",now-3600000,now,4,"PRIVATE SECRET"),SleepSession("deleted",now-3600000,now,3,deleted=true),SleepSession("old",now-20*86400000,now-19*86400000,1)),environment=Environment(current=SensorSample("private-sensor",now,25.0,60.0,20.0,"device")))
        val json=assistantContext(state);val raw=json.toString()
        Assert.assertFalse(raw.contains("PRIVATE"));Assert.assertFalse(raw.contains("private-id"));Assert.assertFalse(raw.contains("private-sensor"))
        Assert.assertEquals(1,json.getJSONArray("days").length());Assert.assertEquals(60,json.getJSONArray("days").getJSONObject(0).getInt("minutes"));Assert.assertEquals("demo",json.getString("source"))
    }
}
