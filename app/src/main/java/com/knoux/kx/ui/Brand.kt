package com.knoux.kx.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Native, resolution-independent derivative; source artwork is not bundled in the APK. */
@Composable
fun KXOrb(modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    val opacity by animateFloatAsState(if (appeared) 1f else 0f, tween(320), label = "brand entrance")
    LaunchedEffect(Unit) { appeared = true }
    // Compose uses the platform duration scale; disabled animations immediately settle.
    Box(modifier.size(232.dp).graphicsLayer { alpha = opacity }.clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 240f
            scale(unit, unit, pivot = Offset.Zero) {
                val center = Offset(120f, 120f)
                drawCircle(Brush.radialGradient(
                    listOf(Cyan.copy(alpha = .30f), Violet.copy(alpha = .09f), Color.Transparent),
                    center, 120f,
                ), 120f, center)
                drawCircle(Brush.radialGradient(
                    listOf(Color.White, Pearl, Color(0xFFB2B7E8), Violet.copy(alpha = .6f)),
                    Offset(90f, 67f), 160f,
                ), 88f, center)
                drawCircle(Brush.linearGradient(listOf(Color.White, Cyan, Violet, Color.White)),
                    88f, center, style = Stroke(2f))
                rotate(-32f, center) {
                    drawOval(Brush.linearGradient(listOf(Color.Transparent, Color.White, Cyan, Color.Transparent)),
                        Offset(13f, 75f), Size(214f, 87f), style = Stroke(3f))
                }
                val ribbon = Path().apply {
                    moveTo(120f, 50f)
                    cubicTo(140f, 68f, 158f, 70f, 178f, 70f)
                    cubicTo(176f, 130f, 153f, 163f, 120f, 187f)
                    cubicTo(84f, 165f, 63f, 131f, 62f, 70f)
                    cubicTo(87f, 70f, 104f, 62f, 120f, 50f)
                    close()
                }
                drawPath(ribbon, Brush.linearGradient(
                    listOf(Color.White.copy(alpha = .9f), Cyan.copy(alpha = .45f), Violet.copy(alpha = .65f)),
                    Offset(65f, 55f), Offset(164f, 187f),
                ))
                drawPath(ribbon, Color.White.copy(alpha = .9f), style = Stroke(1.5f))
                val fold = Path().apply {
                    moveTo(65f, 74f); cubicTo(106f, 97f, 129f, 143f, 120f, 185f)
                    cubicTo(145f, 156f, 157f, 110f, 174f, 72f)
                }
                drawPath(fold, Brush.linearGradient(listOf(Color.White.copy(alpha = .6f), Violet.copy(alpha = .2f))))
                listOf(Offset(41f, 68f), Offset(204f, 103f), Offset(102f, 209f)).forEach {
                    drawCircle(Brush.radialGradient(listOf(Color.White, Cyan, Violet), it, 8f), 7f, it)
                }
                drawArc(Color.White.copy(alpha = .75f), 205f, 85f, false,
                    Offset(42f, 42f), Size(156f, 156f), style = Stroke(3f))
            }
        }
        Text("KX", color = Color(0xFF452783), fontSize = 42.sp,
            fontWeight = FontWeight.Black, letterSpacing = (-3).sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier, shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .92f)),
        border = BorderStroke(1.dp, Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.outline.copy(alpha = .4f), Cyan.copy(alpha = .3f)),
        )),
    ) { content() }
}

@Composable
fun KXButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick, modifier.defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    ) { Text(label) }
}

@Composable
fun BrandBackdrop(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier.background(background).drawBehind {
        drawCircle(Brush.radialGradient(listOf(Violet.copy(alpha = .12f), Color.Transparent),
            Offset(size.width * .9f, size.height * .2f), size.width), size.width,
            Offset(size.width * .9f, size.height * .2f))
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .09f), Color.Transparent),
            Offset(0f, size.height * .7f), size.width), size.width,
            Offset(0f, size.height * .7f))
    }, content = content)
}
