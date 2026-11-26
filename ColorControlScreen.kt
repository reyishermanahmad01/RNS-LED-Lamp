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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.ColorConfigPreset
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.domain.model.ZoneTarget
import com.rns.controller.ui.components.ColorWheelCanvas
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsNightBottom
import com.rns.controller.ui.theme.RnsNightTop
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSuccess
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary

@Composable
fun ColorControlScreen(
    activeModule: LedModule,
    selectedZoneTarget: ZoneTarget,
    selectedColorChannel: Int,
    colorPresets: List<ColorConfigPreset>,
    deviceCount: Int,
    onSelectZoneTarget: (ZoneTarget) -> Unit,
    onSelectColorChannel: (Int) -> Unit,
    onToggleCustomColorMode: (Boolean) -> Unit,
    onApplyColorPreset: (ColorConfigPreset) -> Unit,
    onSaveColorPreset: (String) -> Unit,
    onDeleteColorPreset: (String) -> Unit,
    onTogglePower: () -> Unit,
    onColorChanged: (RgbwColor) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onOpenGroupsDrawer: () -> Unit,
    onOpenFocusDrawer: () -> Unit
) {
    var modeTab by remember { mutableIntStateOf(0) } // 0 = RING, 1 = Aisle, 2 = DIM
    var presetNameInput by remember { mutableStateOf("") }
    val targetZone = when (selectedZoneTarget) {
        ZoneTarget.RIGHT -> activeModule.rightZone
        else -> activeModule.leftZone
    }
    val palette = RnsColorPresets.normalizePalette(targetZone)
    val safeCh = selectedColorChannel.coerceIn(0, 3)
    val activeZoneColor = palette[safeCh]
    val customModeOn = if (selectedZoneTarget == ZoneTarget.BOTH) {
        activeModule.leftZone.customColorEnabled || activeModule.rightZone.customColorEnabled
    } else {
        targetZone.customColorEnabled
    }

    val solidPresets = listOf(
        RgbwColor(255, 0, 0, 0),
        RgbwColor(0, 255, 0, 0),
        RgbwColor(0, 60, 255, 0),
        RgbwColor(255, 255, 0, 0),
        RgbwColor(0, 240, 255, 0),
        RgbwColor(220, 0, 255, 0)
    )
    val customSaved = remember {
        mutableStateListOf<RgbwColor?>(null, null, null, null, null, null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RnsNightTop, Color(0xFF0A1B56), RnsNightBottom)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LED LAMP 12-33-59-976 Header: Menu+Count | [RING | Aisle | DIM] | Power + Gear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenGroupsDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Groups", tint = Color.White)
                }
                Text(
                    text = "$deviceCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
            ) {
                listOf("RING", "Aisle", "DIM").forEachIndexed { idx, label ->
                    val selected = modeTab == idx
                    Box(
                        modifier = Modifier
                            .background(if (selected) Color.White else Color.Black.copy(alpha = 0.35f))
                            .clickable { modeTab = idx }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color(0xFF0F172A) else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePower) {
                    Icon(
                        Icons.Rounded.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = if (activeModule.powerOn) RnsSuccess else Color(0xFFEF4444)
                    )
                }
                IconButton(onClick = onOpenFocusDrawer) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Set Focus", tint = Color.White)
                }
            }
        }

        // Active Effect Lock Banner + Custom Color Mode Toggle
        val lockedPatternLabel = if (activeModule.effectEngine == EffectEngineMode.DMX210) {
            "DMX #${activeModule.dmxModeId} · ${AnimationCatalog.dmxById(activeModule.dmxModeId).name}"
        } else {
            val slot = AnimationCatalog.byId(targetZone.animationId)
            "Animasi #${slot.id} · ${slot.name}"
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = RnsSecondary, modifier = Modifier.size(16.dp))
                    Column {
                        Text(lockedPatternLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Pola animasi terkunci — ubah warna tidak mengganti efek", color = RnsTextMuted, fontSize = 10.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(false to "Warna Asli Efek", true to "Kustom Penuh").forEach { (isCustom, lbl) ->
                    val selected = customModeOn == isCustom
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF0084D8) else Color.White.copy(alpha = 0.08f))
                            .border(1.dp, if (selected) Color.White else RnsBorder, RoundedCornerShape(8.dp))
                            .clickable { onToggleCustomColorMode(isCustom) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lbl, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // 4 Multi-Color Channels (Utama 1..Aksen 4)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Utama 1", "Kedua 2", "Aksen 3", "Aksen 4").forEachIndexed { chIdx, lbl ->
                    val c = palette[chIdx]
                    val isSel = safeCh == chIdx
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.25f))
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) Color(0xFF38BDF8) else RnsBorder,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectColorChannel(chIdx) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(c.r, c.g, c.b))
                                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        )
                        Text(lbl, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }

        // Zone Target Selector (All / Left / Right)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val items = listOf(
                ZoneTarget.BOTH to "Semua (L+R)",
                ZoneTarget.LEFT to "Zona Kiri",
                ZoneTarget.RIGHT to "Zona Kanan"
            )
            items.forEachIndexed { index, (target, label) ->
                SegmentedButton(
                    selected = selectedZoneTarget == target,
                    onClick = { onSelectZoneTarget(target) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size)
                ) {
                    Text(label, fontSize = 11.sp)
                }
            }
        }

        when (modeTab) {
            0 -> {
                // LED LAMP 12-33-59-976: RING Color Wheel + R/G/B stacked badge + rainbow reset + 6 solid + 6 '+' custom slots
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ColorWheelCanvas(
                        selectedColor = activeZoneColor,
                        size = 220.dp,
                        onColorSelected = onColorChanged
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        ) {
                            RgbValueBadge("R", activeZoneColor.r, Color(0xFFFF0000))
                            RgbValueBadge("G", activeZoneColor.g, Color(0xFF00E500))
                            RgbValueBadge("B", activeZoneColor.b, Color(0xFF0033FF))
                        }

                        // Mini Rainbow Ring Reset to White
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.Red,
                                            Color.Yellow,
                                            Color.Green,
                                            Color.Cyan,
                                            Color.Blue,
                                            Color.Magenta,
                                            Color.Red
                                        )
                                    )
                                )
                                .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                .clickable {
                                    onColorChanged(
                                        RgbwColor(255, 255, 255, if (activeModule.driver.supportsWhiteChannel) 255 else 0)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    // 6 Solid Preset Rounded-Square Tiles
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        solidPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(preset.r, preset.g, preset.b))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                    .clickable { onColorChanged(preset) }
                            )
                        }
                    }

                    // 6 '+' Custom Save Slots (Rounded Square)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        customSaved.forEachIndexed { idx, saved ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (saved != null) Color(saved.r, saved.g, saved.b) else Color.Transparent
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (saved == null) {
                                            customSaved[idx] = activeZoneColor
                                        } else {
                                            onColorChanged(saved)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (saved == null) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Foto 17: Aisle R/G/B/W Independent Channel Sliders
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Aisle · Kanal RGB Independen", color = RnsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    ChannelSlider("Red (R)", activeZoneColor.r, Color(0xFFEF4444)) {
                        onColorChanged(activeZoneColor.copy(r = it))
                    }
                    ChannelSlider("Green (G)", activeZoneColor.g, Color(0xFF22C55E)) {
                        onColorChanged(activeZoneColor.copy(g = it))
                    }
                    ChannelSlider("Blue (B)", activeZoneColor.b, Color(0xFF3B82F6)) {
                        onColorChanged(activeZoneColor.copy(b = it))
                    }
                    if (activeModule.driver.supportsWhiteChannel) {
                        ChannelSlider("White (W — SK6812 RGBW)", activeZoneColor.w, Color(0xFFFDE68A)) {
                            onColorChanged(activeZoneColor.copy(w = it))
                        }
                    } else {
                        Text(
                            text = "Kanal White (W) aktif saat driver modul diatur ke SK6812 RGBW.",
                            color = RnsTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            2 -> {
                // Foto 16: DIM Circular Arc Dial Gauge
                val pct = (activeModule.masterBrightness * 100 / 255)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 20f
                            val arcSize = Size(size.width - stroke, size.height - stroke)
                            val topLeft = Offset(stroke / 2f, stroke / 2f)
                            drawArc(
                                color = Color.White.copy(alpha = 0.18f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = Color(0xFF38BDF8),
                                startAngle = 135f,
                                sweepAngle = 270f * (activeModule.masterBrightness / 255f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$pct%", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text("DIMMER", color = RnsTextMuted, fontSize = 11.sp)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf(10, 25, 50, 75, 100).forEach { p ->
                            Text(
                                text = "$p%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(1.dp, RnsBorder, RoundedCornerShape(10.dp))
                                    .clickable { onBrightnessChanged((p * 255) / 100) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Master Brightness & Speed Sliders (always accessible on Color tab)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${(activeModule.masterBrightness * 100 / 255)}%", color = RnsPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = activeModule.masterBrightness.toFloat(),
                onValueChange = { onBrightnessChanged(it.toInt()) },
                valueRange = 0f..255f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF38BDF8))
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kecepatan Animasi (Terpisah)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${activeModule.speed}%", color = Color(0xFF22C55E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = activeModule.speed.toFloat(),
                onValueChange = { onSpeedChanged(it.toInt()) },
                valueRange = 1f..100f,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF22C55E))
            )
        }

        // Preset Konfigurasi Warna & Simpan Konfigurasi
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.28f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Preset & Simpan Konfigurasi Warna", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            colorPresets.forEach { preset ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, RnsBorder, RoundedCornerShape(10.dp))
                        .clickable { onApplyColorPreset(preset) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(preset.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (preset.splitZonesIndependent) "Kiri & Kanan Independen" else "Sinkron L+R",
                            color = RnsTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        preset.leftPalette.take(4).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.r, c.g, c.b))
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                        if (!preset.builtIn) {
                            IconButton(
                                onClick = { onDeleteColorPreset(preset.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Hapus", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    placeholder = { Text("Nama konfigurasi warna...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            onSaveColorPreset(presetNameInput)
                            presetNameInput = ""
                        }
                    }
                ) {
                    Text("Simpan", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RgbValueBadge(label: String, value: Int, accent: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(accent)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(value.toString().padStart(3, ' '), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun ChannelSlider(label: String, value: Int, accent: Color, onChange: (Int) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = RnsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value.toString(), color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
        )
    }
}
