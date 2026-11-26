package com.rns.controller.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.ZoneTarget
import com.rns.controller.ui.screens.AnimationsScreen
import com.rns.controller.ui.screens.ColorControlScreen
import com.rns.controller.ui.screens.DashboardScreen
import com.rns.controller.ui.screens.MultiControlScreen
import com.rns.controller.ui.screens.SimulatorScreen
import com.rns.controller.ui.theme.RnsBackground
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSurface
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.viewmodel.RnsViewModel

private data class NavTab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RnsAppRoot(
    viewModel: RnsViewModel,
    onRequestBlePermissions: (() -> Unit) -> Unit
) {
    val modules by viewModel.modules.collectAsState()
    val activeModule by viewModel.activeModule.collectAsState()
    val securityLock by viewModel.securityLock.collectAsState()
    val colorPresets by viewModel.colorPresets.collectAsState()
    val combinationPresets by viewModel.combinationPresets.collectAsState()
    val bleDevices by viewModel.bleScannedDevices.collectAsState()
    val lanDevices by viewModel.lanDevices.collectAsState()
    val isBleScanning by viewModel.isBleScanning.collectAsState()
    val isLanScanning by viewModel.isLanScanning.collectAsState()
    val bleStatusMessage by viewModel.bleStatusMessage.collectAsState()
    val selectedZoneTarget by viewModel.selectedZoneTarget.collectAsState()
    val selectedColorChannel by viewModel.selectedColorChannel.collectAsState()
    val customSegmentColors by viewModel.customSegmentColors.collectAsState()
    val timerRemainingSec by viewModel.timerRemainingSec.collectAsState()
    val showLockDialog by viewModel.showLockDialog.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()

    var showGroupsSheet by remember { mutableStateOf(false) }
    var showFocusSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // 5-Tab Bottom Navigation: Home | Color | Animation | Simulator | Scanner
    val tabs = listOf(
        NavTab("Home", Icons.Rounded.Home),
        NavTab("Color", Icons.Rounded.ColorLens),
        NavTab("Animation", Icons.Rounded.AutoAwesome),
        NavTab("Simulator", Icons.Rounded.LiveTv),
        NavTab("Scanner", Icons.Rounded.QrCodeScanner)
    )
    var currentTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = RnsBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = RnsSurface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { currentTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1, fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                0 -> DashboardScreen(
                    modules = modules,
                    activeModule = activeModule,
                    securityLock = securityLock,
                    timerRemainingSec = timerRemainingSec,
                    onSelectModule = viewModel::selectModule,
                    onTogglePower = viewModel::togglePower,
                    onAllPower = viewModel::toggleAllPower,
                    onConnectActive = {
                        if (activeModule.target == HardwareTarget.ESP32_BLE) {
                            onRequestBlePermissions {
                                viewModel.connectModule(activeModule.id)
                            }
                        } else {
                            viewModel.connectModule(activeModule.id)
                        }
                    },
                    onDisconnectActive = { viewModel.disconnectModule(activeModule.id) },
                    onSyncAllColors = viewModel::applyColorToAllModules,
                    onOpenGroupsDrawer = { showGroupsSheet = true },
                    onOpenFocusDrawer = { showFocusSheet = true },
                    onOpenSecurityDialog = viewModel::openLockDialog,
                    onNavigateTab = { currentTab = it }
                )
                1 -> ColorControlScreen(
                    activeModule = activeModule,
                    selectedZoneTarget = selectedZoneTarget,
                    selectedColorChannel = selectedColorChannel,
                    colorPresets = colorPresets,
                    deviceCount = modules.size,
                    onSelectZoneTarget = viewModel::selectZoneTarget,
                    onSelectColorChannel = viewModel::selectColorChannel,
                    onToggleCustomColorMode = { viewModel.toggleCustomColorMode(it) },
                    onApplyColorPreset = viewModel::applyColorPreset,
                    onSaveColorPreset = viewModel::saveColorPreset,
                    onDeleteColorPreset = viewModel::deleteColorPreset,
                    onTogglePower = viewModel::togglePower,
                    onColorChanged = { viewModel.setColor(it) },
                    onBrightnessChanged = viewModel::setMasterBrightness,
                    onSpeedChanged = viewModel::setSpeed,
                    onOpenGroupsDrawer = { showGroupsSheet = true },
                    onOpenFocusDrawer = { showFocusSheet = true }
                )
                2 -> AnimationsScreen(
                    activeModule = activeModule,
                    selectedZoneTarget = selectedZoneTarget,
                    combinationPresets = combinationPresets,
                    onSelectZoneTarget = viewModel::selectZoneTarget,
                    onSelectAnimation = { viewModel.selectAnimation(it) },
                    onSelectDmxMode = viewModel::setDmxMode,
                    onSetEffectEngine = viewModel::setEffectEngine,
                    onSpeedChanged = viewModel::setSpeed,
                    onBrightnessChanged = viewModel::setMasterBrightness,
                    onSensitivityChanged = viewModel::setSensitivity,
                    onToggleAutoCycle = viewModel::toggleAutoCycle,
                    onIntervalChanged = viewModel::setAutoCycleInterval,
                    onToggleSplitMode = viewModel::setSplitZonesIndependent,
                    onUpdateLeft = viewModel::updateLeftZone,
                    onUpdateRight = viewModel::updateRightZone,
                    onPickColorForZone = { z, ch, c -> viewModel.setColor(c, z, ch) },
                    onToggleZoneCustomColor = { z, enabled -> viewModel.toggleCustomColorMode(enabled, z) },
                    onPickAnimForZone = { z, id -> viewModel.selectAnimation(id, z) },
                    onToggleCombinationEnabled = viewModel::toggleCombinationEnabled,
                    onToggleCombinationAutoCycle = viewModel::toggleCombinationAutoCycle,
                    onActivateCombinationStep = viewModel::activateCombinationStep,
                    onAddCombinationSteps = viewModel::addCombinationSteps,
                    onMoveCombinationStep = viewModel::moveCombinationStep,
                    onRemoveCombinationStep = viewModel::removeCombinationStep,
                    onUpdateCombinationStep = viewModel::updateCombinationStep,
                    onApplyCombinationPreset = viewModel::applyCombinationPreset,
                    onSaveCombinationPreset = viewModel::saveCombinationPreset,
                    onDeleteCombinationPreset = viewModel::deleteCombinationPreset
                )
                3 -> SimulatorScreen(
                    activeModule = activeModule,
                    customSegmentColors = customSegmentColors,
                    onSelectDirection = viewModel::setCustomDirection,
                    onSelectStyle = viewModel::setCustomStyle,
                    onPaintSegment = viewModel::paintCustomSegment,
                    onSpeedChanged = viewModel::setSpeed,
                    onBrightnessChanged = viewModel::setMasterBrightness,
                    onToggleLeftTest = {
                        viewModel.updateLeftZone(enabled = !activeModule.leftZone.enabled)
                    },
                    onToggleRightTest = {
                        viewModel.updateRightZone(enabled = !activeModule.rightZone.enabled)
                    },
                    onToggleCustomColorMode = { viewModel.toggleCustomColorMode(it, ZoneTarget.BOTH) },
                    onToggleCombinationAutoCycle = viewModel::toggleCombinationAutoCycle,
                    onActivateCombinationStep = viewModel::activateCombinationStep
                )
                4 -> MultiControlScreen(
                    modules = modules,
                    activeModuleId = activeModule.id,
                    bleDevices = bleDevices,
                    lanDevices = lanDevices,
                    isBleScanning = isBleScanning,
                    isLanScanning = isLanScanning,
                    bleStatusMessage = bleStatusMessage,
                    onSelectModule = viewModel::selectModule,
                    onAddModule = viewModel::addModule,
                    onRegisterQrCode = viewModel::registerScannedQr,
                    onRemoveModule = viewModel::removeModule,
                    onConnectModule = { id ->
                        val targetMod = modules.firstOrNull { it.id == id }
                        if (targetMod?.target == HardwareTarget.ESP32_BLE) {
                            onRequestBlePermissions {
                                viewModel.connectModule(id)
                            }
                        } else {
                            viewModel.connectModule(id)
                        }
                    },
                    onSyncAllColors = viewModel::applyColorToAllModules,
                    onUpdateModuleConfig = viewModel::updateModuleConfig,
                    onStartBleScan = {
                        onRequestBlePermissions {
                            viewModel.startBleScan()
                        }
                    },
                    onStopBleScan = viewModel::stopBleScan,
                    onDiscoverLan = viewModel::discoverWifiDevices,
                    onAssignAndConnectDiscovered = { moduleId, device ->
                        if (device.target == HardwareTarget.ESP32_BLE) {
                            onRequestBlePermissions {
                                viewModel.assignAndConnectBleDevice(moduleId, device)
                            }
                        } else {
                            viewModel.assignLanDevice(moduleId, device)
                        }
                    },
                    onAddDiscoveredModule = { device ->
                        if (device.target == HardwareTarget.ESP32_BLE) {
                            onRequestBlePermissions {
                                viewModel.addAndConnectDiscoveredDevice(device)
                            }
                        } else {
                            viewModel.addAndConnectDiscoveredDevice(device)
                        }
                    },
                    onProvisionWifi = viewModel::provisionRouterWifi
                )
            }
        }
    }

    // LED LAMP Foto 10: Groups Sheet
    if (showGroupsSheet) {
        ModalBottomSheet(onDismissRequest = { showGroupsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Groups & Multi-Control", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("ALL DEVICES (${modules.size}/10 Modul Aktif)", color = RnsPrimary, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.toggleAllPower(true); showGroupsSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ALL ON")
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleAllPower(false); showGroupsSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ALL OFF")
                    }
                }
                OutlinedButton(
                    onClick = {
                        viewModel.applyColorToAllModules()
                        showGroupsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sinkronkan Warna ke Semua Modul")
                }
                OutlinedButton(
                    onClick = {
                        showGroupsSheet = false
                        currentTab = 4
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ADD NEW GROUP / MODULE")
                }
            }
        }
    }

    // LED LAMP Foto 11: Set Focus Sheet
    if (showFocusSheet) {
        ModalBottomSheet(onDismissRequest = { showFocusSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Set Focus · #${activeModule.slotNumber} ${activeModule.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Driver IC: ${activeModule.driver.displayName} (${activeModule.driver.defaultColorOrder})", color = RnsTextMuted, fontSize = 12.sp)

                Text("Subarea Kanan-Kiri", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.selectZoneTarget(ZoneTarget.LEFT) }, modifier = Modifier.weight(1f)) {
                        Text("Left", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {
                        viewModel.setSplitZonesIndependent(false)
                        viewModel.selectZoneTarget(ZoneTarget.BOTH)
                    }, modifier = Modifier.weight(1f)) {
                        Text("Sync L=R", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.selectZoneTarget(ZoneTarget.RIGHT) }, modifier = Modifier.weight(1f)) {
                        Text("Right", fontSize = 11.sp)
                    }
                }

                Text("Button K1 / K2 / K3", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.triggerQuickKey("K1") }, modifier = Modifier.weight(1f)) {
                        Text("K1 Strobe", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.triggerQuickKey("K2") }, modifier = Modifier.weight(1f)) {
                        Text("K2 DMX", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { viewModel.triggerQuickKey("K3") }, modifier = Modifier.weight(1f)) {
                        Text("K3 Patrol", fontSize = 11.sp)
                    }
                }

                Text("Timer Mati Otomatis", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (mins, label) ->
                        OutlinedButton(onClick = { viewModel.setTimerMinutes(mins) }, modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        showFocusSheet = false
                        viewModel.openLockDialog()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (securityLock.enabled) {
                            if (securityLock.locked) "Buka RNS Security Lock" else "RNS Security Lock Aktif"
                        } else "Aktifkan RNS Security Lock"
                    )
                }
            }
        }
    }

    // RNS Security Lock PIN Modal
    if (showLockDialog) {
        var pinInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::closeLockDialog,
            title = {
                Text(
                    if (securityLock.enabled && securityLock.locked) "Buka RNS Security Lock"
                    else "Pengaturan RNS Security Lock"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (securityLock.enabled && securityLock.locked) {
                            "Kontrol terkunci. Masukkan PIN untuk membuka akses modul."
                        } else {
                            "Lindungi perubahan warna, animasi, dan DMX dengan PIN."
                        },
                        color = RnsSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Masukkan PIN (min 4 digit)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (securityLock.enabled && securityLock.locked) {
                        viewModel.unlockWithPin(pinInput)
                    } else {
                        viewModel.enableSecurityPin(pinInput.ifBlank { securityLock.pinCode })
                    }
                }) {
                    Text(if (securityLock.enabled && securityLock.locked) "Buka Kunci" else "Kunci Sekarang")
                }
            },
            dismissButton = {
                Row {
                    if (securityLock.enabled && !securityLock.locked) {
                        TextButton(onClick = viewModel::disableSecurityLock) {
                            Text("Nonaktifkan")
                        }
                    }
                    TextButton(onClick = viewModel::closeLockDialog) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}
