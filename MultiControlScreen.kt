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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Water
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rns.controller.data.protocol.RnsQrCodec
import com.rns.controller.data.repository.ModuleRepository
import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.DeviceCategoryKey
import com.rns.controller.domain.model.DiscoveredDevice
import com.rns.controller.domain.model.HardwarePinConfig
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.LedDriverType
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.WifiTopology
import com.rns.controller.ui.theme.RnsBorder
import com.rns.controller.ui.theme.RnsDanger
import com.rns.controller.ui.theme.RnsHeaderBlue
import com.rns.controller.ui.theme.RnsPrimary
import com.rns.controller.ui.theme.RnsSecondary
import com.rns.controller.ui.theme.RnsSuccess
import com.rns.controller.ui.theme.RnsSurface
import com.rns.controller.ui.theme.RnsTextMuted
import com.rns.controller.ui.theme.RnsTextPrimary

private data class DeviceTemplateOption(
    val label: String,
    val subtitle: String,
    val category: DeviceCategoryKey,
    val target: HardwareTarget,
    val driver: LedDriverType
)

private val DEVICE_TEMPLATE_OPTIONS = listOf(
    DeviceTemplateOption("LED BLE 00-XXXX", "ESP32 BLE RGB", DeviceCategoryKey.RGB, HardwareTarget.ESP32_BLE, LedDriverType.WS2812B),
    DeviceTemplateOption("LED BLE 01-XXXX", "ESP8266 Wi-Fi RGB", DeviceCategoryKey.RGB, HardwareTarget.ESP8266_WIFI, LedDriverType.WS2812B),
    DeviceTemplateOption("LED BLE 02-XXXX", "Meteor WS2811", DeviceCategoryKey.RGB, HardwareTarget.ESP8266_WIFI, LedDriverType.WS2811),
    DeviceTemplateOption("LED BLE 03-XXXX", "SK6812 RGBW BLE", DeviceCategoryKey.RGB, HardwareTarget.ESP32_BLE, LedDriverType.SK6812_RGBW),
    DeviceTemplateOption("LED DMX 00-XXXX", "210 DMX ESP32 BLE", DeviceCategoryKey.DMX, HardwareTarget.ESP32_BLE, LedDriverType.WS2812B),
    DeviceTemplateOption("LED DMX 01-XXXX", "210 DMX ESP8266", DeviceCategoryKey.DMX, HardwareTarget.ESP8266_WIFI, LedDriverType.WS2812B),
    DeviceTemplateOption("LED DMX 06-XXXX", "DMX Simulator", DeviceCategoryKey.DMX, HardwareTarget.SIMULATOR, LedDriverType.WS2812B),
    DeviceTemplateOption("LED CAR 00-XXXX", "Kanan-Kiri Lampu Mobil", DeviceCategoryKey.CAR, HardwareTarget.ESP8266_WIFI, LedDriverType.WS2812B),
    DeviceTemplateOption("LED CAR 02-XXXX", "Car Simulator L/R", DeviceCategoryKey.CAR, HardwareTarget.SIMULATOR, LedDriverType.SK6812_RGB),
    DeviceTemplateOption("LED SMART", "Spektrum Aquascape", DeviceCategoryKey.AQUARIUM, HardwareTarget.ESP8266_WIFI, LedDriverType.SK6812_RGBW),
    DeviceTemplateOption("LED STAGE", "Strobe & Beam DMX", DeviceCategoryKey.STAGE, HardwareTarget.ESP32_BLE, LedDriverType.WS2812B),
    DeviceTemplateOption("LED LIGHT", "Analog MOSFET RGB", DeviceCategoryKey.STAGE, HardwareTarget.ESP32_WIFI, LedDriverType.ANALOG_RGB),
    DeviceTemplateOption("LED SUN", "Warm / Cool White", DeviceCategoryKey.CT, HardwareTarget.ESP8266_WIFI, LedDriverType.SK6812_RGBW),
    DeviceTemplateOption("LED LIKE", "Single White Dimmer", DeviceCategoryKey.WHI_DIM, HardwareTarget.ESP8266_WIFI, LedDriverType.ANALOG_RGB),
    DeviceTemplateOption("LED PHO", "Studio Full White", DeviceCategoryKey.PHO, HardwareTarget.ESP32_BLE, LedDriverType.SK6812_RGBW)
)

