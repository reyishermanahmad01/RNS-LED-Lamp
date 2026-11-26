package com.rns.controller.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class HardwareTarget(val label: String) {
    SIMULATOR("Simulator (Tanpa Wemos)"),
    ESP8266_WIFI("ESP8266 (Wemos Wi-Fi)"),
    ESP32_WIFI("ESP32 (Wi-Fi)"),
    ESP32_BLE("Bluetooth BLE (ESP32 / DMX Motor)")
}

@Serializable
enum class BleControllerProfile(val label: String) {
    AUTO_DETECT("Deteksi Otomatis (RNS / DMX / LED LAMP)"),
    RNS_NATIVE("RNS ESP32 Native (24B + JSON Palette)"),
    LED_DMX_BLE("Modul DMX / LED LAMP BLE (FFF0/FFE0)"),
    LED_LAMP_ELK("ELK-BLEDOM / SP105E / Triones"),
    NORDIC_UART("Nordic UART Bridge / Classic SPP")
}

@Serializable
enum class WifiTopology(val label: String) {
    ACCESS_POINT("Wi-Fi AP (Direct 192.168.4.1)"),
    ROUTER_STA("Wi-Fi Router (STA LAN)")
}

@Serializable
enum class LedDriverType(
    val displayName: String,
    val supportsWhiteChannel: Boolean,
    val isAddressable: Boolean,
    val defaultColorOrder: String
) {
    WS2812B("WS2812B (Addressable RGB)", false, true, "GRB"),
    WS2811("WS2811 (12V Addressable RGB)", false, true, "BRG"),
    SK6812_RGB("SK6812 RGB (Addressable)", false, true, "GRB"),
    SK6812_RGBW("SK6812 RGBW (RGB + White)", true, true, "GRBW"),
    ANALOG_RGB("Analog RGB (MOSFET PWM)", false, false, "RGB")
}

@Serializable
enum class PlaybackMode {
    MANUAL,
    AUTO_CYCLE
}

@Serializable
enum class ZoneTarget {
    BOTH,
    LEFT,
    RIGHT
}

@Serializable
enum class EffectEngineMode {
    ANIM100,
    DMX210
}

@Serializable
enum class CustomDirection(val label: String) {
    FORWARD("Forward"),
    REVERSE("Reverse"),
    CHANGE("Change"),
    CYCLE("Cycle")
}

@Serializable
enum class CustomTransitionStyle(val label: String) {
    GD("GD"),
    FD("FD"),
    FW("FW"),
    FS("FS"),
    AC("AC"),
    PU("PU"),
    BREATHE("Breathe"),
    HO("HO")
}

@Serializable
enum class DeviceCategoryKey(val label: String) {
    RGB("RGB"),
    DMX("DMX"),
    CAR("CAR"),
    AQUARIUM("Aquarium"),
    STAGE("Stage"),
    CT("CT"),
    WHI_DIM("WHI DIM"),
    PHO("PHO")
}

@Serializable
enum class ConnectionState(val label: String, val isRealHardware: Boolean) {
    SIMULATOR_ACTIVE("Simulator Aktif (Tanpa Perangkat Nyata)", false),
    DISCONNECTED("Belum Terhubung ke Perangkat", false),
    CONNECTING("Menghubungkan...", false),
    CONNECTED_WIFI("Terhubung via Wi-Fi", true),
    CONNECTED_BLE("Terhubung via BLE ESP32", true),
    ERROR("Gagal Terhubung", false)
}

@Serializable
data class RgbwColor(
    val r: Int = 0,
    val g: Int = 180,
    val b: Int = 255,
    val w: Int = 0
)

@Serializable
data class ZoneConfig(
    val enabled: Boolean = true,
    val color: RgbwColor = RgbwColor(),
    val secondaryColor: RgbwColor = RgbwColor(255, 60, 180, 0),
    val paletteColors: List<RgbwColor> = emptyList(),
    val customColorEnabled: Boolean = false,
    val brightness: Int = 200,
    val animationId: Int = 1,
    val pixelCount: Int = 30
)

