package com.example.sleepmonitor.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.example.sleepmonitor.ui.theme.*
import kotlin.math.sin

class GlassScene(val layer:GraphicsLayer){var origin by mutableStateOf(Offset.Zero)}
val LocalGlassScene=staticCompositionLocalOf<GlassScene?>{null}

/** Record only the atmosphere. Glass panes reuse it without capturing their own text. */
@Composable fun GlassAtmosphere(modifier:Modifier=Modifier,scene:GlassScene){
    NightBackdrop(modifier.onGloballyPositioned{scene.origin=it.positionInRoot()}.drawWithContent{
        scene.layer.record{this@drawWithContent.drawContent()};drawLayer(scene.layer)
    })
}

@Composable fun LiquidGlass(modifier:Modifier=Modifier,corner:Int=12,tint:Color=Card,content:@Composable BoxScope.()->Unit){
    val dark=LocalDarkTheme.current;val accent=Purple;val scene=LocalGlassScene.current
    var position by remember{mutableStateOf(Offset.Zero)}
    val phase=ambientPhase(18000);val shape=RoundedCornerShape(corner.dp)
    Box(modifier.onGloballyPositioned{position=it.positionInRoot()}.clip(shape)){
        if(scene!=null)Canvas(Modifier.matchParentSize().graphicsLayer{
            if(Build.VERSION.SDK_INT>=31)renderEffect=BlurEffect(18.dp.toPx(),18.dp.toPx(),TileMode.Clamp)
        }){translate(scene.origin.x-position.x,scene.origin.y-position.y){drawLayer(scene.layer)}}
        Box(Modifier.matchParentSize().background(Brush.linearGradient(if(dark)listOf(tint.copy(alpha=.79f),tint.copy(alpha=.60f))else listOf(Color.White.copy(alpha=.70f),Color(0xFFE5F3FF).copy(alpha=.36f),Color.White.copy(alpha=.58f)))) )
        Canvas(Modifier.matchParentSize()){
            // A broad moving reflection and a fine bevel keep the surface glass-like.
            val x=size.width*(.5f+.55f*sin(phase.value))
            drawRect(Brush.linearGradient(listOf(Color.Transparent,Color.White.copy(alpha=if(dark).035f else .20f),Color.Transparent),Offset(x-size.width*.35f,0f),Offset(x+size.width*.1f,size.height)))
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent,Color.White.copy(alpha=if(dark).45f else .95f),Color.Transparent)),Offset(14.dp.toPx(),.7.dp.toPx()),Offset(size.width-14.dp.toPx(),.7.dp.toPx()),1.dp.toPx())
        }
        Box(Modifier.matchParentSize().border(1.dp,Brush.linearGradient(listOf(Color.White.copy(alpha=if(dark).28f else .95f),accent.copy(alpha=.10f),Color.White.copy(alpha=.38f))),shape))
        content()
    }
}
