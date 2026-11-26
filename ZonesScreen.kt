package com.rns.controller.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.domain.model.ZoneConfig
import com.rns.controller.domain.model.ZoneTarget
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSurface
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary

@Composable
fun ZonesScreen(
    activeModule: LedModule,
    onToggleSplitMode: (Boolean) -> Unit,
    onUpdateLeft: (enabled: Boolean?, brightness: Int?, pixelCount: Int?) -> Unit,
    onUpdateRight: (enabled: Boolean?, brightness: Int?, pixelCount: Int?) -> Unit,
    onPickColorForZone: (ZoneTarget, Int, RgbwColor) -> Unit,
    onToggleZoneCustomColor: (ZoneTarget, Boolean) -> Unit,
    onPickAnimForZone: (ZoneTarget, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RnsSurface)
                .border(1.dp, RnsBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kombinasi Kanan & Kiri Terpisah",
                    color = RnsTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (activeModule.splitZonesIndependent) {
                        "Aktif: Zona Kiri dan Zona Kanan diatur independen"
                    } else {
                        "Sinkron: Zona Kanan mengikuti Zona Kiri"
                    },
                    color = RnsTextMuted,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = activeModule.splitZonesIndependent,
                onCheckedChange = onToggleSplitMode
            )
        }

        ZoneEditorCard(
            title = "Zona Lampu Kiri (L)",
            zone = activeModule.leftZone,
            onToggleEnabled = { onUpdateLeft(it, null, null) },
            onToggleCustomColor = { onToggleZoneCustomColor(ZoneTarget.LEFT, it) },
            onBrightnessChanged = { onUpdateLeft(null, it, null) },
            onPixelCountChanged = { onUpdateLeft(null, null, it) },
            onColorPicked = { ch, color -> onPickColorForZone(ZoneTarget.LEFT, ch, color) },
            onAnimStep = { delta ->
                val next = ((activeModule.leftZone.animationId - 1 + delta + 100) % 100) + 1
                onPickAnimForZone(ZoneTarget.LEFT, next)
            }
        )

        ZoneEditorCard(
            title = "Zona Lampu Kanan (R)",
            zone = if (activeModule.splitZonesIndependent) activeModule.rightZone else activeModule.leftZone,
            onToggleEnabled = { onUpdateRight(it, null, null) },
            onToggleCustomColor = { onToggleZoneCustomColor(ZoneTarget.RIGHT, it) },
            onBrightnessChanged = { onUpdateRight(null, it, null) },
            onPixelCountChanged = { onUpdateRight(null, null, it) },
            onColorPicked = { ch, color -> onPickColorForZone(ZoneTarget.RIGHT, ch, color) },
            onAnimStep = { delta ->
                val base = if (activeModule.splitZonesIndependent) activeModule.rightZone.animationId else activeModule.leftZone.animationId
                val next = ((base - 1 + delta + 100) % 100) + 1
                onPickAnimForZone(ZoneTarget.RIGHT, next)
            }
        )
    }
}

@Composable
private fun ZoneEditorCard(
    title: String,
    zone: ZoneConfig,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleCustomColor: (Boolean) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onPixelCountChanged: (Int) -> Unit,
    onColorPicked: (Int, RgbwColor) -> Unit,
    onAnimStep: (Int) -> Unit
) {
    val anim = AnimationCatalog.byId(zone.animationId)
    var activeChannel by remember { mutableIntStateOf(0) }
    val palette = RnsColorPresets.normalizePalette(zone)
    val swatches = listOf(
        RgbwColor(255, 45, 45, 0),
        RgbwColor(255, 160, 0, 0),
        RgbwColor(0, 210, 120, 0),
        RgbwColor(0, 180, 255, 0),
        RgbwColor(120, 90, 255, 0),
        RgbwColor(255, 255, 255, 255)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RnsSurface)
            .border(1.dp, RnsBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = RnsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Switch(checked = zone.enabled, onCheckedChange = onToggleEnabled)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Slot #${anim.id.toString().padStart(3, '0')}: ${anim.name}",
                color = RnsPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Prev",
                    color = RnsTextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, RnsBorder, RoundedCornerShape(8.dp))
                        .clickable { onAnimStep(-1) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                Text(
                    text = "Next",
                    color = RnsTextPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, RnsBorder, RoundedCornerShape(8.dp))
                        .clickable { onAnimStep(1) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (zone.customColorEnabled) "Palet Kustom Penuh" else "Warna Asli Efek",
                color = if (zone.customColorEnabled) RnsSecondary else RnsTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Switch(checked = zone.customColorEnabled, onCheckedChange = onToggleCustomColor)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("C1", "C2", "C3", "C4").forEachIndexed { chIdx, lbl ->
                val c = palette[chIdx]
                val sel = activeChannel == chIdx
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) RnsPrimary.copy(alpha = 0.25f) else Color.Transparent)
                        .border(1.dp, if (sel) RnsPrimary else RnsBorder, RoundedCornerShape(8.dp))
                        .clickable { activeChannel = chIdx }
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(c.r, c.g, c.b))
                    )
                    Text(lbl, color = RnsTextPrimary, fontSize = 11.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            swatches.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(color.r, color.g, color.b))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable { onColorPicked(activeChannel, color) }
                )
            }
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Kecerahan Zona", color = RnsTextMuted, fontSize = 12.sp)
                Text("${(zone.brightness * 100 / 255)}%", color = RnsTextPrimary, fontSize = 12.sp)
            }
            Slider(
                value = zone.brightness.toFloat(),
                onValueChange = { onBrightnessChanged(it.toInt()) },
                valueRange = 0f..255f
            )
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Jumlah LED Zona", color = RnsTextMuted, fontSize = 12.sp)
                Text("${zone.pixelCount} LED", color = RnsTextPrimary, fontSize = 12.sp)
            }
            Slider(
                value = zone.pixelCount.toFloat(),
                onValueChange = { onPixelCountChanged(it.toInt()) },
                valueRange = 4f..120f
            )
        }
    }
}
