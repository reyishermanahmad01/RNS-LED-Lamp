package com.rns.controller.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.CombinationPreset
import com.rns.controller.domain.model.CombinationStep
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.PlaybackMode
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.domain.model.ZoneTarget
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsNightBottom
import com.rns.controller.ui.theme.RnsNightTop
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSurface
import com.rns.controller.ui.theme.RnsSurfaceElevated
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary
import kotlin.math.sin

@Composable
fun AnimationsScreen(
    activeModule: LedModule,
    selectedZoneTarget: ZoneTarget,
    combinationPresets: List<CombinationPreset>,
    onSelectZoneTarget: (ZoneTarget) -> Unit,
    onSelectAnimation: (Int) -> Unit,
    onSelectDmxMode: (Int) -> Unit,
    onSetEffectEngine: (EffectEngineMode) -> Unit,
    onSpeedChanged: (Int) -> Unit,
    onBrightnessChanged: (Int) -> Unit,
    onSensitivityChanged: (Int) -> Unit,
    onToggleAutoCycle: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onToggleSplitMode: (Boolean) -> Unit,
    onUpdateLeft: (enabled: Boolean?, brightness: Int?, pixelCount: Int?) -> Unit,
    onUpdateRight: (enabled: Boolean?, brightness: Int?, pixelCount: Int?) -> Unit,
    onPickColorForZone: (ZoneTarget, Int, RgbwColor) -> Unit,
    onToggleZoneCustomColor: (ZoneTarget, Boolean) -> Unit,
    onPickAnimForZone: (ZoneTarget, Int) -> Unit,
    onToggleCombinationEnabled: (Boolean) -> Unit,
    onToggleCombinationAutoCycle: (Boolean) -> Unit,
    onActivateCombinationStep: (Int) -> Unit,
    onAddCombinationSteps: (List<CombinationStep>) -> Unit,
    onMoveCombinationStep: (Int, Int) -> Unit,
    onRemoveCombinationStep: (Int) -> Unit,
    onUpdateCombinationStep: (Int, CombinationStep) -> Unit,
    onApplyCombinationPreset: (CombinationPreset) -> Unit,
    onSaveCombinationPreset: (String) -> Unit,
    onDeleteCombinationPreset: (String) -> Unit
) {
    // 0 = 100 Anim, 1 = 210 DMX, 2 = Kanan-Kiri, 3 = Kombinasi, 4 = Mic
    var subTab by remember {
        mutableIntStateOf(
            when {
                activeModule.combinationEnabled -> 3
                activeModule.effectEngine == EffectEngineMode.DMX210 -> 1
                else -> 0
            }
        )
    }
    var modeCollectTab by remember { mutableIntStateOf(0) } // 0 = Mode, 1 = Collect
    var selectedCategory by remember { mutableStateOf("Semua") }
    var searchQuery by remember { mutableStateOf("") }
    var voiceMode by remember { mutableIntStateOf(0) } // 0 = Voice, 1 = Music
    var voicePreset by remember { mutableIntStateOf(1) } // 1..255
    val favorites = remember { mutableStateListOf(2, 36, 41, 56, 76) }
    val dmxSceneSlots = remember { mutableStateListOf<Int?>(null, null, null, null, null, null) }

    val currentAnimId = when (selectedZoneTarget) {
        ZoneTarget.RIGHT -> if (activeModule.splitZonesIndependent) activeModule.rightZone.animationId else activeModule.leftZone.animationId
        else -> activeModule.leftZone.animationId
    }
    val currentSlot = AnimationCatalog.byId(currentAnimId)

    val filteredSlots = remember(selectedCategory, searchQuery, modeCollectTab, favorites.size) {
        AnimationCatalog.slots.filter { slot ->
            val matchFav = modeCollectTab == 0 || slot.id in favorites
            val matchCat = selectedCategory == "Semua" || slot.category == selectedCategory
            val matchQuery = searchQuery.isBlank() ||
                slot.name.contains(searchQuery, ignoreCase = true) ||
                slot.id.toString().contains(searchQuery)
            matchFav && matchCat && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RnsNightTop, Color(0xFF0A1B56), RnsNightBottom)))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("Anim", "DMX", "L / R", "Kombi", "Mic").forEachIndexed { idx, label ->
                SegmentedButton(
                    selected = subTab == idx,
                    onClick = {
                        subTab = idx
                        if (idx == 0) onSetEffectEngine(EffectEngineMode.ANIM100)
                        if (idx == 1) onSetEffectEngine(EffectEngineMode.DMX210)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 5)
                ) {
                    Text(label, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        when (subTab) {
            1 -> {
                // LED LAMP 12-34-12-530: 210 DMX Engine + 4 '+' Scene Boxes + Pause + 2 '+' + Auto-Cycle + Drum Selector + 3 Sliders
                val currentDmx = AnimationCatalog.dmxById(activeModule.dmxModeId)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1: 4 Outlined '+' Quick DMX Preset Slots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        (0..3).forEach { slotIdx ->
                            val savedDmx = dmxSceneSlots[slotIdx]
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (savedDmx != null) Color(0xFF0084D8).copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (savedDmx != null) {
                                            onSelectDmxMode(savedDmx)
                                        } else {
                                            dmxSceneSlots[slotIdx] = activeModule.dmxModeId
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = savedDmx?.toString() ?: "+",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Row 2: Pause | 2 '+' Slots | Auto-Cycle Circle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleAutoCycle(false) },
                            modifier = Modifier
                                .size(42.dp)
                                .border(1.5.dp, Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Jeda Auto", tint = Color.White)
                        }
                        (4..5).forEach { slotIdx ->
                            val savedDmx = dmxSceneSlots[slotIdx]
                            Box(
                                modifier = Modifier
                                    .size(width = 56.dp, height = 42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (savedDmx != null) Color(0xFF0084D8).copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (savedDmx != null) {
                                            onSelectDmxMode(savedDmx)
                                        } else {
                                            dmxSceneSlots[slotIdx] = activeModule.dmxModeId
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = savedDmx?.toString() ?: "+",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = { onToggleAutoCycle(activeModule.playbackMode != PlaybackMode.AUTO_CYCLE) },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (activeModule.playbackMode == PlaybackMode.AUTO_CYCLE) Color(0xFF0084D8) else Color.Transparent
                                )
                                .border(1.5.dp, Color.White, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Auto-Cycle", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        OutlinedButton(onClick = { onSelectDmxMode(11) }) {
                            Text("Strobo Full White DMX (11–17 & 105–132)", fontSize = 11.sp, color = Color.White)
                        }
                    }

                    // Perspective Drum Mode Picker for 210 DMX
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onSelectDmxMode(((activeModule.dmxModeId - 2 + 210) % 210) + 1) }) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev DMX", tint = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#${currentDmx.id.toString().padStart(3, '0')} · ${currentDmx.name}",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(currentDmx.group, color = RnsTextMuted, fontSize = 11.sp)
                            }
                            IconButton(onClick = { onSelectDmxMode((activeModule.dmxModeId % 210) + 1) }) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next DMX", tint = Color.White)
                            }
                        }

                        // Nearby DMX drum slots
                        (-2..2).forEach { offset ->
                            val targetId = ((activeModule.dmxModeId - 1 + offset + 210) % 210) + 1
                            val slot = AnimationCatalog.dmxById(targetId)
                            val isCurr = offset == 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurr) Color(0xFF0084D8).copy(alpha = 0.45f) else Color.Transparent)
                                    .clickable { onSelectDmxMode(slot.id) }
                                    .padding(horizontal = 12.dp, vertical = if (isCurr) 8.dp else 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${slot.id}. ${slot.name}",
                                    color = if (isCurr) Color.White else Color.White.copy(alpha = 0.55f),
                                    fontWeight = if (isCurr) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isCurr) 13.sp else 12.sp
                                )
                                Text(slot.group, color = RnsTextMuted, fontSize = 10.sp)
                            }
                        }
                    }

                    // 3 DMX Sliders: Mode, Speed, Brightness
                    DmxSliderRow("Mode (1–210)", activeModule.dmxModeId.toFloat(), 1f..210f, Color(0xFF38BDF8)) {
                        onSelectDmxMode(it.toInt())
                    }
                    DmxSliderRow("Speed", activeModule.speed.toFloat(), 1f..100f, Color(0xFF22C55E)) {
                        onSpeedChanged(it.toInt())
                    }
                    DmxSliderRow("Brightness", activeModule.masterBrightness.toFloat(), 0f..255f, Color.White) {
                        onBrightnessChanged(it.toInt())
                    }

                    InlineCustomColorBar(
                        activeModule = activeModule,
                        selectedZoneTarget = selectedZoneTarget,
                        onSelectZoneTarget = onSelectZoneTarget,
                        onToggleCustomColor = onToggleZoneCustomColor,
                        onPickColor = onPickColorForZone
                    )
                }
            }

            2 -> {
                ZonesScreen(
                    activeModule = activeModule,
                    onToggleSplitMode = onToggleSplitMode,
                    onUpdateLeft = onUpdateLeft,
                    onUpdateRight = onUpdateRight,
                    onPickColorForZone = onPickColorForZone,
                    onToggleZoneCustomColor = onToggleZoneCustomColor,
                    onPickAnimForZone = onPickAnimForZone
                )
            }

            3 -> {
                CombinationScreen(
                    activeModule = activeModule,
                    combinationPresets = combinationPresets,
                    onToggleCombinationEnabled = onToggleCombinationEnabled,
                    onToggleCombinationAutoCycle = onToggleCombinationAutoCycle,
                    onActivateStep = onActivateCombinationStep,
                    onAddSteps = onAddCombinationSteps,
                    onMoveStep = onMoveCombinationStep,
                    onRemoveStep = onRemoveCombinationStep,
                    onUpdateStep = onUpdateCombinationStep,
                    onApplyPreset = onApplyCombinationPreset,
                    onSavePreset = onSaveCombinationPreset,
                    onDeletePreset = onDeleteCombinationPreset
                )
            }

            4 -> {
                // LED LAMP 12-34-46-364 & 12-35-27-976: Voice MODE 1..255 + Vinyl Disc Music + Favorites
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Voice", "Music").forEachIndexed { idx, label ->
                            Button(
                                onClick = { voiceMode = idx },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (voiceMode == idx) Color.White else Color.Black.copy(alpha = 0.4f),
                                    contentColor = if (voiceMode == idx) Color(0xFF0F172A) else Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (voiceMode == 0) {
                        // Voice MODE 1..255 Drum + Sensitivity
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (-1..1).forEach { off ->
                                val m = ((voicePreset - 1 + off + 255) % 255) + 1
                                val isCurr = off == 0
                                Text(
                                    text = "MODE $m",
                                    color = if (isCurr) Color.White else Color.White.copy(alpha = 0.5f),
                                    fontWeight = if (isCurr) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = if (isCurr) 20.sp else 12.sp,
                                    modifier = Modifier.clickable {
                                        voicePreset = m
                                        onSelectAnimation(((m - 1) % 100) + 1)
                                    }
                                )
                            }
                            DmxSliderRow("Voice Mode (1–255)", voicePreset.toFloat(), 1f..255f, Color.White) {
                                voicePreset = it.toInt()
                                onSelectAnimation(((voicePreset - 1) % 100) + 1)
                            }
                            DmxSliderRow("Sensitivity", activeModule.sensitivity.toFloat(), 1f..100f, Color(0xFF22C55E)) {
                                onSensitivityChanged(it.toInt())
                            }
                        }
                    } else {
                        // Music Rainbow Vinyl Disc UI (12-35-27-976)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.28f))
                                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DmxSliderRow("Speed", activeModule.speed.toFloat(), 1f..100f, Color(0xFF34D399)) {
                                onSpeedChanged(it.toInt())
                            }
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
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
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0A0C12)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1E293B))
                                    )
                                }
                            }
                            Text(
                                text = "Sinkronisasi Musik & Beat",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text("Favorit & Strobo Full White Cepat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    AnimationCatalog.slots.filter { it.isWhiteStrobe || it.id in favorites }.forEach { slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(1.dp, if (slot.id == currentAnimId) RnsPrimary else RnsBorder, RoundedCornerShape(10.dp))
                                .clickable { onSelectAnimation(slot.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#${slot.id} ${slot.name}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(slot.category, color = RnsSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            else -> {
                // LED LAMP 12-35-16-084 & 12-33-31-981: Mode | Collect Pills + 100 Animasi Dot-Wave Grid
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Mode", "Collect").forEachIndexed { idx, label ->
                                val active = modeCollectTab == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (active) Color(0xFF3C7CFF) else Color.Black.copy(alpha = 0.45f))
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                                        .clickable { modeCollectTab = idx }
                                        .padding(horizontal = 18.dp, vertical = 6.dp)
                                ) {
                                    Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (activeModule.playbackMode == PlaybackMode.AUTO_CYCLE) "Auto ON" else "Auto OFF",
                                color = RnsSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = activeModule.playbackMode == PlaybackMode.AUTO_CYCLE,
                                onCheckedChange = onToggleAutoCycle
                            )
                        }
                    }

                    if (modeCollectTab == 1) {
                        // Collect View: '+' Bookmark Active Effect + Saved Cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (currentSlot.id !in favorites) favorites.add(currentSlot.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("+ Tambah #${currentSlot.id} ${currentSlot.name} ke Collect", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${favorites.size}", color = RnsSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${currentSlot.id.toString().padStart(3, '0')} ${currentSlot.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (activeModule.playbackMode == PlaybackMode.AUTO_CYCLE) {
                                    "Auto ON (${activeModule.autoCycleIntervalSec}d) · Speed ${activeModule.speed}%"
                                } else {
                                    "Manual · Speed ${activeModule.speed}%"
                                },
                                color = RnsSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (activeModule.playbackMode == PlaybackMode.AUTO_CYCLE) {
                        DmxSliderRow("Interval Auto (detik)", activeModule.autoCycleIntervalSec.toFloat(), 3f..60f, RnsSecondary) {
                            onIntervalChanged(it.toInt())
                        }
                    }

                    InlineCustomColorBar(
                        activeModule = activeModule,
                        selectedZoneTarget = selectedZoneTarget,
                        onSelectZoneTarget = onSelectZoneTarget,
                        onToggleCustomColor = onToggleZoneCustomColor,
                        onPickColor = onPickColorForZone
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari 100 animasi atau Strobo White...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AnimationCatalog.categories) { cat ->
                            val selected = cat == selectedCategory
                            Text(
                                text = cat,
                                color = if (selected) Color.White else RnsTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) Color(0xFF0084D8) else Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, if (selected) Color.White.copy(alpha = 0.5f) else RnsBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredSlots, key = { it.id }) { slot ->
                            val isSelected = slot.id == currentAnimId
                            val isFav = slot.id in favorites
                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) Color(0xFF0084D8).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.35f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectAnimation(slot.id) }
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${slot.id.toString().padStart(3, '0')}",
                                        color = if (slot.isWhiteStrobe) Color.White else Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Icon(
                                        imageVector = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorite",
                                        tint = if (isFav) Color(0xFFFACC15) else RnsTextMuted,
                                        modifier = Modifier.clickable {
                                            if (isFav) favorites.remove(slot.id) else favorites.add(slot.id)
                                        }
                                    )
                                }

                                // Dot-wave preview matching LED LAMP Foto 18 cards
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                ) {
                                    val dots = 12
                                    val stepX = size.width / (dots + 1)
                                    for (i in 1..dots) {
                                        val norm = i.toFloat() / dots
                                        val y = size.height / 2f + sin(norm * 6.28f + slot.id) * (size.height * 0.28f)
                                        val dotColor = if (slot.isWhiteStrobe) {
                                            if (i % 2 == 0) Color.White else Color.White.copy(alpha = 0.25f)
                                        } else {
                                            Color.hsv(((slot.id * 29f + i * 22f) % 360f), 0.85f, 1f)
                                        }
                                        drawCircle(color = dotColor, radius = 4.5f, center = Offset(stepX * i, y))
                                    }
                                }

                                Text(
                                    text = slot.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineCustomColorBar(
    activeModule: LedModule,
    selectedZoneTarget: ZoneTarget,
    onSelectZoneTarget: (ZoneTarget) -> Unit,
    onToggleCustomColor: (ZoneTarget, Boolean) -> Unit,
    onPickColor: (ZoneTarget, Int, RgbwColor) -> Unit
) {
    var chIdx by remember { mutableIntStateOf(0) }
    val targetZone = if (selectedZoneTarget == ZoneTarget.RIGHT) activeModule.rightZone else activeModule.leftZone
    val palette = RnsColorPresets.normalizePalette(targetZone)
    val customOn = if (selectedZoneTarget == ZoneTarget.BOTH) {
        activeModule.leftZone.customColorEnabled || activeModule.rightZone.customColorEnabled
    } else {
        targetZone.customColorEnabled
    }
    val quickSwatches = listOf(
        RgbwColor(255, 0, 0, 0),
        RgbwColor(255, 160, 0, 0),
        RgbwColor(0, 235, 110, 0),
        RgbwColor(0, 200, 255, 0),
        RgbwColor(180, 40, 255, 0),
        RgbwColor(255, 255, 255, 255)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.32f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kustom Warna Efek (Pola Tetap)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(ZoneTarget.BOTH to "L+R", ZoneTarget.LEFT to "Kiri", ZoneTarget.RIGHT to "Kanan").forEach { (zt, lbl) ->
                    val sel = selectedZoneTarget == zt
                    Text(
                        text = lbl,
                        color = if (sel) Color(0xFF0F172A) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) Color.White else Color.White.copy(alpha = 0.12f))
                            .clickable { onSelectZoneTarget(zt) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = if (customOn) "Kustom ON" else "Asli",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (customOn) Color(0xFF0084D8) else Color.White.copy(alpha = 0.12f))
                        .clickable { onToggleCustomColor(selectedZoneTarget, !customOn) }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0..3).forEach { i ->
                    val c = palette[i]
                    val sel = chIdx == i
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sel) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, if (sel) Color(0xFF38BDF8) else RnsBorder, RoundedCornerShape(6.dp))
                            .clickable { chIdx = i }
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(c.r, c.g, c.b))
                        )
                        Text("C${i + 1}", color = Color.White, fontSize = 9.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                quickSwatches.forEach { sw ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(sw.r, sw.g, sw.b))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                            .clickable { onPickColor(selectedZoneTarget, chIdx, sw) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DmxSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value.toInt().toString(), color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
        )
    }
}
