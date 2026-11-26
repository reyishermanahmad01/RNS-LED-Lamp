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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.CombinationPreset
import com.rns.controller.domain.model.CombinationStep
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSuccess
import com.rns.controller.ui.theme.RnsTextMuted

@Composable
fun CombinationScreen(
    activeModule: LedModule,
    combinationPresets: List<CombinationPreset>,
    onToggleCombinationEnabled: (Boolean) -> Unit,
    onToggleCombinationAutoCycle: (Boolean) -> Unit,
    onActivateStep: (Int) -> Unit,
    onAddSteps: (List<CombinationStep>) -> Unit,
    onMoveStep: (Int, Int) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onUpdateStep: (Int, CombinationStep) -> Unit,
    onApplyPreset: (CombinationPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    var showMultiPicker by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Master Combination & Auto-Cycle Control Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mode Custom Gabungan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = if (activeModule.combinationEnabled) {
                            "Aktif · Urutan #${activeModule.combinationStepIndex + 1}/${activeModule.combinationSteps.size}"
                        } else {
                            "Gabungkan beberapa efek Anim & DMX dengan warna terpisah"
                        },
                        color = RnsSecondary,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = activeModule.combinationEnabled,
                    onCheckedChange = onToggleCombinationEnabled
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pergantian Kombinasi Otomatis",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = activeModule.combinationAutoCycle,
                    onCheckedChange = onToggleCombinationAutoCycle
                )
            }

            Button(
                onClick = { showMultiPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0084D8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Pilih & Tambah Banyak Efek (Anim / DMX)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Saved Combination Presets Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Preset Kombinasi Tersimpan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(combinationPresets, key = { it.id }) { preset ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, RnsBorder, RoundedCornerShape(10.dp))
                            .clickable { onApplyPreset(preset) }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Column {
                            Text(preset.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${preset.steps.size} efek", color = RnsSecondary, fontSize = 10.sp)
                        }
                        if (!preset.builtIn) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Hapus",
                                tint = Color(0xFFF87171),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDeletePreset(preset.id) }
                            )
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
                    value = newPresetName,
                    onValueChange = { newPresetName = it },
                    placeholder = { Text("Nama preset kombinasi...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSavePreset(newPresetName)
                            newPresetName = ""
                        }
                    }
                ) {
                    Text("Simpan", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Ordered Combination Step Cards
        Text(
            text = "Daftar Urutan Efek (${activeModule.combinationSteps.size} Langkah)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        activeModule.combinationSteps.forEachIndexed { idx, step ->
            val isActive = activeModule.combinationEnabled && idx == activeModule.combinationStepIndex
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) Color(0xFF0084D8).copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.34f))
                    .border(
                        width = if (isActive) 1.5.dp else 1.dp,
                        color = if (isActive) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${idx + 1} · ${step.label}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val modeDesc = if (step.engine == EffectEngineMode.DMX210) {
                            "DMX #${step.dmxModeId}"
                        } else if (step.splitZones) {
                            "Anim L#${step.animationId} / R#${step.rightAnimationId}"
                        } else {
                            "Anim #${step.animationId}"
                        }
                        Text(
                            text = "$modeDesc · ${step.durationSec}d · Speed ${step.speed}% · ${if (step.splitZones) "L/R Terpisah" else "L+R Sinkron"}",
                            color = RnsTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onActivateStep(idx) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Jalankan",
                                tint = if (isActive) RnsSuccess else Color(0xFF38BDF8)
                            )
                        }
                        IconButton(onClick = { editingIndex = idx }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onMoveStep(idx, -1) },
                            enabled = idx > 0,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naik", tint = if (idx > 0) Color.White else RnsTextMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = { onMoveStep(idx, 1) },
                            enabled = idx < activeModule.combinationSteps.lastIndex,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turun", tint = if (idx < activeModule.combinationSteps.lastIndex) Color.White else RnsTextMuted, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onRemoveStep(idx) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Hapus", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // L/R Palette Swatches preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("L:", color = RnsTextMuted, fontSize = 10.sp)
                        step.leftPalette.take(4).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.r, c.g, c.b))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("R:", color = RnsTextMuted, fontSize = 10.sp)
                        step.rightPalette.take(4).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.r, c.g, c.b))
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }

    // Multi-Select Anim / DMX Picker Dialog
    if (showMultiPicker) {
        var pickerEngineTab by remember { mutableIntStateOf(0) } // 0 = 100 Anim, 1 = 210 DMX
        var query by remember { mutableStateOf("") }
        val selectedAnims = remember { mutableStateListOf<Int>() }
        val selectedDmx = remember { mutableStateListOf<Int>() }

        AlertDialog(
            onDismissRequest = { showMultiPicker = false },
            title = { Text("Pilih Beberapa Efek Sekaligus", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("100 Animasi", "210 DMX").forEachIndexed { idx, lbl ->
                            Button(
                                onClick = { pickerEngineTab = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pickerEngineTab == idx) RnsPrimary else Color(0xFF1E293B)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(lbl, fontSize = 11.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Cari ID atau nama efek...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (pickerEngineTab == 0) {
                            val filtered = AnimationCatalog.slots.filter {
                                query.isBlank() || it.name.contains(query, true) || it.id.toString().contains(query)
                            }
                            items(filtered, key = { it.id }) { slot ->
                                val checked = slot.id in selectedAnims
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) selectedAnims.remove(slot.id) else selectedAnims.add(slot.id)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (it) selectedAnims.add(slot.id) else selectedAnims.remove(slot.id)
                                        }
                                    )
                                    Text("#${slot.id} ${slot.name}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        } else {
                            val filtered = AnimationCatalog.dmxModes.filter {
                                query.isBlank() || it.name.contains(query, true) || it.id.toString().contains(query)
                            }
                            items(filtered, key = { it.id }) { dmx ->
                                val checked = dmx.id in selectedDmx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (checked) selectedDmx.remove(dmx.id) else selectedDmx.add(dmx.id)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (it) selectedDmx.add(dmx.id) else selectedDmx.remove(dmx.id)
                                        }
                                    )
                                    Text("DMX #${dmx.id} ${dmx.name}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val totalSelected = selectedAnims.size + selectedDmx.size
                Button(
                    enabled = totalSelected > 0,
                    onClick = {
                        val lp = RnsColorPresets.normalizePalette(activeModule.leftZone)
                        val rp = RnsColorPresets.normalizePalette(activeModule.rightZone)
                        val now = System.currentTimeMillis()
                        val addedAnims = selectedAnims.mapIndexed { idx, id ->
                            val slot = AnimationCatalog.byId(id)
                            CombinationStep(
                                id = "step_${now}_a_$idx",
                                label = slot.name,
                                engine = EffectEngineMode.ANIM100,
                                animationId = slot.id,
                                rightAnimationId = slot.id,
                                dmxModeId = activeModule.dmxModeId,
                                splitZones = activeModule.splitZonesIndependent,
                                customColorEnabled = true,
                                leftPalette = lp,
                                rightPalette = rp,
                                brightness = activeModule.masterBrightness,
                                speed = slot.defaultSpeed,
                                durationSec = 6
                            )
                        }
                        val addedDmx = selectedDmx.mapIndexed { idx, id ->
                            val dmx = AnimationCatalog.dmxById(id)
                            CombinationStep(
                                id = "step_${now}_d_$idx",
                                label = "DMX #${dmx.id} ${dmx.name}",
                                engine = EffectEngineMode.DMX210,
                                animationId = activeModule.leftZone.animationId,
                                rightAnimationId = activeModule.rightZone.animationId,
                                dmxModeId = dmx.id,
                                splitZones = activeModule.splitZonesIndependent,
                                customColorEnabled = true,
                                leftPalette = lp,
                                rightPalette = rp,
                                brightness = activeModule.masterBrightness,
                                speed = activeModule.speed,
                                durationSec = 6
                            )
                        }
                        onAddSteps(addedAnims + addedDmx)
                        showMultiPicker = false
                    }
                ) {
                    Text("Tambah ($totalSelected)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiPicker = false }) { Text("Batal") }
            }
        )
    }

    // Step Editor Dialog (Warna L/R terpisah, durasi, speed, brightness)
    val editIdx = editingIndex
    val stepToEdit = editIdx?.let { activeModule.combinationSteps.getOrNull(it) }
    if (editIdx != null && stepToEdit != null) {
        var draft by remember(editIdx, stepToEdit) { mutableStateOf(stepToEdit) }
        var editZoneRight by remember { mutableStateOf(false) }
        var editChannelIdx by remember { mutableIntStateOf(0) }
        val swatches = listOf(
            RgbwColor(255, 0, 0, 0),
            RgbwColor(255, 160, 0, 0),
            RgbwColor(255, 255, 0, 0),
            RgbwColor(0, 230, 100, 0),
            RgbwColor(0, 200, 255, 0),
            RgbwColor(40, 80, 255, 0),
            RgbwColor(200, 40, 255, 0),
            RgbwColor(255, 255, 255, 255)
        )

        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Edit Langkah #${editIdx + 1}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draft.label,
                        onValueChange = { draft = draft.copy(label = it) },
                        label = { Text("Label Langkah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Kiri & Kanan Terpisah", fontSize = 12.sp)
                        Switch(checked = draft.splitZones, onCheckedChange = { draft = draft.copy(splitZones = it) })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Kustom Warna Penuh", fontSize = 12.sp)
                        Switch(checked = draft.customColorEnabled, onCheckedChange = { draft = draft.copy(customColorEnabled = it) })
                    }

                    Text("Durasi: ${draft.durationSec} detik", fontSize = 12.sp)
                    Slider(
                        value = draft.durationSec.toFloat(),
                        onValueChange = { draft = draft.copy(durationSec = it.toInt()) },
                        valueRange = 2f..30f
                    )
                    Text("Kecepatan: ${draft.speed}%", fontSize = 12.sp)
                    Slider(
                        value = draft.speed.toFloat(),
                        onValueChange = { draft = draft.copy(speed = it.toInt()) },
                        valueRange = 1f..100f
                    )
                    Text("Kecerahan: ${(draft.brightness * 100 / 255)}%", fontSize = 12.sp)
                    Slider(
                        value = draft.brightness.toFloat(),
                        onValueChange = { draft = draft.copy(brightness = it.toInt()) },
                        valueRange = 20f..255f
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { editZoneRight = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (!editZoneRight) "• Zona Kiri" else "Zona Kiri", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { editZoneRight = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (editZoneRight) "• Zona Kanan" else "Zona Kanan", fontSize = 11.sp)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0..3).forEach { ch ->
                            val pal = if (editZoneRight) draft.rightPalette else draft.leftPalette
                            val c = pal.getOrNull(ch) ?: RnsColorPresets.defaultLeftPalette[ch]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (editChannelIdx == ch) 2.dp else 1.dp,
                                        color = if (editChannelIdx == ch) RnsPrimary else RnsBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { editChannelIdx = ch }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(c.r, c.g, c.b))
                                )
                                Text("C${ch + 1}", fontSize = 10.sp)
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        swatches.forEach { sw ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(sw.r, sw.g, sw.b))
                                    .border(1.dp, Color.Gray, CircleShape)
                                    .clickable {
                                        if (editZoneRight) {
                                            val next = draft.rightPalette.toMutableList()
                                            while (next.size < 4) next.add(RnsColorPresets.defaultRightPalette[next.size])
                                            next[editChannelIdx] = sw
                                            draft = draft.copy(splitZones = true, rightPalette = next, customColorEnabled = true)
                                        } else {
                                            val next = draft.leftPalette.toMutableList()
                                            while (next.size < 4) next.add(RnsColorPresets.defaultLeftPalette[next.size])
                                            next[editChannelIdx] = sw
                                            draft = draft.copy(leftPalette = next, customColorEnabled = true)
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateStep(editIdx, draft)
                    editingIndex = null
                }) {
                    Text("Simpan Langkah")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) { Text("Batal") }
            }
        )
    }
}