@Serializable
data class CombinationStep(
    val id: String,
    val label: String,
    val engine: EffectEngineMode = EffectEngineMode.ANIM100,
    val animationId: Int = 1,
    val rightAnimationId: Int = 1,
    val dmxModeId: Int = 1,
    val splitZones: Boolean = false,
    val customColorEnabled: Boolean = true,
    val leftPalette: List<RgbwColor> = listOf(
        RgbwColor(0, 180, 255, 0),
        RgbwColor(255, 60, 180, 0),
        RgbwColor(0, 255, 160, 0),
        RgbwColor(255, 200, 0, 0)
    ),
    val rightPalette: List<RgbwColor> = listOf(
        RgbwColor(255, 110, 20, 0),
        RgbwColor(0, 220, 255, 0),
        RgbwColor(255, 30, 90, 0),
        RgbwColor(255, 240, 0, 0)
    ),
    val brightness: Int = 220,
    val speed: Int = 60,
    val durationSec: Int = 6
)

@Serializable
data class CombinationPreset(
    val id: String,
    val name: String,
    val builtIn: Boolean = false,
    val steps: List<CombinationStep>
)

@Serializable
data class ColorConfigPreset(
    val id: String,
    val name: String,
    val builtIn: Boolean = false,
    val customColorEnabled: Boolean = true,
    val splitZonesIndependent: Boolean = false,
    val leftColor: RgbwColor,
    val leftSecondary: RgbwColor,
    val leftPalette: List<RgbwColor>,
    val rightColor: RgbwColor,
    val rightSecondary: RgbwColor,
    val rightPalette: List<RgbwColor>
)

@Serializable
data class HardwarePinConfig(
    val leftDataPin: Int = 2,
    val rightDataPin: Int = 4,
    val pwmRedPin: Int = 14,
    val pwmGreenPin: Int = 12,
    val pwmBluePin: Int = 13
)

@Serializable
data class SecurityLockState(
    val enabled: Boolean = false,
    val pinCode: String = "1234",
    val locked: Boolean = false
)

@Serializable
data class TrustedDeviceRecord(
    val controllerId: String,
    val controllerName: String,
    val authToken: String = "",
    val pairedAtMs: Long = 0L
)

@Serializable
data class LedModule(
    val id: String,
    val slotNumber: Int,
    val uniqueCode: String = "RNS-M${slotNumber.toString().padStart(2, '0')}-0001",
    val name: String,
    val deviceCategory: DeviceCategoryKey = DeviceCategoryKey.DMX,
    val target: HardwareTarget = HardwareTarget.SIMULATOR,
    val wifiTopology: WifiTopology = WifiTopology.ACCESS_POINT,
    val ipAddress: String = "192.168.4.1",
    val udpPort: Int = 4210,
    val bleDeviceAddress: String = "",
    val bleDeviceName: String = "",
    val bleProfile: BleControllerProfile = BleControllerProfile.AUTO_DETECT,
    val routerSsid: String = "",
    val driver: LedDriverType = LedDriverType.WS2812B,
    val pins: HardwarePinConfig = HardwarePinConfig(),
    val powerOn: Boolean = true,
    val masterBrightness: Int = 220,
    val speed: Int = 60,
    val sensitivity: Int = 90,
    val effectEngine: EffectEngineMode = EffectEngineMode.ANIM100,
    val dmxModeId: Int = 1,
    val playbackMode: PlaybackMode = PlaybackMode.MANUAL,
    val autoCycleIntervalSec: Int = 10,
    val splitZonesIndependent: Boolean = false,
    val customDirection: CustomDirection = CustomDirection.FORWARD,
    val customStyle: CustomTransitionStyle = CustomTransitionStyle.GD,
    val leftZone: ZoneConfig = ZoneConfig(color = RgbwColor(0, 180, 255, 0), animationId = 2, pixelCount = 24),
    val rightZone: ZoneConfig = ZoneConfig(color = RgbwColor(255, 110, 20, 0), animationId = 2, pixelCount = 24),
    val combinationEnabled: Boolean = false,
    val combinationAutoCycle: Boolean = false,
    val combinationStepIndex: Int = 0,
    val combinationSteps: List<CombinationStep> = RnsColorPresets.defaultCombinationSteps(),
    val connectionState: ConnectionState = ConnectionState.SIMULATOR_ACTIVE,
    val isPaired: Boolean = false,
    val lastError: String? = null
)

