package com.rns.controller.data.repository

import android.content.Context
import com.rns.controller.data.protocol.ParsedModuleQr
import com.rns.controller.data.protocol.RnsQrCodec
import com.rns.controller.data.transport.Esp32BleTransport
import com.rns.controller.data.transport.WifiUdpTransport
import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.ColorConfigPreset
import com.rns.controller.domain.model.CombinationPreset
import com.rns.controller.domain.model.ConnectionState
import com.rns.controller.domain.model.DeviceCategoryKey
import com.rns.controller.domain.model.DiscoveredDevice
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.HardwarePinConfig
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.LedDriverType
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.PlaybackMode
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.RnsColorPresets
import com.rns.controller.domain.model.SecurityLockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class ModuleRepository(
    context: Context,
    val wifiTransport: WifiUdpTransport,
    val bleTransport: Esp32BleTransport
) {
    companion object {
        const val MAX_MODULES = 10
        private const val PREFS_NAME = "rns_modules_prefs"
        private const val KEY_MODULES = "modules_json"
        private const val KEY_SECURITY = "security_lock_json"
        private const val KEY_COLOR_PRESETS = "rns_color_presets_json"
        private const val KEY_COMBO_PRESETS = "rns_combo_presets_json"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _modules = MutableStateFlow(loadInitialModules())
    val modules: StateFlow<List<LedModule>> = _modules.asStateFlow()

    private val _activeModuleId = MutableStateFlow(_modules.value.first().id)
    val activeModuleId: StateFlow<String> = _activeModuleId.asStateFlow()

    private val _securityLock = MutableStateFlow(loadSecurityLock())
    val securityLock: StateFlow<SecurityLockState> = _securityLock.asStateFlow()

    private val _colorPresets = MutableStateFlow(loadColorPresets())
    val colorPresets: StateFlow<List<ColorConfigPreset>> = _colorPresets.asStateFlow()

    private val _combinationPresets = MutableStateFlow(loadCombinationPresets())
    val combinationPresets: StateFlow<List<CombinationPreset>> = _combinationPresets.asStateFlow()

    init {
        startAutoCycleLoop()
    }

    private fun loadInitialModules(): List<LedModule> {
        val raw = prefs.getString(KEY_MODULES, null)
        if (!raw.isNullOrBlank()) {
            runCatching {
                val parsed = json.decodeFromString<List<LedModule>>(raw)
                if (parsed.isNotEmpty()) return parsed.take(MAX_MODULES)
            }
        }
        return listOf(
            LedModule(
                id = UUID.randomUUID().toString(),
                slotNumber = 1,
                uniqueCode = "RNS-M01-4A9C",
                name = "LED DMX",
                deviceCategory = DeviceCategoryKey.DMX,
                target = HardwareTarget.SIMULATOR,
                driver = LedDriverType.WS2812B,
                connectionState = ConnectionState.SIMULATOR_ACTIVE
            )
        )
    }

    private fun loadSecurityLock(): SecurityLockState {
        val raw = prefs.getString(KEY_SECURITY, null)
        if (!raw.isNullOrBlank()) {
            runCatching { return json.decodeFromString<SecurityLockState>(raw) }
        }
        return SecurityLockState(enabled = false, pinCode = "1234", locked = false)
    }

    private fun persist(list: List<LedModule>) {
        prefs.edit().putString(KEY_MODULES, json.encodeToString(list)).apply()
    }

    private fun persistSecurity(state: SecurityLockState) {
        prefs.edit().putString(KEY_SECURITY, json.encodeToString(state)).apply()
    }

    private fun loadColorPresets(): List<ColorConfigPreset> {
        val raw = prefs.getString(KEY_COLOR_PRESETS, null)
        val saved = if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<List<ColorConfigPreset>>(raw) }.getOrDefault(emptyList())
        } else emptyList()
        return RnsColorPresets.builtinColorPresets + saved.filter { !it.builtIn }
    }

    private fun persistColorPresets(all: List<ColorConfigPreset>) {
        val customOnly = all.filter { !it.builtIn }
        prefs.edit().putString(KEY_COLOR_PRESETS, json.encodeToString(customOnly)).apply()
    }

    fun saveColorPreset(preset: ColorConfigPreset) {
        _colorPresets.update { current ->
            val next = listOf(preset.copy(builtIn = false)) + current.filterNot { it.id == preset.id }
            persistColorPresets(next)
            next
        }
    }

    fun deleteColorPreset(presetId: String) {
        _colorPresets.update { current ->
            val next = current.filter { it.builtIn || it.id != presetId }
            persistColorPresets(next)
            next
        }
    }

    private fun loadCombinationPresets(): List<CombinationPreset> {
        val raw = prefs.getString(KEY_COMBO_PRESETS, null)
        val saved = if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<List<CombinationPreset>>(raw) }.getOrDefault(emptyList())
        } else emptyList()
        return RnsColorPresets.builtinCombinationPresets() + saved.filter { !it.builtIn }
    }

    private fun persistCombinationPresets(all: List<CombinationPreset>) {
        val customOnly = all.filter { !it.builtIn }
        prefs.edit().putString(KEY_COMBO_PRESETS, json.encodeToString(customOnly)).apply()
    }

    fun saveCombinationPreset(preset: CombinationPreset) {
        _combinationPresets.update { current ->
            val next = listOf(preset.copy(builtIn = false)) + current.filterNot { it.id == preset.id }
            persistCombinationPresets(next)
            next
        }
    }

    fun deleteCombinationPreset(presetId: String) {
        _combinationPresets.update { current ->
            val next = current.filter { it.builtIn || it.id != presetId }
            persistCombinationPresets(next)
            next
        }
    }

    fun updateSecurityLock(transform: (SecurityLockState) -> SecurityLockState) {
        _securityLock.update { current ->
            val next = transform(current)
            persistSecurity(next)
            next
        }
    }

    fun selectActiveModule(moduleId: String) {
        if (_modules.value.any { it.id == moduleId }) {
            _activeModuleId.value = moduleId
        }
    }

    fun addModule(
        name: String,
        target: HardwareTarget,
        driver: LedDriverType,
        category: DeviceCategoryKey = DeviceCategoryKey.DMX
    ): Boolean {
        val current = _modules.value
        if (current.size >= MAX_MODULES) return false
        val nextSlot = (1..MAX_MODULES).firstOrNull { slot -> current.none { it.slotNumber == slot } } ?: return false
        val initialConn = if (target == HardwareTarget.SIMULATOR) {
            ConnectionState.SIMULATOR_ACTIVE
        } else {
            ConnectionState.DISCONNECTED
        }
        val newMod = LedModule(
            id = UUID.randomUUID().toString(),
            slotNumber = nextSlot,
            uniqueCode = RnsQrCodec.generateUniqueCode(nextSlot),
            name = name.ifBlank { "LED ${category.label} ${nextSlot.toString().padStart(2, '0')}" },
            deviceCategory = category,
            target = target,
            driver = driver,
            effectEngine = if (category == DeviceCategoryKey.DMX) EffectEngineMode.DMX210 else EffectEngineMode.ANIM100,
            connectionState = initialConn
        )
        val updated = current + newMod
        _modules.value = updated
        _activeModuleId.value = newMod.id
        persist(updated)
        return true
    }

    fun addModuleFromDiscovered(dev: DiscoveredDevice): String? {
        val current = _modules.value
        val existing = current.firstOrNull {
            (dev.target == HardwareTarget.ESP32_BLE && it.bleDeviceAddress.equals(dev.address, ignoreCase = true)) ||
                (dev.target != HardwareTarget.ESP32_BLE && it.ipAddress == dev.address)
        }
        if (existing != null) {
            _activeModuleId.value = existing.id
            return existing.id
        }
        if (current.size >= MAX_MODULES) return null
        val nextSlot = (1..MAX_MODULES).firstOrNull { slot -> current.none { it.slotNumber == slot } } ?: return null
        val isDmxName = dev.name.contains("DMX", ignoreCase = true)
        val newMod = LedModule(
            id = UUID.randomUUID().toString(),
            slotNumber = nextSlot,
            uniqueCode = RnsQrCodec.generateUniqueCode(nextSlot),
            name = dev.name.ifBlank { "BLE DMX $nextSlot" },
            deviceCategory = if (isDmxName) DeviceCategoryKey.DMX else DeviceCategoryKey.RGB,
            target = dev.target,
            ipAddress = if (dev.target != HardwareTarget.ESP32_BLE) dev.address else "192.168.4.1",
            bleDeviceAddress = if (dev.target == HardwareTarget.ESP32_BLE) dev.address else "",
            bleDeviceName = if (dev.target == HardwareTarget.ESP32_BLE) dev.name else "",
            bleProfile = dev.detectedProfile,
            driver = runCatching { LedDriverType.valueOf(dev.driver) }.getOrDefault(LedDriverType.WS2812B),
            effectEngine = if (isDmxName) EffectEngineMode.DMX210 else EffectEngineMode.ANIM100,
            connectionState = ConnectionState.DISCONNECTED
        )
        val updated = current + newMod
        _modules.value = updated
        _activeModuleId.value = newMod.id
        persist(updated)
        return newMod.id
    }

    fun addOrSelectFromQr(parsed: ParsedModuleQr): Pair<Boolean, String> {
        val current = _modules.value
        val existing = current.firstOrNull { it.uniqueCode.equals(parsed.uniqueCode, ignoreCase = true) }
        if (existing != null) {
            _activeModuleId.value = existing.id
            return true to "Modul ${existing.name} (${existing.uniqueCode}) sudah terdaftar dan dipilih."
        }
        if (current.size >= MAX_MODULES) {
            return false to "Maksimal $MAX_MODULES modul telah tercapai."
        }
        val nextSlot = (1..MAX_MODULES).firstOrNull { slot -> current.none { it.slotNumber == slot } }
            ?: return false to "Slot modul penuh."
        val initialConn = if (parsed.target == HardwareTarget.SIMULATOR) {
            ConnectionState.SIMULATOR_ACTIVE
        } else {
            ConnectionState.DISCONNECTED
        }
        val newMod = LedModule(
            id = UUID.randomUUID().toString(),
            slotNumber = nextSlot,
            uniqueCode = parsed.uniqueCode,
            name = parsed.name,
            target = parsed.target,
            wifiTopology = parsed.wifiTopology,
            ipAddress = parsed.ipAddress,
            udpPort = parsed.udpPort,
            driver = parsed.driver,
            pins = HardwarePinConfig(
                leftDataPin = parsed.leftDataPin,
                rightDataPin = parsed.rightDataPin
            ),
            connectionState = initialConn
        )
        val updated = current + newMod
        _modules.value = updated
        _activeModuleId.value = newMod.id
        persist(updated)
        return true to "Berhasil menambahkan ${newMod.name} (${newMod.uniqueCode})."
    }

    fun removeModule(moduleId: String) {
        val current = _modules.value
        if (current.size <= 1) return
        val target = current.firstOrNull { it.id == moduleId }
        if (target?.bleDeviceAddress?.isNotBlank() == true) {
            bleTransport.disconnect(target.bleDeviceAddress)
        }
        val updated = current.filterNot { it.id == moduleId }
        _modules.value = updated
        if (_activeModuleId.value == moduleId) {
            _activeModuleId.value = updated.first().id
        }
        persist(updated)
    }

    fun updateModule(moduleId: String, dispatchToHardware: Boolean = true, transform: (LedModule) -> LedModule) {
        var updatedModule: LedModule? = null
        _modules.update { list ->
            val mapped = list.map { mod ->
                if (mod.id == moduleId) {
                    val next = transform(mod)
                    updatedModule = next
                    next
                } else mod
            }
            persist(mapped)
            mapped
        }
        if (dispatchToHardware) {
            updatedModule?.let { dispatch(it) }
        }
    }

    fun allPower(powerOn: Boolean) {
        _modules.update { list ->
            val mapped = list.map { it.copy(powerOn = powerOn) }
            persist(mapped)
            mapped
        }
        _modules.value.forEach { dispatch(it) }
    }

    fun applyColorToAllModules(color: RgbwColor, brightness: Int) {
        _modules.update { list ->
            val mapped = list.map { mod ->
                mod.copy(
                    masterBrightness = brightness,
                    leftZone = mod.leftZone.copy(color = color),
                    rightZone = mod.rightZone.copy(color = color)
                )
            }
            persist(mapped)
            mapped
        }
        _modules.value.forEach { dispatch(it) }
    }

    fun connectModule(moduleId: String) {
        val mod = _modules.value.firstOrNull { it.id == moduleId } ?: return
        when (mod.target) {
            HardwareTarget.SIMULATOR -> {
                updateModule(moduleId, dispatchToHardware = false) {
                    it.copy(connectionState = ConnectionState.SIMULATOR_ACTIVE, lastError = null)
                }
            }
            HardwareTarget.ESP8266_WIFI, HardwareTarget.ESP32_WIFI -> {
                updateModule(moduleId, dispatchToHardware = false) {
                    it.copy(connectionState = ConnectionState.CONNECTING, lastError = null)
                }
                scope.launch {
                    val result = wifiTransport.checkDeviceHealth(mod.ipAddress)
                    updateModule(moduleId, dispatchToHardware = result.isSuccess) {
                        if (result.isSuccess) {
                            it.copy(connectionState = ConnectionState.CONNECTED_WIFI, isPaired = true, lastError = null)
                        } else {
                            it.copy(
                                connectionState = ConnectionState.ERROR,
                                lastError = result.exceptionOrNull()?.message ?: "Tidak dapat menjangkau ${mod.ipAddress}"
                            )
                        }
                    }
                }
            }
            HardwareTarget.ESP32_BLE -> {
                if (mod.bleDeviceAddress.isBlank()) {
                    updateModule(moduleId, dispatchToHardware = false) {
                        it.copy(
                            connectionState = ConnectionState.ERROR,
                            lastError = "Pilih perangkat Bluetooth BLE / DMX terlebih dahulu lewat Scan BLE."
                        )
                    }
                    return
                }
                updateModule(moduleId, dispatchToHardware = false) {
                    it.copy(connectionState = ConnectionState.CONNECTING, lastError = null)
                }
                bleTransport.connect(
                    address = mod.bleDeviceAddress,
                    preferredProfile = mod.bleProfile,
                    onConnected = { detectedProfile ->
                        updateModule(moduleId, dispatchToHardware = true) {
                            it.copy(
                                connectionState = ConnectionState.CONNECTED_BLE,
                                isPaired = true,
                                bleProfile = if (it.bleProfile == BleControllerProfile.AUTO_DETECT) detectedProfile else it.bleProfile,
                                lastError = null
                            )
                        }
                    },
                    onDisconnected = { reason ->
                        updateModule(moduleId, dispatchToHardware = false) {
                            it.copy(
                                connectionState = if (reason != null) ConnectionState.ERROR else ConnectionState.DISCONNECTED,
                                lastError = reason
                            )
                        }
                    }
                )
            }
        }
    }

    fun disconnectModule(moduleId: String) {
        val mod = _modules.value.firstOrNull { it.id == moduleId } ?: return
        if (mod.target == HardwareTarget.ESP32_BLE && mod.bleDeviceAddress.isNotBlank()) {
            bleTransport.disconnect(mod.bleDeviceAddress)
        }
        updateModule(moduleId, dispatchToHardware = false) {
            it.copy(
                connectionState = if (it.target == HardwareTarget.SIMULATOR) {
                    ConnectionState.SIMULATOR_ACTIVE
                } else {
                    ConnectionState.DISCONNECTED
                },
                lastError = null
            )
        }
    }

    private fun dispatch(module: LedModule) {
        when (module.target) {
            HardwareTarget.SIMULATOR -> Unit
            HardwareTarget.ESP8266_WIFI, HardwareTarget.ESP32_WIFI -> {
                if (module.connectionState == ConnectionState.CONNECTED_WIFI) {
                    scope.launch { wifiTransport.sendModuleCommand(module) }
                }
            }
            HardwareTarget.ESP32_BLE -> {
                if (module.connectionState == ConnectionState.CONNECTED_BLE) {
                    bleTransport.sendModuleCommand(module)
                }
            }
        }
    }

    private fun startAutoCycleLoop() {
        scope.launch {
            val lastTickByModule = mutableMapOf<String, Long>()
            val lastComboTickByModule = mutableMapOf<String, Long>()
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _modules.value.forEach { mod ->
                    // Priority 1: Mode Custom Gabungan (Combination) Auto-Cycle
                    if (mod.powerOn && mod.combinationEnabled && mod.combinationAutoCycle && mod.combinationSteps.isNotEmpty()) {
                        val safeIdx = mod.combinationStepIndex.coerceIn(0, mod.combinationSteps.lastIndex)
                        val activeStep = mod.combinationSteps[safeIdx]
                        val durationMs = activeStep.durationSec.coerceIn(2, 120) * 1000L
                        val lastCombo = lastComboTickByModule[mod.id] ?: now
                        if (now - lastCombo >= durationMs) {
                            lastComboTickByModule[mod.id] = now
                            updateModule(mod.id, dispatchToHardware = true) { current ->
                                if (current.combinationSteps.isEmpty()) return@updateModule current
                                val nextIdx = (current.combinationStepIndex + 1) % current.combinationSteps.size
                                val nextStep = current.combinationSteps[nextIdx]
                                RnsColorPresets.applyCombinationStep(current, nextStep, nextIdx)
                            }
                        } else if (!lastComboTickByModule.containsKey(mod.id)) {
                            lastComboTickByModule[mod.id] = now
                        }
                        lastTickByModule[mod.id] = now
                    } else if (mod.powerOn && !mod.combinationEnabled && mod.playbackMode == PlaybackMode.AUTO_CYCLE) {
                        lastComboTickByModule[mod.id] = now
                        val last = lastTickByModule[mod.id] ?: now
                        if (now - last >= mod.autoCycleIntervalSec * 1000L) {
                            lastTickByModule[mod.id] = now
                            updateModule(mod.id) { current ->
                                if (current.effectEngine == EffectEngineMode.DMX210) {
                                    current.copy(dmxModeId = (current.dmxModeId % 210) + 1)
                                } else {
                                    val nextLeft = (current.leftZone.animationId % 100) + 1
                                    val nextRight = if (current.splitZonesIndependent) {
                                        (current.rightZone.animationId % 100) + 1
                                    } else nextLeft
                                    current.copy(
                                        leftZone = current.leftZone.copy(animationId = nextLeft),
                                        rightZone = current.rightZone.copy(animationId = nextRight)
                                    )
                                }
                            }
                        }
                    } else {
                        lastTickByModule[mod.id] = now
                        lastComboTickByModule[mod.id] = now
                    }
                }
            }
        }
    }
}
