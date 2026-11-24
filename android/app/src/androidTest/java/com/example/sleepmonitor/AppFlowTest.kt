package com.example.sleepmonitor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sleepmonitor.data.*
import org.junit.*
import org.junit.runner.RunWith
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String){val file=File(ui.activity.filesDir,"$name.png");file.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
    @Test fun demoSleepCycleHistoryStatisticsAndEnvironment(){
        ui.waitUntil(15000){ui.onAllNodesWithText("GIẤC NGỦ GẦN NHẤT").fetchSemanticsNodes().isNotEmpty()}
        // Finish a demo session left active by an earlier manual run before exercising Start.
        if(ui.onAllNodesWithText("THỨC DẬY").fetchSemanticsNodes().isNotEmpty()){
            ui.onNodeWithText("THỨC DẬY").performScrollTo().performClick()
            ui.waitUntil(10000){ui.onAllNodesWithText("Lưu đánh giá").fetchSemanticsNodes().isNotEmpty()}
            ui.onNodeWithText("Lưu đánh giá").performClick()
            ui.waitUntil(10000){ui.onAllNodesWithText("BẮT ĐẦU NGỦ").fetchSemanticsNodes().isNotEmpty()}
        }
        shot("01-home")
        ui.onNodeWithText("BẮT ĐẦU NGỦ").performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("THỨC DẬY").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("THỨC DẬY").performScrollTo().performClick()
        ui.waitForIdle()
        ui.waitUntil(10000){ui.onAllNodesWithText("Lưu đánh giá").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Rất tốt").performClick();ui.onNodeWithText("Lưu đánh giá").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Lưu đánh giá").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithContentDescription("Lịch sử",useUnmergedTree=true).performClick();ui.onNodeWithText("Thêm phiên ngủ").assertExists();shot("02-history")
        ui.onNodeWithContentDescription("Thống kê",useUnmergedTree=true).performClick();ui.onNodeWithText("Tháng này").performClick();ui.onNodeWithText("Trung bình / ngày có ghi nhận").assertExists();shot("03-statistics")
        ui.onNodeWithContentDescription("Môi trường",useUnmergedTree=true).performClick();ui.onNodeWithText("Đèn đầu giường").assertExists();shot("04-environment")
    }
    @Test fun sqliteSeparatesDemoAndLiveAndSurvivesReopen(){
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext;val db=LocalStore(ctx)
        val now=System.currentTimeMillis();db.put(true,SleepSession("isolation-check",now-60000,now));db.put(false,SleepSession("live-check",now-60000,now,dirty=true));db.close()
        val reopened=LocalStore(ctx)
        Assert.assertFalse(reopened.sessions(false).any{it.id=="isolation-check"})
        Assert.assertFalse(reopened.sessions(true).any{it.id=="live-check"})
        Assert.assertTrue(reopened.sessions(false).first{it.id=="live-check"}.dirty)
        reopened.resetLive();reopened.close()
    }
    @Test fun manualRecordEditingDeletionAndSettingsValidation(){
        ui.onNodeWithContentDescription("Ghi ngủ",useUnmergedTree=true).performClick()
        ui.onNodeWithText("Ghi chú").performScrollTo().performTextInput("QA manual record")
        shot("05-sleep-form")
        ui.onNodeWithText("LƯU PHIÊN NGỦ").performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Chi tiết giấc ngủ").fetchSemanticsNodes().isNotEmpty()}
        ui.waitUntil(10000){ui.onAllNodesWithText("Đã lưu phiên ngủ").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithText("QA manual record").assertExists();shot("06-detail")
        ui.onNodeWithText("Sửa phiên ngủ").performScrollTo().performClick()
        ui.onNodeWithText("Ghi chú").performScrollTo().performTextClearance()
        ui.onNodeWithText("Ghi chú").performTextInput("QA edited record")
        ui.onNodeWithText("LƯU PHIÊN NGỦ").performScrollTo().performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Chi tiết giấc ngủ").fetchSemanticsNodes().isNotEmpty()}
        ui.waitUntil(10000){ui.onAllNodesWithText("Đã lưu phiên ngủ").fetchSemanticsNodes().isEmpty()}
        ui.onNodeWithText("QA edited record").assertExists()
        ui.onNodeWithText("Xóa phiên ngủ").performScrollTo().performClick()
        ui.onNodeWithText("Xóa",useUnmergedTree=true).performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Chi tiết giấc ngủ").fetchSemanticsNodes().isEmpty()}
        val db=LocalStore(ui.activity)
        ui.waitUntil(10000){db.sessions(true).any{it.notes=="QA edited record"&&it.deleted}}
        db.close()
        ui.onNodeWithContentDescription("Cài đặt",useUnmergedTree=true).performClick();shot("07-settings")
        ui.onNodeWithText("Nhiệt độ thấp (°C)").performScrollTo().performTextClearance()
        ui.onNodeWithText("Lưu tự động hóa và ngưỡng").assertIsNotEnabled()
    }
    @Test fun emulatorCanReachRealBackendAndSyncOfflineSession(){
        val ctx=InstrumentationRegistry.getInstrumentation().targetContext;val db=LocalStore(ctx)
        val connection=Connection(demo=false);val repo=LocalSleepRepository(db,connection);val now=System.currentTimeMillis()
        val id="android-integration-$now";repo.saveSleepSession(SleepSession(id,now-8*3600000,now-1000,4,"Emulator integration test"))
        Assert.assertTrue(repo.getSleepSessions().first{it.id==id}.dirty)
        repo.synchronize();val saved=repo.getSleepSessions().first{it.id==id}
        Assert.assertFalse(saved.dirty);Assert.assertEquals(1,saved.version)
        repo.saveSleepSession(saved.copy(deleted=true));repo.synchronize();Assert.assertTrue(repo.getSleepSessions().first{it.id==id}.deleted)
        db.close()
    }
}