@Serializable
data class AnimationSlot(
    val id: Int,
    val name: String,
    val category: String,
    val defaultSpeed: Int = 60,
    val isWhiteStrobe: Boolean = false
)

@Serializable
data class DmxModeSlot(
    val id: Int,
    val name: String,
    val group: String
)

@Serializable
data class DiscoveredDevice(
    val name: String,
    val address: String,
    val target: HardwareTarget,
    val driver: String = "WS2812B",
    val rssi: Int = 0,
    val detectedProfile: BleControllerProfile = BleControllerProfile.AUTO_DETECT,
    val isBonded: Boolean = false
)

object RnsColorPresets {
    val defaultLeftPalette = listOf(
        RgbwColor(0, 180, 255, 0),
        RgbwColor(255, 60, 180, 0),
        RgbwColor(0, 255, 160, 0),
        RgbwColor(255, 200, 0, 0)
    )

    val defaultRightPalette = listOf(
        RgbwColor(255, 110, 20, 0),
        RgbwColor(0, 220, 255, 0),
        RgbwColor(255, 30, 90, 0),
        RgbwColor(255, 240, 0, 0)
    )

    fun normalizePalette(zone: ZoneConfig): List<RgbwColor> {
        val c0 = zone.color
        val c1 = zone.paletteColors.getOrNull(1) ?: zone.secondaryColor
        val c2 = zone.paletteColors.getOrNull(2) ?: defaultLeftPalette[2]
        val c3 = zone.paletteColors.getOrNull(3) ?: defaultLeftPalette[3]
        return listOf(c0, c1, c2, c3)
    }

    fun patchZoneChannelColor(
        zone: ZoneConfig,
        channelIdx: Int,
        color: RgbwColor,
        enableCustom: Boolean = true
    ): ZoneConfig {
        val pal = normalizePalette(zone).toMutableList()
        val idx = channelIdx.coerceIn(0, 3)
        pal[idx] = color
        return zone.copy(
            color = pal[0],
            secondaryColor = pal[1],
            paletteColors = pal,
            customColorEnabled = enableCustom
        )
    }

