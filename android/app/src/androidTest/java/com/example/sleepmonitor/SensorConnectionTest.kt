package com.example.sleepmonitor

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sleepmonitor.data.*
import com.example.sleepmonitor.ui.environment.EnvironmentScreen
import com.example.sleepmonitor.ui.theme.SleepTheme
import com.example.sleepmonitor.viewmodel.AppState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorConnectionTest {
    @get:Rule val ui=createComposeRule()

    @Test fun adcReadingsDisappearWhenSwitchTurnsOffAndReturnOnReconnect() {
        val now=System.currentTimeMillis()
        val sample=SensorSample("adc-test",now,26.5,55.0,null,"device",1234.0,994.0)
        val state=mutableStateOf(AppState(
            connection=Connection(demo=false),serverReachable=true,now=now,
            environment=Environment(current=sample,samples=listOf(sample),
                status=DeviceStatus(online=true,sensorOnly=true))))
        ui.setContent { SleepTheme(false) { EnvironmentScreen(state.value) {} } }
        ui.onNodeWithText("1234").assertExists()
        ui.onNodeWithText("ADC").assertExists()
        ui.onNodeWithText("AO 994 mV · ADC 12-bit, thang 0–4095").assertExists()
        ui.runOnIdle {
            state.value=state.value.copy(environment=state.value.environment.copy(
                status=state.value.environment.status.copy(online=false)))
        }
        ui.onNodeWithText("Mất kết nối thiết bị").assertExists()
        ui.onAllNodesWithText("—").assertCountEquals(3)
        ui.runOnIdle {
            state.value=state.value.copy(environment=state.value.environment.copy(
                status=state.value.environment.status.copy(online=true)))
        }
        ui.onNodeWithText("1234").assertExists()
        ui.onNodeWithText("Mất kết nối thiết bị").assertDoesNotExist()
    }
}
