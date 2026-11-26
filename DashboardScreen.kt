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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.PlaybackMode
import com.rns.controller.domain.model.SecurityLockState
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsDanger
import com.rns.controller.ui.theme.RnsHeaderBlue
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSuccess
import com.rns.controller.ui.theme.RnsSurface
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary

@Composable
fun DashboardScreen(
    modules: List<LedModule>,
    activeModule: LedModule,
    securityLock: SecurityLockState,
    timerRemainingSec: Int,
    onSelectModule: (String) -> Unit,
    onTogglePower: () -> Unit,
    onAllPower: (Boolean) -> Unit,
    onConnectActive: () -> Unit,
    onDisconnectActive: () -> Unit,
    onSyncAllColors: () -> Unit,
    onOpenGroupsDrawer: () -> Unit,
    onOpenFocusDrawer: () -> Unit,
    onOpenSecurityDialog: () -> Unit,
    onNavigateTab: (Int) -> Unit
) {
    val activeAnim = AnimationCatalog.byId(activeModule.leftZone.animationId)
    val activeDmx = AnimationCatalog.dmxById(activeModule.dmxModeId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // LED LAMP Foto 1 Header (#0084D8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RnsHeaderBlue)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenGroupsDrawer) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Groups", tint = Color.White)
                }
                Text(
                    text = "RNS LED",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenSecurityDialog) {
                    Icon(
                        imageVector = if (securityLock.enabled && securityLock.locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = "RNS Security Lock",
                        tint = if (securityLock.enabled && securityLock.locked) Color(0xFFFDE047) else Color.White
                    )
                }
                IconButton(onClick = onOpenFocusDrawer) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Set Focus", tint = Color.White)
                }
                IconButton(
                    onClick = { onNavigateTab(4) },
                    modifier = Modifier
                        .size(34.dp)
                        .border(1.5.dp, Color.White, CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Device", tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Device icon grid (LED LAMP Home style, up to 10 modules)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Perangkat (${modules.size}/10)",
                    color = RnsTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onAllPower(true) }) {
                        Text("ALL ON", fontSize = 11.sp, color = RnsSuccess)
                    }
                    OutlinedButton(onClick = { onAllPower(false) }) {
                        Text("ALL OFF", fontSize = 11.sp, color = RnsDanger)
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(modules, key = { it.id }) { mod ->
                    val isSelected = mod.id == activeModule.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) RnsHeaderBlue.copy(alpha = 0.22f) else RnsSurface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) RnsHeaderBlue else RnsBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectModule(mod.id) }
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFEF4444),
                                            Color(0xFFF59E0B),
                                            Color(0xFF22C55E),
                                            Color(0xFF0084D8),
                                            Color(0xFF8B5CF6),
                                            Color(0xFFEF4444)
                                        )
                                    )
                                )
                                .padding(3.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF09143A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = mod.name,
                                tint = if (mod.powerOn) Color(0xFF38BDF8) else RnsTextMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = mod.name,
                            color = RnsTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            text = "#${mod.slotNumber.toString().padStart(2, '0')} · ${mod.deviceCategory.label}",
                            color = RnsTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Active Module Status & Connection Summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RnsSurface)
                    .border(1.dp, RnsBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "#${activeModule.slotNumber.toString().padStart(2, '0')} · ${activeModule.uniqueCode}",
                            color = RnsPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeModule.name,
                            color = RnsTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onTogglePower,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeModule.powerOn) RnsSuccess else Color(0xFF334155)
                        )
                    ) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = "Power")
                        Text(if (activeModule.powerOn) " ON" else " OFF", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Status: ${activeModule.connectionState.label}",
                    color = if (activeModule.connectionState.isRealHardware) RnsSuccess else RnsSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (activeModule.isPaired) "Pairing Resmi: Terpercaya" else "Pairing Resmi: Belum Dipasangkan",
                    color = if (activeModule.isPaired) RnsSuccess else RnsTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (timerRemainingSec > 0) {
                    val mins = timerRemainingSec / 60
                    val secs = timerRemainingSec % 60
                    Text(
                        text = "Timer Aktif: ${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}",
                        color = RnsSecondary,
                        fontSize = 12.sp
                    )
                }

                activeModule.lastError?.let { err ->
                    Text(text = err, color = RnsDanger, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeModule.connectionState.isRealHardware) {
                        OutlinedButton(
                            onClick = onDisconnectActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Putuskan", fontSize = 12.sp, color = RnsDanger)
                        }
                    } else if (activeModule.target.name != "SIMULATOR") {
                        Button(
                            onClick = onConnectActive,
                            colors = ButtonDefaults.buttonColors(containerColor = RnsHeaderBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hubungkan Perangkat", fontSize = 12.sp)
                        }
                    }
                    if (modules.size > 1) {
                        OutlinedButton(onClick = onSyncAllColors, modifier = Modifier.weight(1f)) {
                            Text("Sinkron Semua", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Ringkasan Konfigurasi Aktif
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RnsSurface)
                    .border(1.dp, RnsBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Ringkasan Modul", color = RnsTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                SummaryRow("Perangkat", activeModule.target.label)
                SummaryRow("Driver IC", "${activeModule.driver.displayName} (${activeModule.driver.defaultColorOrder})")
                SummaryRow(
                    "Mesin Efek",
                    if (activeModule.effectEngine == EffectEngineMode.DMX210) {
                        "210 DMX · #${activeDmx.id.toString().padStart(3, '0')} ${activeDmx.name}"
                    } else {
                        "100 Anim · #${activeAnim.id.toString().padStart(3, '0')} ${activeAnim.name}"
                    }
                )
                SummaryRow(
                    "Mode Putar",
                    if (activeModule.playbackMode == PlaybackMode.AUTO_CYCLE) "Auto (${activeModule.autoCycleIntervalSec}d)" else "Manual"
                )
                SummaryRow(
                    "Zona L/R",
                    if (activeModule.splitZonesIndependent) "Terpisah (Independen)" else "Sinkron (L = R)"
                )
            }

            // Akses Cepat ke 4 Halaman Lain
            Text("Akses Cepat", color = RnsTextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickJumpCard(
                    title = "Color Control",
                    subtitle = "RING · Aisle · DIM",
                    icon = Icons.Rounded.ColorLens,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(1) }
                )
                QuickJumpCard(
                    title = "Animation",
                    subtitle = "100 Efek & 210 DMX",
                    icon = Icons.Rounded.AutoAwesome,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(2) }
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickJumpCard(
                    title = "Simulator",
                    subtitle = "Serpentine & L/R",
                    icon = Icons.Rounded.LiveTv,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(3) }
                )
                QuickJumpCard(
                    title = "Device Scanner",
                    subtitle = "Select Device & QR",
                    icon = Icons.Rounded.QrCodeScanner,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTab(4) }
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = RnsTextMuted, fontSize = 12.sp)
        Text(value, color = RnsTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuickJumpCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RnsSurface)
            .border(1.dp, RnsBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = title, tint = RnsPrimary)
        Text(title, color = RnsTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(subtitle, color = RnsTextMuted, fontSize = 11.sp)
    }
}