    val builtinColorPresets = listOf(
        ColorConfigPreset(
            id = "preset_cyber_neon",
            name = "Cyber Neon (Cyan-Pink)",
            builtIn = true,
            customColorEnabled = true,
            splitZonesIndependent = false,
            leftColor = RgbwColor(0, 235, 255, 0),
            leftSecondary = RgbwColor(255, 20, 145, 0),
            leftPalette = listOf(
                RgbwColor(0, 235, 255, 0),
                RgbwColor(255, 20, 145, 0),
                RgbwColor(125, 45, 255, 0),
                RgbwColor(0, 255, 175, 0)
            ),
            rightColor = RgbwColor(0, 235, 255, 0),
            rightSecondary = RgbwColor(255, 20, 145, 0),
            rightPalette = listOf(
                RgbwColor(0, 235, 255, 0),
                RgbwColor(255, 20, 145, 0),
                RgbwColor(125, 45, 255, 0),
                RgbwColor(0, 255, 175, 0)
            )
        ),
        ColorConfigPreset(
            id = "preset_ice_fire_lr",
            name = "Split Ice & Fire (Kiri-Kanan)",
            builtIn = true,
            customColorEnabled = true,
            splitZonesIndependent = true,
            leftColor = RgbwColor(0, 165, 255, 0),
            leftSecondary = RgbwColor(0, 255, 220, 0),
            leftPalette = listOf(
                RgbwColor(0, 165, 255, 0),
                RgbwColor(0, 255, 220, 0),
                RgbwColor(120, 210, 255, 0),
                RgbwColor(255, 255, 255, 0)
            ),
            rightColor = RgbwColor(255, 40, 0, 0),
            rightSecondary = RgbwColor(255, 190, 0, 0),
            rightPalette = listOf(
                RgbwColor(255, 40, 0, 0),
                RgbwColor(255, 190, 0, 0),
                RgbwColor(255, 95, 15, 0),
                RgbwColor(255, 240, 120, 0)
            )
        ),
        ColorConfigPreset(
            id = "preset_patrol_custom",
            name = "Patrol Gold & Emerald",
            builtIn = true,
            customColorEnabled = true,
            splitZonesIndependent = true,
            leftColor = RgbwColor(255, 185, 0, 0),
            leftSecondary = RgbwColor(255, 255, 255, 0),
            leftPalette = listOf(
                RgbwColor(255, 185, 0, 0),
                RgbwColor(255, 255, 255, 0),
                RgbwColor(255, 90, 0, 0),
                RgbwColor(255, 225, 100, 0)
            ),
            rightColor = RgbwColor(0, 235, 120, 0),
            rightSecondary = RgbwColor(0, 175, 255, 0),
            rightPalette = listOf(
                RgbwColor(0, 235, 120, 0),
                RgbwColor(0, 175, 255, 0),
                RgbwColor(120, 255, 180, 0),
                RgbwColor(255, 255, 255, 0)
            )
        ),
        ColorConfigPreset(
            id = "preset_sunset_violet",
            name = "Sunset Coral & Violet",
            builtIn = true,
            customColorEnabled = true,
            splitZonesIndependent = false,
            leftColor = RgbwColor(255, 70, 50, 0),
            leftSecondary = RgbwColor(180, 35, 255, 0),
            leftPalette = listOf(
                RgbwColor(255, 70, 50, 0),
                RgbwColor(180, 35, 255, 0),
                RgbwColor(255, 180, 20, 0),
                RgbwColor(255, 25, 130, 0)
            ),
            rightColor = RgbwColor(255, 70, 50, 0),
            rightSecondary = RgbwColor(180, 35, 255, 0),
            rightPalette = listOf(
                RgbwColor(255, 70, 50, 0),
                RgbwColor(180, 35, 255, 0),
                RgbwColor(255, 180, 20, 0),
                RgbwColor(255, 25, 130, 0)
            )
        )
    )

    fun defaultCombinationSteps(): List<CombinationStep> = listOf(
        CombinationStep(
            id = "step_1",
            label = "Pelangi Cyber Cyan-Magenta",
            engine = EffectEngineMode.ANIM100,
            animationId = 5,
            rightAnimationId = 5,
            dmxModeId = 1,
            splitZones = false,
            customColorEnabled = true,
            leftPalette = listOf(
                RgbwColor(0, 235, 255, 0),
                RgbwColor(255, 20, 145, 0),
                RgbwColor(125, 45, 255, 0),
                RgbwColor(0, 255, 175, 0)
            ),
            rightPalette = listOf(
                RgbwColor(0, 235, 255, 0),
                RgbwColor(255, 20, 145, 0),
                RgbwColor(125, 45, 255, 0),
                RgbwColor(0, 255, 175, 0)
            ),
            brightness = 230,
            speed = 64,
            durationSec = 6
        ),
        CombinationStep(
            id = "step_2",
            label = "Komet Kiri-Kanan Ice & Fire",
            engine = EffectEngineMode.ANIM100,
            animationId = 19,
            rightAnimationId = 22,
            dmxModeId = 1,
            splitZones = true,
            customColorEnabled = true,
            leftPalette = listOf(
                RgbwColor(0, 170, 255, 0),
                RgbwColor(0, 255, 220, 0),
                RgbwColor(120, 210, 255, 0),
                RgbwColor(255, 255, 255, 0)
            ),
            rightPalette = listOf(
                RgbwColor(255, 45, 0, 0),
                RgbwColor(255, 195, 0, 0),
                RgbwColor(255, 95, 15, 0),
                RgbwColor(255, 240, 120, 0)
            ),
            brightness = 240,
            speed = 72,
            durationSec = 6
        ),
        CombinationStep(
            id = "step_3",
            label = "DMX #014 Strobo Emas-Biru",
            engine = EffectEngineMode.DMX210,
            animationId = 38,
            rightAnimationId = 38,
            dmxModeId = 14,
            splitZones = true,
            customColorEnabled = true,
            leftPalette = listOf(
                RgbwColor(255, 210, 0, 0),
                RgbwColor(255, 255, 255, 0),
                RgbwColor(255, 120, 0, 0),
                RgbwColor(255, 240, 120, 0)
            ),
            rightPalette = listOf(
                RgbwColor(0, 165, 255, 0),
                RgbwColor(255, 255, 255, 0),
                RgbwColor(80, 0, 255, 0),
                RgbwColor(0, 255, 220, 0)
            ),
            brightness = 255,
            speed = 80,
            durationSec = 5
        )
    )

