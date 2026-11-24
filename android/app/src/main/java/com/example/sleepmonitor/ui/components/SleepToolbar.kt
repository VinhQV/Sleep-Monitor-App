package com.example.sleepmonitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepmonitor.ui.theme.*

/** The same compact glass toolbar remains available on every destination. */
@Composable fun SleepToolbar(route: String, root: Boolean, back: () -> Unit, toggleTheme: () -> Unit, settings: () -> Unit) {
    val dark = LocalDarkTheme.current
    Box(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp)) {
        LiquidToolbarSurface(Modifier.fillMaxWidth(), corner = 28) {
            Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!root) LiquidIconButton(Icons.Outlined.ArrowBack, "Quay lại", back)
                else Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Icon(if (dark) Icons.Outlined.NightsStay else Icons.Outlined.WbSunny, null,
                        Modifier.size(18.dp), tint = if (dark) Purple else Amber)
                }
                Text(when {
                    route == "chat" -> "Sleep AI"
                    route == "settings" -> "Cài đặt"
                    route == "env" -> "Không gian ngủ"
                    !root -> "Nhật ký của bạn"
                    else -> "NHẬT KÝ GIẤC NGỦ"
                }, Modifier.weight(1f), color = Ink,
                    fontSize = if (root) 10.sp else 14.sp, letterSpacing = if (root) 1.sp else 0.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                LiquidIconButton(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    if (dark) "Chuyển sang giao diện sáng" else "Chuyển sang giao diện tối", toggleTheme)
                if (route != "settings") LiquidIconButton(Icons.Outlined.Settings, "Cài đặt", settings)
            }
        }
    }
}