@Composable
fun MultiControlScreen(
    modules: List<LedModule>,
    activeModuleId: String,
    bleDevices: List<DiscoveredDevice>,
    lanDevices: List<DiscoveredDevice>,
    isBleScanning: Boolean,
    isLanScanning: Boolean,
    bleStatusMessage: String?,
    onSelectModule: (String) -> Unit,
    onAddModule: (String, HardwareTarget, LedDriverType, DeviceCategoryKey) -> Unit,
    onRegisterQrCode: (String) -> Unit,
    onRemoveModule: (String) -> Unit,
    onConnectModule: (String) -> Unit,
    onSyncAllColors: () -> Unit,
    onUpdateModuleConfig: (
        moduleId: String,
        name: String,
        target: HardwareTarget,
        topology: WifiTopology,
        ip: String,
        bleAddr: String,
        bleName: String,
        bleProfile: BleControllerProfile,
        driver: LedDriverType,
        pins: HardwarePinConfig
    ) -> Unit,
    onStartBleScan: () -> Unit,
    onStopBleScan: () -> Unit,
    onDiscoverLan: () -> Unit,
    onAssignAndConnectDiscovered: (moduleId: String, device: DiscoveredDevice) -> Unit,
    onAddDiscoveredModule: (DiscoveredDevice) -> Unit,
    onProvisionWifi: (moduleId: String, mode: String, ssid: String, pass: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DeviceCategoryKey.DMX) }
    var editingModule by remember { mutableStateOf<LedModule?>(null) }
    var wifiProvisionModule by remember { mutableStateOf<LedModule?>(null) }
    var qrIdentityModule by remember { mutableStateOf<LedModule?>(null) }
    var qrInputText by remember { mutableStateOf("") }

    val templatesForCategory = remember(selectedCategory) {
        DEVICE_TEMPLATE_OPTIONS.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // LED LAMP Foto 2-9 Blue "Select Device" Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RnsHeaderBlue)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Device",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${modules.size}/${ModuleRepository.MAX_MODULES} Modul",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // LED LAMP Foto 2-9 2-Column Category Sidebar + Device Template Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4F6FA))
                .padding(bottom = 8.dp)
        ) {
            // Left Sidebar
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .background(Color(0xFFE9EEF5))
            ) {
                DeviceCategoryKey.entries.forEach { cat ->
                    val active = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (active) Color.White else Color.Transparent)
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.label,
                            color = if (active) RnsHeaderBlue else Color(0xFF475569),
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Right Device Icon Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tap ikon untuk menambahkan (${selectedCategory.label}):",
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
                templatesForCategory.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tpl ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                    .clickable(enabled = modules.size < ModuleRepository.MAX_MODULES) {
                                        onAddModule(tpl.label, tpl.target, tpl.driver, tpl.category)
                                    }
                                    .padding(10.dp)
                            ) {
                                val iconVector = when (tpl.category) {
                                    DeviceCategoryKey.CAR -> Icons.Rounded.DirectionsCar
                                    DeviceCategoryKey.AQUARIUM -> Icons.Rounded.Water
                                    DeviceCategoryKey.CT -> Icons.Rounded.Home
                                    DeviceCategoryKey.WHI_DIM -> Icons.Rounded.Lightbulb
                                    DeviceCategoryKey.PHO -> Icons.Rounded.WbSunny
                                    else -> Icons.Rounded.Bluetooth
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF0084D8), Color(0xFF09143A))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(iconVector, contentDescription = tpl.label, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Text(
                                    text = tpl.label,
                                    color = Color(0xFF0F172A),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                                Text(
                                    text = tpl.subtitle,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // QR / Barcode Scanner Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RnsSurface)
                    .border(1.dp, RnsBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = RnsPrimary)
                    Text(
                        text = "Pemindai QR Code / Barcode Modul Unik",
                        color = RnsTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = qrInputText,
                    onValueChange = { qrInputText = it },
                    placeholder = { Text("rns://module?uid=... atau RNS-M02-XXXX") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onRegisterQrCode(qrInputText)
                        qrInputText = ""
                    },
                    enabled = qrInputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RnsHeaderBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Daftarkan via QR / Barcode", fontSize = 12.sp)
                }
            }

            // BLE & Wi-Fi Discovery + Sync All
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (isBleScanning) onStopBleScan() else onStartBleScan() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isBleScanning) "Stop Scan" else "Scan BLE / DMX", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDiscoverLan,
                    enabled = !isLanScanning,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isLanScanning) "Memindai..." else "Scan Wi-Fi UDP", fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(onClick = onSyncAllColors) {
                    Text("Sinkron", fontSize = 11.sp, maxLines = 1)
                }
            }

            if (!bleStatusMessage.isNullOrBlank()) {
                Text(
                    text = bleStatusMessage,
                    color = RnsPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RnsSurface)
                        .border(1.dp, RnsBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            if (bleDevices.isNotEmpty() || lanDevices.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RnsSurface)
                        .border(1.dp, RnsBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Perangkat Bluetooth / Wi-Fi Ditemukan (Tap untuk Hubungkan):",
                        color = RnsPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    (bleDevices + lanDevices).forEach { dev ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0E1626))
                                .border(1.dp, RnsBorder, RoundedCornerShape(10.dp))
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
                                        text = dev.name,
                                        color = RnsTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append(dev.address)
                                            if (dev.rssi != 0) append(" · ${dev.rssi} dBm")
                                            if (dev.isBonded) append(" · Terpasang")
                                        },
                                        color = RnsTextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                    if (dev.target == HardwareTarget.ESP32_BLE) {
                                        Text(
                                            text = "Profil: ${dev.detectedProfile.label}",
                                            color = RnsPrimary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onAssignAndConnectDiscovered(activeModuleId, dev) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RnsHeaderBlue),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pasang & Hubungkan", fontSize = 11.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onAddDiscoveredModule(dev) },
                                    enabled = modules.size < ModuleRepository.MAX_MODULES
                                ) {
                                    Text("+ Modul Baru", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Up to 10 independent module cards
            modules.forEach { mod ->
                val isActive = mod.id == activeModuleId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RnsSurface)
                        .border(1.dp, if (isActive) RnsPrimary else RnsBorder, RoundedCornerShape(14.dp))
                        .clickable { onSelectModule(mod.id) }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${mod.slotNumber.toString().padStart(2, '0')} · ${mod.uniqueCode}",
                                color = RnsPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = mod.name,
                                color = RnsTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${mod.target.label} · ${mod.driver.displayName}",
                                color = RnsTextMuted,
                                fontSize = 12.sp
                            )
                            if (mod.target == HardwareTarget.ESP32_BLE) {
                                val bleLabel = mod.bleDeviceName.ifBlank { mod.bleDeviceAddress }.ifBlank { "Belum dipilih" }
                                Text(
                                    text = "BLE: $bleLabel · ${mod.bleProfile.label}",
                                    color = RnsPrimary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = if (mod.connectionState.isRealHardware) "TERHUBUNG" else "SIMULATOR / OFFLINE",
                            color = if (mod.connectionState.isRealHardware) RnsSuccess else RnsSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!mod.lastError.isNullOrBlank()) {
                        Text(
                            text = mod.lastError,
                            color = RnsDanger,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(onClick = { qrIdentityModule = mod }) {
                            Text("QR", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { editingModule = mod },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Atur", fontSize = 11.sp)
                        }
                        if (mod.target != HardwareTarget.SIMULATOR) {
                            Button(
                                onClick = { onConnectModule(mod.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = RnsHeaderBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Hubungkan", fontSize = 11.sp)
                            }
                        }
                        if (mod.target == HardwareTarget.ESP8266_WIFI || mod.target == HardwareTarget.ESP32_WIFI) {
                            OutlinedButton(onClick = { wifiProvisionModule = mod }) {
                                Text("Wi-Fi", fontSize = 11.sp)
                            }
                        }
                        if (modules.size > 1) {
                            TextButton(onClick = { onRemoveModule(mod.id) }) {
                                Text("Hapus", color = RnsDanger, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    qrIdentityModule?.let { mod ->
        val uri = RnsQrCodec.encodeModuleQrUri(mod)
        AlertDialog(
            onDismissRequest = { qrIdentityModule = null },
            title = { Text("Identitas QR #${mod.slotNumber.toString().padStart(2, '0')}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kode Unik Barcode: ${mod.uniqueCode}", fontWeight = FontWeight.Bold)
                    Text("Payload QR URI:", fontSize = 12.sp, color = RnsTextMuted)
                    Text(uri, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(onClick = { qrIdentityModule = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    editingModule?.let { mod ->
        ModuleConfigDialog(
            module = mod,
            bleDevices = bleDevices,
            onDismiss = { editingModule = null },
            onSave = { name, target, topo, ip, bleAddr, bleName, bleProfile, drv, pins ->
                onUpdateModuleConfig(mod.id, name, target, topo, ip, bleAddr, bleName, bleProfile, drv, pins)
                editingModule = null
            }
        )
    }

    wifiProvisionModule?.let { mod ->
        WifiProvisionDialog(
            module = mod,
            onDismiss = { wifiProvisionModule = null },
            onSubmit = { mode, ssid, pass ->
                onProvisionWifi(mod.id, mode, ssid, pass)
                wifiProvisionModule = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleConfigDialog(
    module: LedModule,
    bleDevices: List<DiscoveredDevice>,
    onDismiss: () -> Unit,
    onSave: (
        String,
        HardwareTarget,
        WifiTopology,
        String,
        String,
        String,
        BleControllerProfile,
        LedDriverType,
        HardwarePinConfig
    ) -> Unit
) {
    var name by remember { mutableStateOf(module.name) }
    var target by remember { mutableStateOf(module.target) }
    var topology by remember { mutableStateOf(module.wifiTopology) }
    var ip by remember { mutableStateOf(module.ipAddress) }
    var bleAddr by remember { mutableStateOf(module.bleDeviceAddress) }
    var bleName by remember { mutableStateOf(module.bleDeviceName) }
    var bleProfile by remember { mutableStateOf(module.bleProfile) }
    var driver by remember { mutableStateOf(module.driver) }
    var targetMenu by remember { mutableStateOf(false) }
    var driverMenu by remember { mutableStateOf(false) }
    var topoMenu by remember { mutableStateOf(false) }
    var bleProfileMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan #${module.slotNumber} (${module.uniqueCode})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Modul") },
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = targetMenu,
                    onExpandedChange = { targetMenu = it }
                ) {
                    OutlinedTextField(
                        value = target.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Perangkat & Protokol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = targetMenu,
                        onDismissRequest = { targetMenu = false }
                    ) {
                        HardwareTarget.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = { target = item; targetMenu = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = driverMenu,
                    onExpandedChange = { driverMenu = it }
                ) {
                    OutlinedTextField(
                        value = driver.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Driver IC LED") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = driverMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = driverMenu,
                        onDismissRequest = { driverMenu = false }
                    ) {
                        LedDriverType.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.displayName) },
                                onClick = { driver = item; driverMenu = false }
                            )
                        }
                    }
                }

                if (target == HardwareTarget.ESP8266_WIFI || target == HardwareTarget.ESP32_WIFI) {
                    ExposedDropdownMenuBox(
                        expanded = topoMenu,
                        onExpandedChange = { topoMenu = it }
                    ) {
                        OutlinedTextField(
                            value = topology.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mode Wi-Fi") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topoMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = topoMenu,
                            onDismissRequest = { topoMenu = false }
                        ) {
                            WifiTopology.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    onClick = {
                                        topology = item
                                        if (item == WifiTopology.ACCESS_POINT && ip.isBlank()) {
                                            ip = "192.168.4.1"
                                        }
                                        topoMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("Alamat IP ESP8266/ESP32") },
                        singleLine = true
                    )
                }

                if (target == HardwareTarget.ESP32_BLE) {
                    ExposedDropdownMenuBox(
                        expanded = bleProfileMenu,
                        onExpandedChange = { bleProfileMenu = it }
                    ) {
                        OutlinedTextField(
                            value = bleProfile.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Profil Protokol BLE DMX / Controller") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bleProfileMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = bleProfileMenu,
                            onDismissRequest = { bleProfileMenu = false }
                        ) {
                            BleControllerProfile.entries.forEach { prof ->
                                DropdownMenuItem(
                                    text = { Text(prof.label) },
                                    onClick = {
                                        bleProfile = prof
                                        bleProfileMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bleAddr,
                        onValueChange = { bleAddr = it },
                        label = { Text("Alamat MAC BLE (ESP32 / DMX Motor)") },
                        singleLine = true
                    )
                    bleDevices.forEach { dev ->
                        TextButton(onClick = {
                            bleAddr = dev.address
                            bleName = dev.name
                            if (dev.detectedProfile != BleControllerProfile.AUTO_DETECT) {
                                bleProfile = dev.detectedProfile
                            }
                        }) {
                            Text("Pilih ${dev.name} (${dev.address})", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(name, target, topology, ip, bleAddr, bleName, bleProfile, driver, module.pins)
            }) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun WifiProvisionDialog(
    module: LedModule,
    onDismiss: () -> Unit,
    onSubmit: (mode: String, ssid: String, pass: String) -> Unit
) {
    var mode by remember { mutableStateOf("STA") }
    var ssid by remember { mutableStateOf(module.routerSsid) }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Konfigurasi Wi-Fi AP / Router") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kirim kredensial ke ${module.ipAddress}/api/wifi", fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { mode = "STA" }) {
                        Text(if (mode == "STA") "✓ Router (STA)" else "Router (STA)")
                    }
                    OutlinedButton(onClick = { mode = "AP" }) {
                        Text(if (mode == "AP") "✓ Wi-Fi AP" else "Wi-Fi AP")
                    }
                }
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("SSID Wi-Fi") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password Wi-Fi") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(mode, ssid, pass) }) { Text("Kirim") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