    fun builtinCombinationPresets(): List<CombinationPreset> = listOf(
        CombinationPreset(
            id = "combo_preset_show",
            name = "Show Mix (Cyber + Split + DMX)",
            builtIn = true,
            steps = defaultCombinationSteps()
        ),
        CombinationPreset(
            id = "combo_preset_patrol",
            name = "Patrol & Strobe Dual L/R",
            builtIn = true,
            steps = listOf(
                CombinationStep(
                    id = "patrol_1",
                    label = "Strobo Ganda Alternatif",
                    engine = EffectEngineMode.ANIM100,
                    animationId = 41,
                    rightAnimationId = 41,
                    splitZones = true,
                    customColorEnabled = true,
                    leftPalette = listOf(
                        RgbwColor(255, 20, 30, 0),
                        RgbwColor(255, 255, 255, 0),
                        RgbwColor(255, 120, 0, 0),
                        RgbwColor(255, 220, 0, 0)
                    ),
                    rightPalette = listOf(
                        RgbwColor(0, 120, 255, 0),
                        RgbwColor(255, 255, 255, 0),
                        RgbwColor(0, 230, 255, 0),
                        RgbwColor(120, 60, 255, 0)
                    ),
                    brightness = 255,
                    speed = 78,
                    durationSec = 5
                ),
                CombinationStep(
                    id = "patrol_2",
                    label = "DMX #112 Wig-Wag Split",
                    engine = EffectEngineMode.DMX210,
                    animationId = 45,
                    rightAnimationId = 45,
                    dmxModeId = 112,
                    splitZones = true,
                    customColorEnabled = true,
                    leftPalette = listOf(
                        RgbwColor(255, 170, 0, 0),
                        RgbwColor(255, 255, 255, 0),
                        RgbwColor(255, 60, 0, 0),
                        RgbwColor(255, 230, 120, 0)
                    ),
                    rightPalette = listOf(
                        RgbwColor(0, 220, 140, 0),
                        RgbwColor(255, 255, 255, 0),
                        RgbwColor(0, 175, 255, 0),
                        RgbwColor(120, 255, 200, 0)
                    ),
                    brightness = 240,
                    speed = 82,
                    durationSec = 6
                )
            )
        )
    )

    fun applyCombinationStep(module: LedModule, step: CombinationStep, stepIndex: Int): LedModule {
        val lp = (0 until 4).map { step.leftPalette.getOrNull(it) ?: defaultLeftPalette[it] }
        val rp = (0 until 4).map { step.rightPalette.getOrNull(it) ?: lp[it] }
        return module.copy(
            effectEngine = step.engine,
            dmxModeId = if (step.engine == EffectEngineMode.DMX210) step.dmxModeId.coerceIn(1, 210) else module.dmxModeId,
            masterBrightness = step.brightness.coerceIn(0, 255),
            speed = step.speed.coerceIn(1, 100),
            splitZonesIndependent = step.splitZones,
            combinationEnabled = true,
            combinationStepIndex = stepIndex,
            leftZone = module.leftZone.copy(
                animationId = step.animationId.coerceIn(1, 100),
                customColorEnabled = step.customColorEnabled,
                color = lp[0],
                secondaryColor = lp[1],
                paletteColors = lp
            ),
            rightZone = module.rightZone.copy(
                animationId = (if (step.splitZones) step.rightAnimationId else step.animationId).coerceIn(1, 100),
                customColorEnabled = step.customColorEnabled,
                color = rp[0],
                secondaryColor = rp[1],
                paletteColors = rp
            )
        )
    }
}

