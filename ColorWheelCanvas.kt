package com.rns.controller.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rns.controller.domain.model.RgbwColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun ColorWheelCanvas(
    selectedColor: RgbwColor,
    size: Dp = 230.dp,
    onColorSelected: (RgbwColor) -> Unit
) {
    val hueSat = remember(selectedColor) {
        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(selectedColor.r, selectedColor.g, selectedColor.b, hsv)
        hsv[0] to hsv[1]
    }
    var pointerRatio by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .pointerInput(selectedColor.w) {
                    fun handleTouch(pos: Offset) {
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val radius = min(cx, cy) - 10f
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        val dist = hypot(dx, dy).coerceAtMost(radius)
                        val angleRad = atan2(dy, dx)
                        var deg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (deg < 0f) deg += 360f
                        val sat = (dist / radius).coerceIn(0f, 1f)
                        pointerRatio = Offset(
                            cos(angleRad) * sat,
                            sin(angleRad) * sat
                        )
                        val argb = AndroidColor.HSVToColor(floatArrayOf(deg, sat, 1f))
                        onColorSelected(
                            RgbwColor(
                                r = AndroidColor.red(argb),
                                g = AndroidColor.green(argb),
                                b = AndroidColor.blue(argb),
                                w = selectedColor.w
                            )
                        )
                    }
                    detectTapGestures { handleTouch(it) }
                }
                .pointerInput(selectedColor.w) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val cx = this.size.width / 2f
                        val cy = this.size.height / 2f
                        val radius = min(cx, cy) - 10f
                        val dx = change.position.x - cx
                        val dy = change.position.y - cy
                        val dist = hypot(dx, dy).coerceAtMost(radius)
                        val angleRad = atan2(dy, dx)
                        var deg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (deg < 0f) deg += 360f
                        val sat = (dist / radius).coerceIn(0f, 1f)
                        pointerRatio = Offset(
                            cos(angleRad) * sat,
                            sin(angleRad) * sat
                        )
                        val argb = AndroidColor.HSVToColor(floatArrayOf(deg, sat, 1f))
                        onColorSelected(
                            RgbwColor(
                                r = AndroidColor.red(argb),
                                g = AndroidColor.green(argb),
                                b = AndroidColor.blue(argb),
                                w = selectedColor.w
                            )
                        )
                    }
                }
        ) {
            val radius = this.size.minDimension / 2f - 10f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red
                    ),
                    center = center
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            val rad = Math.toRadians(hueSat.first.toDouble())
            val sat = hueSat.second
            val knobPos = Offset(
                center.x + (cos(rad) * sat * radius).toFloat(),
                center.y + (sin(rad) * sat * radius).toFloat()
            )
            drawCircle(
                color = Color(selectedColor.r, selectedColor.g, selectedColor.b),
                radius = 13f,
                center = knobPos
            )
            drawCircle(
                color = Color.White,
                radius = 14f,
                center = knobPos,
                style = Stroke(width = 3.5f)
            )
        }
    }
}
