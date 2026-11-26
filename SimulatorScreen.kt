package com.rns.controller.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.data.protocol.RnsProtocolCodec
import com.rns.controller.data.simulator.LedSimulatorEngine
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.CustomDirection
import com.rns.controller.domain.model.CustomTransitionStyle
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.ui.components.LedSimulatorStrip
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsNightBottom
import com.rns.controller.ui.theme.RnsNightTop
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSuccess
import com.rns.controller.ui.theme.RnsTextMuted
import kotlinx.coroutines.delay

@Composable
fun SimulatorScreen(
    activeModule: LedModule,
    customSegmentColors: List<RgbwColor?>,
    onSelectDirection: (CustomDirection) -> Unit,
    onSelectStyle: (CustomTransitionStyle) -> Unit,
    onPaintSegment: (Int, RgbwColor?) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onToggleLeftTest: () -> Unit,
    onToggleRightTest: () -> Unit,
    onToggleCustomColorMode: (Boolean) -> Unit,
    onToggleCombinationAutoCycle: (Boolean) -> Unit,
    onActivateCombinationStep: (Int) -> Unit
) {
    var timeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var brushColor by remember { mutableStateOf<RgbwColor?>(RgbwColor(0, 180, 255, 0)) }

    LaunchedEffect(activeModule.id, activeModule.powerOn) {
        while (true) {
            timeMs = System.currentTimeMillis()
            delay(33L)
        }
    }

    val serpentinePixels = remember(activeModule, customSegmentColors, timeMs) {
        LedSimulatorEngine.renderCustomSerpentineSegments(activeModule, timeMs, customSegmentColors, 24)
    }

    val leftAnim = AnimationCatalog.byId(activeModule.leftZone.animationId)
    val rightAnim = AnimationCatalog.byId(
        if (activeModule.splitZonesIndependent) activeModule.rightZone.animationId else activeModule.leftZone.animationId
    )
    val hexFrame = RnsProtocolCodec.encodeCompactBinary(activeModule)
        .joinToString(" ") { "%02X".format(it) }

    val paintSwatches = listOf(
        RgbwColor(255, 30, 30, 0),
        RgbwColor(255, 160, 0, 0),
        RgbwColor(0, 220, 90, 0),
        RgbwColor(0, 180, 255, 0),
        RgbwColor(180, 50, 255, 0),
        RgbwColor(255, 255, 255, 255),
        null // Eraser
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RnsNightTop, Color(0xFF0A1B56), RnsNightBottom)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LED LAMP Foto 15 Direction Selector: Forward | Reverse | Change | Cycle
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CustomDirection.entries.forEach { dir ->
                val selected = activeModule.customDirection == dir
                Button(
                    onClick = { onSelectDirection(dir) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF0084D8) else Color.Black.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(dir.label, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        // LED LAMP Foto 15 Interactive Serpentine S-Curve Custom Strip
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Serpentine S-Curve Preview (Tap Segmen)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text("Tanpa Wemos", color = RnsSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            // 5-Row U-Turn Segmented Serpentine Track matching LED LAMP (12-34-35-603)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                val rows = 5
                val cols = 5
                val padX = 24f
                val padY = 18f
                val gap = 6f
                val stepY = (size.height - padY * 2) / (rows - 1)
                val segW = (size.width - padX * 2 - gap * (cols - 1)) / cols
                val segH = 14f

                var idx = 0
                for (r in 0 until rows) {
                    val isLtoR = r % 2 == 0
                    val y = padY + r * stepY - segH / 2f
                    if (r < rows - 1) {
                        val turnX = if (isLtoR) size.width - padX else padX
                        val cy = padY + r * stepY + stepY / 2f
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = if (isLtoR) -90f else 90f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(turnX - stepY / 2f, cy - stepY / 2f),
                            size = androidx.compose.ui.geometry.Size(stepY, stepY),
                            style = Stroke(width = segH, cap = StrokeCap.Butt)
                        )
                    }
                    for (step in 0 until cols) {
                        if (idx >= 24) break
                        val c = if (isLtoR) step else cols - 1 - step
                        val x = padX + c * (segW + gap)
                        val px = serpentinePixels.getOrNull(idx)
                        val ledColor = if (px == null) {
                            Color(0xFF1E293B)
                        } else {
                            Color(px.r.coerceIn(0, 255), px.g.coerceIn(0, 255), px.b.coerceIn(0, 255))
                        }
                        drawRoundRect(
                            color = ledColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(segW, segH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        idx++
                    }
                }
            }

            // Paint Swatches + Clickable Segment Selector Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                paintSwatches.forEach { sw ->
                    val isBrush = brushColor == sw
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (sw != null) Color(sw.r, sw.g, sw.b) else Color(0xFF334155))
                            .border(
                                width = if (isBrush) 2.5.dp else 1.dp,
                                color = if (isBrush) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { brushColor = sw },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sw == null) {
                            Text("X", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                OutlinedButton(onClick = {
                    (0 until 24).forEach { onPaintSegment(it, brushColor) }
                }) {
                    Text("Isi Semua", fontSize = 10.sp, color = Color.White)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (0 until 24).forEach { segIdx ->
                    val seg = customSegmentColors.getOrNull(segIdx)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (seg != null) Color(seg.r, seg.g, seg.b) else Color(0xFF1E293B)
                            )
                            .clickable { onPaintSegment(segIdx, brushColor) }
                    )
                }
            }
        }

        // Dual Strip Left & Right Preview
        LedSimulatorStrip(module = activeModule)

        // Custom Color Status + Left/Right Palettes
        val leftPal = RnsColorPresets.normalizePalette(activeModule.leftZone)
        val rightPal = RnsColorPresets.normalizePalette(
            if (activeModule.splitZonesIndependent) activeModule.rightZone else activeModule.leftZone
        )
        val isCustomColorOn = activeModule.leftZone.customColorEnabled || activeModule.rightZone.customColorEnabled
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, RnsBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("L:", color = RnsTextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    leftPal.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(c.r, c.g, c.b))
                        )
                    }
                }
                Text("R:", color = RnsTextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    rightPal.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(c.r, c.g, c.b))
                        )
                    }
                }
            }
            Text(
                text = if (isCustomColorOn) "Kustom Warna: ON" else "Warna Asli Efek",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCustomColorOn) Color(0xFF0084D8) else Color.White.copy(alpha = 0.14f))
                    .clickable { onToggleCustomColorMode(!isCustomColorOn) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Kombinasi Aktif Quick Bar (Prev / Next / Auto)
        if (activeModule.combinationSteps.isNotEmpty()) {
            val activeIdx = activeModule.combinationStepIndex.coerceIn(0, activeModule.combinationSteps.lastIndex)
            val activeStep = activeModule.combinationSteps[activeIdx]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, if (activeModule.combinationEnabled) Color(0xFF38BDF8) else RnsBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kombinasi #${activeIdx + 1}/${activeModule.combinationSteps.size}: ${activeStep.label}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (activeModule.combinationAutoCycle) "Auto-Cycle Kombinasi ON" else "Kombinasi Manual",
                        color = if (activeModule.combinationAutoCycle) RnsSuccess else RnsTextMuted,
                        fontSize = 10.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Prev",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onActivateCombinationStep(activeIdx - 1) }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                    Text(
                        text = "Next",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable { onActivateCombinationStep(activeIdx + 1) }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                    Text(
                        text = if (activeModule.combinationAutoCycle) "Auto ON" else "Auto",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeModule.combinationAutoCycle) Color(0xFF0084D8) else Color.White.copy(alpha = 0.12f))
                            .clickable { onToggleCombinationAutoCycle(!activeModule.combinationAutoCycle) }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleLeftTest, modifier = Modifier.weight(1f)) {
                Text("Uji Kiri: ${if (activeModule.leftZone.enabled) "ON" else "OFF"}", fontSize = 11.sp, color = Color.White)
            }
            OutlinedButton(onClick = onToggleRightTest, modifier = Modifier.weight(1f)) {
                Text("Uji Kanan: ${if (activeModule.rightZone.enabled) "ON" else "OFF"}", fontSize = 11.sp, color = Color.White)
            }
        }

        // LED LAMP 12-35-22-712 Quick Style Bar: Jump | Breathe | Flash | Gradient
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "Jump" to CustomTransitionStyle.FS,
                "Breathe" to CustomTransitionStyle.BREATHE,
                "Flash" to CustomTransitionStyle.FD,
                "Gradient" to CustomTransitionStyle.GD
            ).forEach { (label, style) ->
                val selected = activeModule.customStyle == style
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Color.White else Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .clickable { onSelectStyle(style) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color(0xFF0F172A) else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // LED LAMP 12-34-35-603: 2x4 Grid GD / FD / FW / FS / AC / PU / Breathe / HO
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CustomTransitionStyle.entries.chunked(4).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { style ->
                        val selected = activeModule.customStyle == style
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFF0084D8) else Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, if (selected) Color.White else RnsBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectStyle(style) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(style.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // LED LAMP Foto 15: Green Speed & White Brightness Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .border(1.dp, RnsBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Speed", color = Color.White, fontSize = 12.sp)
                Text("${activeModule.speed}%", color = Color(0xFF22C55E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = activeModule.speed.toFloat(),
                onValueChange = { onSpeedChanged(it.toInt()) },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF22C55E), activeTrackColor = Color(0xFF22C55E))
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness", color = Color.White, fontSize = 12.sp)
                Text("${(activeModule.masterBrightness * 100 / 255)}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = activeModule.masterBrightness.toFloat(),
                onValueChange = { onBrightnessChanged(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
            )
        }

        // Telemetry & 24-Byte Frame
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.32f))
                .border(1.dp, RnsBorder, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val modeText = if (activeModule.effectEngine == EffectEngineMode.DMX210) {
                "210 DMX · #${activeModule.dmxModeId} ${AnimationCatalog.dmxById(activeModule.dmxModeId).name}"
            } else {
                "L #${leftAnim.id} ${leftAnim.name} · R #${rightAnim.id} ${rightAnim.name}"
            }
            Text("Driver: ${activeModule.driver.displayName} (${activeModule.driver.defaultColorOrder})", color = RnsTextMuted, fontSize = 11.sp)
            Text(modeText, color = RnsPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(hexFrame, color = RnsTextMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
    }
}
