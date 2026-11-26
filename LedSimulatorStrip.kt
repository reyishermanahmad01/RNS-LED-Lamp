package com.rns.controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.data.simulator.LedSimulatorEngine
import com.rns.controller.domain.model.LedModule
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsSurfaceElevated
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary
import kotlinx.coroutines.delay

@Composable
fun LedSimulatorStrip(
    module: LedModule,
    modifier: Modifier = Modifier
) {
    var timeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(module.id, module.powerOn) {
        while (true) {
            timeMs = System.currentTimeMillis()
            delay(33L) // ~30 FPS simulator tick
        }
    }

    val (leftPixels, rightPixels) = remember(module, timeMs) {
        LedSimulatorEngine.renderFrame(module, timeMs)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RnsSurfaceElevated)
            .border(1.dp, RnsBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Preview Strip LED (Kiri ${leftPixels.size} px · Kanan ${rightPixels.size} px)",
                color = RnsTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = module.driver.defaultColorOrder,
                color = RnsTextMuted,
                fontSize = 11.sp
            )
        }

        Text("Zona Kiri", color = RnsTextMuted, fontSize = 11.sp)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070A10))
        ) {
            val count = leftPixels.size.coerceAtLeast(1)
            val gap = 3f
            val ledW = ((size.width - gap * (count + 1)) / count).coerceAtLeast(2f)
            leftPixels.forEachIndexed { index, px ->
                val mixW = (px.w * 0.45f).toInt()
                val c = Color(
                    red = (px.r + mixW).coerceIn(0, 255),
                    green = (px.g + mixW).coerceIn(0, 255),
                    blue = (px.b + (mixW * 0.7f).toInt()).coerceIn(0, 255)
                )
                drawRoundRect(
                    color = c,
                    topLeft = Offset(gap + index * (ledW + gap), 4f),
                    size = Size(ledW, size.height - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        Text("Zona Kanan", color = RnsTextMuted, fontSize = 11.sp)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070A10))
        ) {
            val count = rightPixels.size.coerceAtLeast(1)
            val gap = 3f
            val ledW = ((size.width - gap * (count + 1)) / count).coerceAtLeast(2f)
            rightPixels.forEachIndexed { index, px ->
                val mixW = (px.w * 0.45f).toInt()
                val c = Color(
                    red = (px.r + mixW).coerceIn(0, 255),
                    green = (px.g + mixW).coerceIn(0, 255),
                    blue = (px.b + (mixW * 0.7f).toInt()).coerceIn(0, 255)
                )
                drawRoundRect(
                    color = c,
                    topLeft = Offset(gap + index * (ledW + gap), 4f),
                    size = Size(ledW, size.height - 8f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}
