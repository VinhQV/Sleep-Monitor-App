package com.example.sleepmonitor.ui.theme

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sleepmonitor.data.*
import kotlinx.coroutines.delay
import java.time.LocalTime

@Immutable
data class SleepColors(
    val purple: Color, val teal: Color, val ink: Color, val muted: Color,
    val background: Color, val card: Color, val raised: Color, val lavender: Color,
    val border: Color, val amber: Color, val rose: Color
)
private val NightColors = SleepColors(
    Color(0xFFC4ACFF), Color(0xFF7DE3D4), Color(0xFFF2F0FF), Color(0xFF9EA9C2),
    Color(0xFF080C1E), Color(0xFF181B32), Color(0xFF24283F), Color(0xFF2B2448),
    Color(0xFF3C3B5C), Color(0xFFFFD49B), Color(0xFFFFA8B7)
)
private val DayColors = SleepColors(
    Color(0xFF246E9C), Color(0xFF157E91), Color(0xFF202C45), Color(0xFF66728A),
    Color(0xFFF7FBFE), Color(0xFFFFFFFF), Color(0xFFEDF5FA), Color(0xFFE4F2FC),
    Color(0xFFD7E5ED), Color(0xFF9B682D), Color(0xFFAF4865)
)
val LocalSleepColors = staticCompositionLocalOf { NightColors }
val LocalDarkTheme = staticCompositionLocalOf { true }
val LocalMotionEnabled = staticCompositionLocalOf { true }
val Purple: Color @Composable get() = LocalSleepColors.current.purple
val Teal: Color @Composable get() = LocalSleepColors.current.teal
val Ink: Color @Composable get() = LocalSleepColors.current.ink
val Muted: Color @Composable get() = LocalSleepColors.current.muted
val Background: Color @Composable get() = LocalSleepColors.current.background
val Card: Color @Composable get() = LocalSleepColors.current.card
val CardRaised: Color @Composable get() = LocalSleepColors.current.raised
val Lavender: Color @Composable get() = LocalSleepColors.current.lavender
val Border: Color @Composable get() = LocalSleepColors.current.border
val Amber: Color @Composable get() = LocalSleepColors.current.amber
val Rose: Color @Composable get() = LocalSleepColors.current.rose

@Composable
fun SleepTheme(motionEnabled: Boolean = true, appearance: AppearanceMode = AppearanceMode.AUTOMATIC,
               content: @Composable () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var active by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(active) { while (active) { hour = LocalTime.now().hour; delay(30000) } }
    val dark = usesDarkTheme(appearance, hour)
    val moving = motionEnabled && active && ValueAnimator.areAnimatorsEnabled()
    val target = if (dark) NightColors else DayColors
    @Composable fun shade(color: Color): Color = animateColorAsState(color, tween(if (moving) 550 else 0), label = "day-night-color").value
    val colors = SleepColors(shade(target.purple), shade(target.teal), shade(target.ink), shade(target.muted),
        shade(target.background), shade(target.card), shade(target.raised), shade(target.lavender),
        shade(target.border), shade(target.amber), shade(target.rose))
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            @Suppress("DEPRECATION")
            activity.window.apply {
                statusBarColor = colors.background.toArgb()
                navigationBarColor = colors.background.toArgb()
                setBackgroundDrawable(ColorDrawable(colors.background.toArgb()))
            }
        }
    }
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalSleepColors provides colors, LocalDarkTheme provides dark,
        LocalMotionEnabled provides moving, LocalContentColor provides colors.ink) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = colors.purple, onPrimary = if (dark) colors.background else Color.White,
                primaryContainer = colors.lavender, onPrimaryContainer = colors.ink,
                secondary = colors.teal, onSecondary = if (dark) colors.background else Color.White,
                background = colors.background, onBackground = colors.ink,
                surface = colors.card, onSurface = colors.ink, surfaceVariant = colors.raised,
                onSurfaceVariant = colors.muted, outline = colors.border,
                error = colors.rose, onError = if (dark) colors.background else Color.White, surfaceTint = Color.Transparent
            ),
            typography = Typography(
                headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 39.sp, letterSpacing = (-1).sp),
                headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-.7).sp),
                titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp),
                titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
                bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
                bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 21.sp),
                labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp)
            ),
            shapes = Shapes(extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(12.dp), extraLarge = RoundedCornerShape(16.dp)),
            content = content
        )
    }
}
