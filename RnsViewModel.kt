package com.rns.controller.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rns.controller.data.protocol.RnsQrCodec
import com.rns.controller.data.repository.ModuleRepository
import com.rns.controller.domain.catalog.AnimationCatalog
import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.ColorConfigPreset
import com.rns.controller.domain.model.CombinationPreset
import com.rns.controller.domain.model.CombinationStep
import com.rns.controller.domain.model.ConnectionState
import com.rns.controller.domain.model.CustomDirection
import com.rns.controller.domain.model.CustomTransitionStyle
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
import com.rns.controller.domain.model.WifiTopology
import com.rns.controller.domain.model.ZoneTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RnsViewModel(private val repo: ModuleRepository) : ViewModel() {

    val modules: StateFlow<List<LedModule>> = repo.modules

    val activeModule: StateFlow<LedModule> = combine(repo.modules, repo.activeModuleId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.first()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, repo.modules.value.first())

    val securityLock: StateFlow<SecurityLockState> = repo.securityLock
    val colorPresets: StateFlow<List<ColorConfigPreset>> = repo.colorPresets
    val combinationPresets: StateFlow<List<CombinationPreset>> = repo.combinationPresets

    val bleScannedDevices: StateFlow<List<DiscoveredDevice>> = repo.bleTransport.scannedDevices
    val isBleScanning: StateFlow<Boolean> = repo.bleTransport.isScanning
    val bleStatusMessage: StateFlow<String?> = repo.bleTransport.bleStatusMessage

    private val _lanDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val lanDevices: StateFlow<List<DiscoveredDevice>> = _lanDevices.asStateFlow()

    private val _isLanScanning = MutableStateFlow(false)
    val isLanScanning: StateFlow<Boolean> = _isLanScanning.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _selectedZoneTarget = MutableStateFlow(ZoneTarget.BOTH)
    val selectedZoneTarget: StateFlow<ZoneTarget> = _selectedZoneTarget.asStateFlow()

    private val _selectedColorChannel = MutableStateFlow(0)
    val selectedColorChannel: StateFlow<Int> = _selectedColorChannel.asStateFlow()

    private val _timerRemainingSec = MutableStateFlow(0)
    val timerRemainingSec: StateFlow<Int> = _timerRemainingSec.asStateFlow()

    private val _showLockDialog = MutableStateFlow(false)
    val showLockDialog: StateFlow<Boolean> = _showLockDialog.asStateFlow()

    private val _customSegmentColors = MutableStateFlow<List<RgbwColor?>>(
        List(24) { idx ->
            when {
                idx < 8 -> RgbwColor(255, 0, 0, 0)
                idx < 16 -> RgbwColor(0, 220, 90, 0)
                else -> RgbwColor(0, 120, 255, 0)
            }
        }
    )
    val customSegmentColors: StateFlow<List<RgbwColor?>> = _customSegmentColors.asStateFlow()

    init {
        viewModelScope.launch {
            repo.bleTransport.bleStatusMessage.collect { msg ->
                if (!msg.isNullOrBlank()) {
                    _uiMessage.value = msg
                }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val rem = _timerRemainingSec.value
                if (rem > 0) {
                    val next = rem - 1
                    _timerRemainingSec.value = next
                    if (next == 0) {
                        repo.updateModule(activeModule.value.id) { it.copy(powerOn = false) }
                        _uiMessage.value = "Timer selesai: modul dimatikan otomatis."
                    }
                }
            }
        }
    }

    private fun checkLocked(): Boolean {
        val sec = securityLock.value
        if (sec.enabled && sec.locked) {
            _showLockDialog.value = true
            _uiMessage.value = "RNS Security Lock aktif. Masukkan PIN terlebih dahulu."
            return true
        }
        return false
    }

    fun openLockDialog() {
        _showLockDialog.value = true
    }

    fun closeLockDialog() {
        _showLockDialog.value = false
    }

    fun enableSecurityPin(newPin: String) {
        val clean = newPin.trim()
        if (clean.length < 4) {
            _uiMessage.value = "PIN minimal 4 digit."
            return
        }
        repo.updateSecurityLock {
            SecurityLockState(enabled = true, pinCode = clean, locked = true)
        }
        _showLockDialog.value = false
        _uiMessage.value = "RNS Security Lock diaktifkan & dikunci."
    }

    fun unlockWithPin(pin: String): Boolean {
        val sec = securityLock.value
        return if (pin.trim() == sec.pinCode) {
            repo.updateSecurityLock { it.copy(locked = false) }
            _showLockDialog.value = false
            _uiMessage.value = "RNS Security Lock dibuka."
            true
        } else {
            _uiMessage.value = "PIN salah!"
            false
        }
    }

    fun lockNow() {
        if (securityLock.value.enabled) {
            repo.updateSecurityLock { it.copy(locked = true) }
            _uiMessage.value = "Modul dikunci."
        } else {
            _showLockDialog.value = true
        }
    }

    fun disableSecurityLock() {
        repo.updateSecurityLock { SecurityLockState(enabled = false, pinCode = it.pinCode, locked = false) }
        _showLockDialog.value = false
        _uiMessage.value = "RNS Security Lock dinonaktifkan."
    }

    fun setTimerMinutes(minutes: Int) {
        _timerRemainingSec.value = if (minutes <= 0) 0 else minutes * 60
        _uiMessage.value = if (minutes <= 0) "Timer dibatalkan." else "Timer disetel $minutes menit."
    }

    fun triggerQuickKey(key: String) {
        if (checkLocked()) return
        val mod = activeModule.value
        when (key) {
            "K1" -> {
                repo.updateModule(mod.id) {
                    it.copy(
                        powerOn = true,
                        effectEngine = EffectEngineMode.ANIM100,
                        playbackMode = PlaybackMode.MANUAL,
                        leftZone = it.leftZone.copy(animationId = 36),
                        rightZone = it.rightZone.copy(animationId = 36)
                    )
                }
                _uiMessage.value = "Tombol K1: Strobo Full White Classic"
            }
            "K2" -> {
                repo.updateModule(mod.id) {
                    it.copy(
                        powerOn = true,
                        effectEngine = EffectEngineMode.DMX210,
                        dmxModeId = 1,
                        playbackMode = PlaybackMode.AUTO_CYCLE
                    )
                }
                _uiMessage.value = "Tombol K2: 210 DMX Auto-Cycle"
            }
            "K3" -> {
                repo.updateModule(mod.id) {
                    it.copy(
                        powerOn = true,
                        splitZonesIndependent = true,
                        effectEngine = EffectEngineMode.ANIM100,
                        leftZone = it.leftZone.copy(color = RgbwColor(255, 20, 30, 0), animationId = 25),
                        rightZone = it.rightZone.copy(color = RgbwColor(20, 110, 255, 0), animationId = 25)
                    )
                }
                _uiMessage.value = "Tombol K3: Patrol Split Kanan-Kiri"
            }
        }
    }

    fun selectZoneTarget(target: ZoneTarget) {
        _selectedZoneTarget.value = target
    }

    fun selectColorChannel(channelIdx: Int) {
        _selectedColorChannel.value = channelIdx.coerceIn(0, 3)
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    fun selectModule(moduleId: String) = repo.selectActiveModule(moduleId)

    fun togglePower() {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(powerOn = !it.powerOn) }
    }

    fun toggleAllPower(powerOn: Boolean) {
        if (checkLocked()) return
        repo.allPower(powerOn)
        _uiMessage.value = if (powerOn) "Semua modul dinyalakan (ALL ON)." else "Semua modul dimatikan (ALL OFF)."
    }

    fun setMasterBrightness(value: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(masterBrightness = value.coerceIn(0, 255)) }
    }

    fun setSpeed(value: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(speed = value.coerceIn(1, 100)) }
    }

    fun setSensitivity(value: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(sensitivity = value.coerceIn(1, 100)) }
    }

    fun setEffectEngine(engine: EffectEngineMode) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(effectEngine = engine) }
    }

    fun setDmxMode(dmxId: Int) {
        if (checkLocked()) return
        val clamped = dmxId.coerceIn(1, 210)
        val mod = activeModule.value
        repo.updateModule(mod.id) {
            it.copy(
                effectEngine = EffectEngineMode.DMX210,
                combinationEnabled = false,
                dmxModeId = clamped
            )
        }
    }

    fun setCustomDirection(direction: CustomDirection) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(customDirection = direction) }
    }

    fun setCustomStyle(style: CustomTransitionStyle) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { it.copy(customStyle = style) }
    }

    fun paintCustomSegment(index: Int, color: RgbwColor?) {
        if (checkLocked()) return
        val current = _customSegmentColors.value.toMutableList()
        if (index in current.indices) {
            current[index] = color
            _customSegmentColors.value = current
        }
    }

    fun setColor(
        color: RgbwColor,
        target: ZoneTarget = _selectedZoneTarget.value,
        channelIdx: Int = _selectedColorChannel.value
    ) {
        if (checkLocked()) return
        val mod = activeModule.value
        val ch = channelIdx.coerceIn(0, 3)
        repo.updateModule(mod.id) { current ->
            when (target) {
                ZoneTarget.BOTH -> current.copy(
                    leftZone = RnsColorPresets.patchZoneChannelColor(current.leftZone, ch, color, enableCustom = true),
                    rightZone = RnsColorPresets.patchZoneChannelColor(current.rightZone, ch, color, enableCustom = true)
                )
                ZoneTarget.LEFT -> current.copy(
                    splitZonesIndependent = true,
                    leftZone = RnsColorPresets.patchZoneChannelColor(current.leftZone, ch, color, enableCustom = true)
                )
                ZoneTarget.RIGHT -> current.copy(
                    splitZonesIndependent = true,
                    rightZone = RnsColorPresets.patchZoneChannelColor(current.rightZone, ch, color, enableCustom = true)
                )
            }
        }
    }

    fun toggleCustomColorMode(enabled: Boolean, target: ZoneTarget = _selectedZoneTarget.value) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { current ->
            when (target) {
                ZoneTarget.BOTH -> current.copy(
                    leftZone = current.leftZone.copy(
                        customColorEnabled = enabled,
                        paletteColors = RnsColorPresets.normalizePalette(current.leftZone)
                    ),
                    rightZone = current.rightZone.copy(
                        customColorEnabled = enabled,
                        paletteColors = RnsColorPresets.normalizePalette(current.rightZone)
                    )
                )
                ZoneTarget.LEFT -> current.copy(
                    splitZonesIndependent = true,
                    leftZone = current.leftZone.copy(
                        customColorEnabled = enabled,
                        paletteColors = RnsColorPresets.normalizePalette(current.leftZone)
                    )
                )
                ZoneTarget.RIGHT -> current.copy(
                    splitZonesIndependent = true,
                    rightZone = current.rightZone.copy(
                        customColorEnabled = enabled,
                        paletteColors = RnsColorPresets.normalizePalette(current.rightZone)
                    )
                )
            }
        }
        _uiMessage.value = if (enabled) {
            "Mode Kustom Warna aktif (pola animasi tetap)."
        } else {
            "Kembali ke warna asli bawaan efek."
        }
    }

    fun applyColorPreset(preset: ColorConfigPreset) {
        if (checkLocked()) return
        val mod = activeModule.value
        val lp = (0 until 4).map { preset.leftPalette.getOrNull(it) ?: RnsColorPresets.defaultLeftPalette[it] }
        val rp = (0 until 4).map { preset.rightPalette.getOrNull(it) ?: RnsColorPresets.defaultRightPalette[it] }
        repo.updateModule(mod.id) { current ->
            current.copy(
                splitZonesIndependent = preset.splitZonesIndependent,
                leftZone = current.leftZone.copy(
                    customColorEnabled = preset.customColorEnabled,
                    color = preset.leftColor,
                    secondaryColor = preset.leftSecondary,
                    paletteColors = lp
                ),
                rightZone = current.rightZone.copy(
                    customColorEnabled = preset.customColorEnabled,
                    color = preset.rightColor,
                    secondaryColor = preset.rightSecondary,
                    paletteColors = rp
                )
            )
        }
        _uiMessage.value = "Preset warna \"${preset.name}\" diterapkan (animasi tetap)."
    }

    fun saveColorPreset(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val mod = activeModule.value
        val lp = RnsColorPresets.normalizePalette(mod.leftZone)
        val rp = RnsColorPresets.normalizePalette(mod.rightZone)
        val preset = ColorConfigPreset(
            id = "user_col_${System.currentTimeMillis()}",
            name = clean,
            builtIn = false,
            customColorEnabled = true,
            splitZonesIndependent = mod.splitZonesIndependent,
            leftColor = lp[0],
            leftSecondary = lp[1],
            leftPalette = lp,
            rightColor = rp[0],
            rightSecondary = rp[1],
            rightPalette = rp
        )
        repo.saveColorPreset(preset)
        _uiMessage.value = "Preset warna \"$clean\" disimpan."
    }

    fun deleteColorPreset(presetId: String) {
        repo.deleteColorPreset(presetId)
        _uiMessage.value = "Preset warna dihapus."
    }

    fun selectAnimation(slotId: Int, target: ZoneTarget = _selectedZoneTarget.value) {
        if (checkLocked()) return
        val slot = AnimationCatalog.byId(slotId)
        val mod = activeModule.value
        repo.updateModule(mod.id) { current ->
            when (target) {
                ZoneTarget.BOTH -> current.copy(
                    effectEngine = EffectEngineMode.ANIM100,
                    combinationEnabled = false,
                    playbackMode = PlaybackMode.MANUAL,
                    speed = slot.defaultSpeed,
                    leftZone = current.leftZone.copy(animationId = slot.id),
                    rightZone = current.rightZone.copy(animationId = slot.id)
                )
                ZoneTarget.LEFT -> current.copy(
                    effectEngine = EffectEngineMode.ANIM100,
                    combinationEnabled = false,
                    splitZonesIndependent = true,
                    playbackMode = PlaybackMode.MANUAL,
                    leftZone = current.leftZone.copy(animationId = slot.id)
                )
                ZoneTarget.RIGHT -> current.copy(
                    effectEngine = EffectEngineMode.ANIM100,
                    combinationEnabled = false,
                    splitZonesIndependent = true,
                    playbackMode = PlaybackMode.MANUAL,
                    rightZone = current.rightZone.copy(animationId = slot.id)
                )
            }
        }
    }

    fun toggleAutoCycle(enabled: Boolean) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) {
            it.copy(
                combinationEnabled = if (enabled) false else it.combinationEnabled,
                playbackMode = if (enabled) PlaybackMode.AUTO_CYCLE else PlaybackMode.MANUAL
            )
        }
    }

    fun toggleCombinationEnabled(enabled: Boolean) {
        if (checkLocked()) return
        val mod = activeModule.value
        if (enabled && mod.combinationSteps.isNotEmpty()) {
            val safeIdx = mod.combinationStepIndex.coerceIn(0, mod.combinationSteps.lastIndex)
            val step = mod.combinationSteps[safeIdx]
            repo.updateModule(mod.id) { current ->
                RnsColorPresets.applyCombinationStep(current, step, safeIdx).copy(
                    powerOn = true,
                    combinationEnabled = true
                )
            }
            _uiMessage.value = "Mode Custom Gabungan aktif."
        } else {
            repo.updateModule(mod.id) {
                it.copy(combinationEnabled = false, combinationAutoCycle = false)
            }
            _uiMessage.value = "Mode Custom Gabungan dimatikan."
        }
    }

    fun toggleCombinationAutoCycle(enabled: Boolean) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { current ->
            if (enabled && current.combinationSteps.isNotEmpty()) {
                val safeIdx = current.combinationStepIndex.coerceIn(0, current.combinationSteps.lastIndex)
                val step = current.combinationSteps[safeIdx]
                RnsColorPresets.applyCombinationStep(current, step, safeIdx).copy(
                    powerOn = true,
                    combinationEnabled = true,
                    combinationAutoCycle = true
                )
            } else {
                current.copy(combinationAutoCycle = false)
            }
        }
    }

    fun activateCombinationStep(index: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        if (mod.combinationSteps.isEmpty()) return
        val safeIdx = ((index % mod.combinationSteps.size) + mod.combinationSteps.size) % mod.combinationSteps.size
        val step = mod.combinationSteps[safeIdx]
        repo.updateModule(mod.id) { current ->
            RnsColorPresets.applyCombinationStep(current, step, safeIdx).copy(powerOn = true)
        }
    }

    fun addCombinationSteps(newSteps: List<CombinationStep>) {
        if (checkLocked() || newSteps.isEmpty()) return
        val mod = activeModule.value
        repo.updateModule(mod.id, dispatchToHardware = false) { current ->
            current.copy(combinationSteps = current.combinationSteps + newSteps)
        }
        _uiMessage.value = "${newSteps.size} efek ditambahkan ke daftar kombinasi."
    }

    fun moveCombinationStep(index: Int, delta: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        val targetIdx = index + delta
        if (index !in mod.combinationSteps.indices || targetIdx !in mod.combinationSteps.indices) return
        val list = mod.combinationSteps.toMutableList()
        val moved = list.removeAt(index)
        list.add(targetIdx, moved)
        val nextActive = when (mod.combinationStepIndex) {
            index -> targetIdx
            targetIdx -> index
            else -> mod.combinationStepIndex
        }
        repo.updateModule(mod.id, dispatchToHardware = false) { current ->
            current.copy(combinationSteps = list, combinationStepIndex = nextActive)
        }
    }

    fun removeCombinationStep(index: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        if (index !in mod.combinationSteps.indices) return
        val list = mod.combinationSteps.toMutableList()
        list.removeAt(index)
        val nextIdx = if (list.isEmpty()) 0 else mod.combinationStepIndex.coerceIn(0, list.lastIndex)
        repo.updateModule(mod.id) { current ->
            current.copy(
                combinationSteps = list,
                combinationStepIndex = nextIdx,
                combinationEnabled = if (list.isEmpty()) false else current.combinationEnabled,
                combinationAutoCycle = if (list.isEmpty()) false else current.combinationAutoCycle
            )
        }
    }

    fun updateCombinationStep(index: Int, updated: CombinationStep) {
        if (checkLocked()) return
        val mod = activeModule.value
        if (index !in mod.combinationSteps.indices) return
        val list = mod.combinationSteps.toMutableList()
        list[index] = updated
        val shouldDispatch = mod.combinationEnabled && mod.combinationStepIndex == index
        repo.updateModule(mod.id, dispatchToHardware = shouldDispatch) { current ->
            val base = current.copy(combinationSteps = list)
            if (shouldDispatch) RnsColorPresets.applyCombinationStep(base, updated, index) else base
        }
    }

    fun applyCombinationPreset(preset: CombinationPreset) {
        if (checkLocked() || preset.steps.isEmpty()) return
        val mod = activeModule.value
        val first = preset.steps.first()
        repo.updateModule(mod.id) { current ->
            RnsColorPresets.applyCombinationStep(current.copy(combinationSteps = preset.steps), first, 0)
                .copy(powerOn = true, combinationEnabled = true)
        }
        _uiMessage.value = "Preset kombinasi \"${preset.name}\" dimuat."
    }

    fun saveCombinationPreset(name: String) {
        val clean = name.trim()
        val mod = activeModule.value
        if (clean.isBlank() || mod.combinationSteps.isEmpty()) return
        repo.saveCombinationPreset(
            CombinationPreset(
                id = "user_combo_${System.currentTimeMillis()}",
                name = clean,
                builtIn = false,
                steps = mod.combinationSteps
            )
        )
        _uiMessage.value = "Kombinasi \"$clean\" disimpan ke preset."
    }

    fun deleteCombinationPreset(presetId: String) {
        repo.deleteCombinationPreset(presetId)
        _uiMessage.value = "Preset kombinasi dihapus."
    }

    fun setAutoCycleInterval(seconds: Int) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) {
            it.copy(autoCycleIntervalSec = seconds.coerceIn(3, 120))
        }
    }

    fun setSplitZonesIndependent(enabled: Boolean) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) {
            it.copy(splitZonesIndependent = enabled)
        }
    }

    fun updateLeftZone(enabled: Boolean? = null, brightness: Int? = null, pixelCount: Int? = null) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { current ->
            current.copy(
                leftZone = current.leftZone.copy(
                    enabled = enabled ?: current.leftZone.enabled,
                    brightness = brightness?.coerceIn(0, 255) ?: current.leftZone.brightness,
                    pixelCount = pixelCount?.coerceIn(1, 300) ?: current.leftZone.pixelCount
                )
            )
        }
    }

    fun updateRightZone(enabled: Boolean? = null, brightness: Int? = null, pixelCount: Int? = null) {
        if (checkLocked()) return
        val mod = activeModule.value
        repo.updateModule(mod.id) { current ->
            current.copy(
                splitZonesIndependent = true,
                rightZone = current.rightZone.copy(
                    enabled = enabled ?: current.rightZone.enabled,
                    brightness = brightness?.coerceIn(0, 255) ?: current.rightZone.brightness,
                    pixelCount = pixelCount?.coerceIn(1, 300) ?: current.rightZone.pixelCount
                )
            )
        }
    }

    fun addModule(
        name: String,
        target: HardwareTarget,
        driver: LedDriverType,
        category: DeviceCategoryKey = DeviceCategoryKey.DMX
    ) {
        val added = repo.addModule(name, target, driver, category)
        if (!added) {
            _uiMessage.value = "Maksimal ${ModuleRepository.MAX_MODULES} modul telah tercapai."
        } else {
            _uiMessage.value = "Modul $name ditambahkan ke daftar."
        }
    }

    fun registerScannedQr(rawText: String) {
        val parsed = RnsQrCodec.parseModuleQr(rawText)
        if (parsed == null) {
            _uiMessage.value = "Format QR/Barcode tidak dikenali (gunakan rns://module?uid=... atau RNS-MXX-XXXX)."
            return
        }
        val (_, msg) = repo.addOrSelectFromQr(parsed)
        _uiMessage.value = msg
    }

    fun removeModule(moduleId: String) = repo.removeModule(moduleId)

    fun updateModuleConfig(
        moduleId: String,
        name: String,
        target: HardwareTarget,
        wifiTopology: WifiTopology,
        ipAddress: String,
        bleAddress: String,
        bleName: String,
        bleProfile: BleControllerProfile,
        driver: LedDriverType,
        pins: HardwarePinConfig
    ) {
        repo.updateModule(moduleId, dispatchToHardware = false) { current ->
            val cleanBle = bleAddress.trim().uppercase()
            val resetState = when {
                target == HardwareTarget.SIMULATOR -> ConnectionState.SIMULATOR_ACTIVE
                current.target != target || current.bleDeviceAddress != cleanBle -> ConnectionState.DISCONNECTED
                else -> current.connectionState
            }
            current.copy(
                name = name.ifBlank { current.name },
                target = target,
                wifiTopology = wifiTopology,
                ipAddress = ipAddress.trim(),
                bleDeviceAddress = cleanBle,
                bleDeviceName = bleName.trim(),
                bleProfile = bleProfile,
                driver = driver,
                pins = pins,
                connectionState = resetState,
                lastError = null
            )
        }
    }

    fun assignAndConnectBleDevice(moduleId: String, device: DiscoveredDevice) {
        val cleanAddr = device.address.trim().uppercase()
        repo.updateModule(moduleId, dispatchToHardware = false) { current ->
            current.copy(
                target = HardwareTarget.ESP32_BLE,
                bleDeviceAddress = cleanAddr,
                bleDeviceName = device.name,
                bleProfile = if (device.detectedProfile != BleControllerProfile.AUTO_DETECT) {
                    device.detectedProfile
                } else {
                    current.bleProfile
                },
                connectionState = ConnectionState.CONNECTING,
                lastError = null
            )
        }
        repo.selectActiveModule(moduleId)
        repo.connectModule(moduleId)
        _uiMessage.value = "Menghubungkan ke ${device.name} ($cleanAddr)..."
    }

    fun assignLanDevice(moduleId: String, device: DiscoveredDevice) {
        repo.updateModule(moduleId, dispatchToHardware = false) { current ->
            current.copy(
                target = device.target,
                wifiTopology = WifiTopology.ROUTER_STA,
                ipAddress = device.address.trim(),
                connectionState = ConnectionState.CONNECTING,
                lastError = null
            )
        }
        repo.selectActiveModule(moduleId)
        repo.connectModule(moduleId)
        _uiMessage.value = "Menghubungkan ke ${device.name} (${device.address})..."
    }

    fun addAndConnectDiscoveredDevice(device: DiscoveredDevice) {
        val createdId = repo.addModuleFromDiscovered(device)
        if (createdId == null) {
            _uiMessage.value = "Maksimal ${ModuleRepository.MAX_MODULES} modul telah tercapai."
            return
        }
        repo.connectModule(createdId)
        _uiMessage.value = "Modul ${device.name} ditambahkan & dihubungkan."
    }

    fun connectModule(moduleId: String) = repo.connectModule(moduleId)

    fun disconnectModule(moduleId: String) {
        repo.disconnectModule(moduleId)
        _uiMessage.value = "Koneksi modul diputuskan."
    }

    fun applyColorToAllModules() {
        if (checkLocked()) return
        val current = activeModule.value
        repo.applyColorToAllModules(current.leftZone.color, current.masterBrightness)
        _uiMessage.value = "Warna & kecerahan disinkronkan ke semua modul."
    }

    fun discoverWifiDevices() {
        viewModelScope.launch {
            _isLanScanning.value = true
            _lanDevices.value = repo.wifiTransport.discoverLanModules()
            _isLanScanning.value = false
        }
    }

    fun startBleScan() = repo.bleTransport.startScan()
    fun stopBleScan() = repo.bleTransport.stopScan()

    fun provisionRouterWifi(moduleId: String, mode: String, ssid: String, password: String) {
        val mod = modules.value.firstOrNull { it.id == moduleId } ?: return
        viewModelScope.launch {
            val res = repo.wifiTransport.provisionWifiCredentials(mod.ipAddress, mode, ssid, password)
            _uiMessage.value = if (res.isSuccess) {
                "Konfigurasi Wi-Fi $mode berhasil dikirim ke ${mod.ipAddress}."
            } else {
                "Gagal mengirim konfigurasi Wi-Fi: ${res.exceptionOrNull()?.message}"
            }
        }
    }
}

class RnsViewModelFactory(private val repo: ModuleRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RnsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RnsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
