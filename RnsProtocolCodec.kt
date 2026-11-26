package com.rns.controller.data.protocol

import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.PlaybackMode
import com.rns.controller.domain.model.RnsColorPresets
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RnsWireZone(
    val en: Boolean,
    val ce: Boolean = false,
    val r: Int,
    val g: Int,
    val b: Int,
    val w: Int,
    val r2: Int = 255,
    val g2: Int = 60,
    val b2: Int = 180,
    val pal: List<List<Int>> = emptyList(),
    val br: Int,
    val anim: Int,
    val px: Int
)

@Serializable
data class RnsWireCommand(
    val cmd: String = "SET",
    val cid: String = "",
    val mod: Int,
    val pwr: Boolean,
    val br: Int,
    val spd: Int,
    val engine: String = "ANIM100",
    val dmx: Int = 1,
    val auto: Boolean,
    val autoSec: Int,
    val split: Boolean,
    val combo: Boolean = false,
    val comboStep: Int = 0,
    val drv: String,
    val lPin: Int,
    val rPin: Int,
    val pR: Int,
    val pG: Int,
    val pB: Int,
    val left: RnsWireZone,
    val right: RnsWireZone
)

@Serializable
data class WifiProvisionPayload(
    val mode: String, // "AP" or "STA"
    val ssid: String,
    val pass: String,
    val cid: String = "",
    val tok: String = ""
)

@Serializable
data class PairRequestPayload(
    val cid: String,
    val name: String,
    val pin: String
)

@Serializable
data class PairResponsePayload(
    val ok: Boolean = false,
    val token: String = "",
    val uid: String = "",
    val error: String? = null
)

@Serializable
data class RevokeRequestPayload(
    val cid: String,
    val tok: String,
    val targetCid: String
)

@Serializable
data class RecoverRequestPayload(
    val recoveryKey: String,
    val newPin: String
)

object RnsProtocolCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeModuleState(module: LedModule, controllerId: String = ""): String {
        val rightSource = if (module.splitZonesIndependent) module.rightZone else module.leftZone
        val leftPal = RnsColorPresets.normalizePalette(module.leftZone).map {
            listOf(it.r.coerceIn(0, 255), it.g.coerceIn(0, 255), it.b.coerceIn(0, 255))
        }
        val rightPal = RnsColorPresets.normalizePalette(rightSource).map {
            listOf(it.r.coerceIn(0, 255), it.g.coerceIn(0, 255), it.b.coerceIn(0, 255))
        }
        val payload = RnsWireCommand(
            cid = controllerId,
            mod = module.slotNumber,
            pwr = module.powerOn,
            br = module.masterBrightness.coerceIn(0, 255),
            spd = module.speed.coerceIn(1, 100),
            engine = module.effectEngine.name,
            dmx = module.dmxModeId.coerceIn(1, 210),
            auto = module.playbackMode == PlaybackMode.AUTO_CYCLE,
            autoSec = module.autoCycleIntervalSec.coerceIn(3, 120),
            split = module.splitZonesIndependent,
            combo = module.combinationEnabled,
            comboStep = module.combinationStepIndex,
            drv = module.driver.name,
            lPin = module.pins.leftDataPin,
            rPin = module.pins.rightDataPin,
            pR = module.pins.pwmRedPin,
            pG = module.pins.pwmGreenPin,
            pB = module.pins.pwmBluePin,
            left = RnsWireZone(
                en = module.leftZone.enabled,
                ce = module.leftZone.customColorEnabled,
                r = module.leftZone.color.r.coerceIn(0, 255),
                g = module.leftZone.color.g.coerceIn(0, 255),
                b = module.leftZone.color.b.coerceIn(0, 255),
                w = module.leftZone.color.w.coerceIn(0, 255),
                r2 = module.leftZone.secondaryColor.r.coerceIn(0, 255),
                g2 = module.leftZone.secondaryColor.g.coerceIn(0, 255),
                b2 = module.leftZone.secondaryColor.b.coerceIn(0, 255),
                pal = leftPal,
                br = module.leftZone.brightness.coerceIn(0, 255),
                anim = module.leftZone.animationId.coerceIn(1, 100),
                px = module.leftZone.pixelCount.coerceIn(1, 300)
            ),
            right = RnsWireZone(
                en = rightSource.enabled,
                ce = rightSource.customColorEnabled,
                r = rightSource.color.r.coerceIn(0, 255),
                g = rightSource.color.g.coerceIn(0, 255),
                b = rightSource.color.b.coerceIn(0, 255),
                w = rightSource.color.w.coerceIn(0, 255),
                r2 = rightSource.secondaryColor.r.coerceIn(0, 255),
                g2 = rightSource.secondaryColor.g.coerceIn(0, 255),
                b2 = rightSource.secondaryColor.b.coerceIn(0, 255),
                pal = rightPal,
                br = rightSource.brightness.coerceIn(0, 255),
                anim = rightSource.animationId.coerceIn(1, 100),
                px = module.rightZone.pixelCount.coerceIn(1, 300)
            )
        )
        return json.encodeToString(payload)
    }

    /**
     * Compact 24-byte binary packet for ultra-fast UDP & BLE GATT MTU23/MTU64 writes.
     */
    fun encodeCompactBinary(module: LedModule): ByteArray {
        val right = if (module.splitZonesIndependent) module.rightZone else module.leftZone
        val isDmx = module.effectEngine == EffectEngineMode.DMX210
        val flags = (if (module.powerOn) 0x01 else 0) or
            (if (module.playbackMode == PlaybackMode.AUTO_CYCLE) 0x02 else 0) or
            (if (module.splitZonesIndependent) 0x04 else 0) or
            (if (module.leftZone.enabled) 0x08 else 0) or
            (if (right.enabled) 0x10 else 0) or
            (if (isDmx) 0x20 else 0) or
            (if (module.leftZone.customColorEnabled || right.customColorEnabled) 0x40 else 0)

        val leftEff = if (isDmx) module.dmxModeId.coerceIn(1, 210) else module.leftZone.animationId.coerceIn(1, 100)
        val rightEff = if (isDmx) module.dmxModeId.coerceIn(1, 210) else right.animationId.coerceIn(1, 100)

        return byteArrayOf(
            0x52, 0x4E, 0x53, 0x01,
            module.slotNumber.toByte(),
            flags.toByte(),
            module.masterBrightness.coerceIn(0, 255).toByte(),
            module.speed.coerceIn(1, 100).toByte(),
            module.driver.ordinal.toByte(),
            leftEff.toByte(),
            module.leftZone.color.r.toByte(),
            module.leftZone.color.g.toByte(),
            module.leftZone.color.b.toByte(),
            module.leftZone.color.w.toByte(),
            module.leftZone.brightness.coerceIn(0, 255).toByte(),
            rightEff.toByte(),
            right.color.r.toByte(),
            right.color.g.toByte(),
            right.color.b.toByte(),
            right.color.w.toByte(),
            right.brightness.coerceIn(0, 255).toByte(),
            module.leftZone.pixelCount.coerceIn(1, 255).toByte(),
            module.rightZone.pixelCount.coerceIn(1, 255).toByte(),
            module.autoCycleIntervalSec.coerceIn(3, 120).toByte()
        )
    }

    fun encodeWifiProvision(
        mode: String,
        ssid: String,
        password: String,
        controllerId: String = "",
        authToken: String = ""
    ): String =
        json.encodeToString(
            WifiProvisionPayload(
                mode = mode,
                ssid = ssid,
                pass = password,
                cid = controllerId,
                tok = authToken
            )
        )

    fun encodePairRequest(controllerId: String, controllerName: String, pinCode: String): String =
        json.encodeToString(PairRequestPayload(cid = controllerId, name = controllerName, pin = pinCode))

    fun decodePairResponse(rawJson: String): PairResponsePayload? =
        runCatching { json.decodeFromString<PairResponsePayload>(rawJson) }.getOrNull()

    fun encodeRevokeRequest(controllerId: String, authToken: String, targetControllerId: String): String =
        json.encodeToString(RevokeRequestPayload(cid = controllerId, tok = authToken, targetCid = targetControllerId))

    fun encodeRecoverRequest(recoveryKey: String, newPin: String): String =
        json.encodeToString(RecoverRequestPayload(recoveryKey = recoveryKey, newPin = newPin))

    /**
     * Generates command frames compatible with commercial BLE DMX / Motor LED controllers
     * (LED DMX, LED LAMP FFF0/FFE0, ELK-BLEDOM, Triones/FFD5) as well as RNS Native.
     */
    fun encodeBleFramesForProfile(module: LedModule, profile: BleControllerProfile): List<ByteArray> {
        return when (profile) {
            BleControllerProfile.RNS_NATIVE, BleControllerProfile.AUTO_DETECT -> {
                val binary = encodeCompactBinary(module)
                val needsExtendedJson = module.leftZone.customColorEnabled ||
                    module.rightZone.customColorEnabled ||
                    module.combinationEnabled ||
                    module.splitZonesIndependent
                if (needsExtendedJson) {
                    listOf(binary, encodeModuleState(module).toByteArray(Charsets.UTF_8))
                } else {
                    listOf(binary)
                }
            }
            BleControllerProfile.NORDIC_UART -> {
                // Nordic UART bridges accept full JSON state or 24-byte compact binary
                listOf(
                    encodeCompactBinary(module),
                    (encodeModuleState(module) + "\n").toByteArray(Charsets.UTF_8)
                )
            }
            BleControllerProfile.LED_DMX_BLE -> encodeStandard7eFrames(module)
            BleControllerProfile.LED_LAMP_ELK -> encodeTrionesAnd7eFrames(module)
        }
    }

    /**
     * 9-byte standard BLE DMX / LED LAMP (0x7E ... 0xEF) frame sequence:
     * Power -> Brightness -> Speed -> Mode / Color
     */
    fun encodeStandard7eFrames(module: LedModule): List<ByteArray> {
        val frames = mutableListOf<ByteArray>()
        val pwrByte = if (module.powerOn) 0xF0.toByte() else 0x00.toByte()
        frames += byteArrayOf(
            0x7E, 0x00, 0x04, pwrByte, 0x00, 0x01, 0xFF.toByte(), 0x00, 0xEF.toByte()
        )
        if (!module.powerOn) return frames

        val brPct = ((module.masterBrightness.coerceIn(0, 255) * 100) / 255).coerceIn(1, 100)
        frames += byteArrayOf(
            0x7E, 0x00, 0x01, brPct.toByte(), 0x00, 0x00, 0x00, 0x00, 0xEF.toByte()
        )

        val spdByte = module.speed.coerceIn(1, 100).toByte()
        frames += byteArrayOf(
            0x7E, 0x00, 0x02, spdByte, 0x00, 0x00, 0x00, 0x00, 0xEF.toByte()
        )

        val c = module.leftZone.color
        if (module.effectEngine == EffectEngineMode.DMX210) {
            val dmxPreset = (0x80 + ((module.dmxModeId - 1) % 21)).toByte()
            frames += byteArrayOf(
                0x7E, 0x00, 0x03, dmxPreset, 0x03, 0x00, 0x00, 0x00, 0xEF.toByte()
            )
            if (module.leftZone.customColorEnabled) {
                frames += byteArrayOf(
                    0x7E, 0x00, 0x05, 0x03,
                    c.r.coerceIn(0, 255).toByte(),
                    c.g.coerceIn(0, 255).toByte(),
                    c.b.coerceIn(0, 255).toByte(),
                    0x00, 0xEF.toByte()
                )
            }
        } else if (module.leftZone.animationId == 1 || module.leftZone.customColorEnabled) {
            frames += byteArrayOf(
                0x7E, 0x00, 0x05, 0x03,
                c.r.coerceIn(0, 255).toByte(),
                c.g.coerceIn(0, 255).toByte(),
                c.b.coerceIn(0, 255).toByte(),
                0x00, 0xEF.toByte()
            )
        } else {
            val animPreset = (0x80 + ((module.leftZone.animationId - 1) % 21)).toByte()
            frames += byteArrayOf(
                0x7E, 0x00, 0x03, animPreset, 0x03, 0x00, 0x00, 0x00, 0xEF.toByte()
            )
        }
        return frames
    }

    private fun encodeTrionesAnd7eFrames(module: LedModule): List<ByteArray> {
        val frames = encodeStandard7eFrames(module).toMutableList()
        val c = module.leftZone.color
        // 7-byte Triones / FFD5 static color packet: 0x56 R G B W 0xF0 0xAA
        frames += byteArrayOf(
            0x56,
            c.r.coerceIn(0, 255).toByte(),
            c.g.coerceIn(0, 255).toByte(),
            c.b.coerceIn(0, 255).toByte(),
            c.w.coerceIn(0, 255).toByte(),
            0xF0.toByte(),
            0xAA.toByte()
        )
        return frames
    }
}
