package com.rns.controller

import com.rns.controller.data.protocol.RnsProtocolCodec
import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.EffectEngineMode
import com.rns.controller.domain.model.LedDriverType
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.PlaybackMode
import com.rns.controller.domain.model.RgbwColor
import com.rns.controller.domain.model.ZoneConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RnsProtocolCodecTest {

    @Test
    fun compactBinaryPacketHasValidHeaderAnd24Bytes() {
        val mod = LedModule(
            id = "m1",
            slotNumber = 3,
            name = "Test",
            driver = LedDriverType.SK6812_RGBW,
            powerOn = true,
            masterBrightness = 200,
            playbackMode = PlaybackMode.AUTO_CYCLE,
            splitZonesIndependent = true,
            leftZone = ZoneConfig(color = RgbwColor(10, 20, 30, 40), animationId = 12),
            rightZone = ZoneConfig(color = RgbwColor(50, 60, 70, 80), animationId = 75)
        )
        val bytes = RnsProtocolCodec.encodeCompactBinary(mod)
        assertEquals(24, bytes.size)
        assertEquals(0x52.toByte(), bytes[0]) // 'R'
        assertEquals(0x4E.toByte(), bytes[1]) // 'N'
        assertEquals(0x53.toByte(), bytes[2]) // 'S'
        assertEquals(3.toByte(), bytes[4])
        assertEquals(12.toByte(), bytes[9])
        assertEquals(75.toByte(), bytes[15])
    }

    @Test
    fun compactBinarySetsDmxFlagAndDmxModeByte() {
        val mod = LedModule(
            id = "m-dmx",
            slotNumber = 2,
            name = "DMX",
            effectEngine = EffectEngineMode.DMX210,
            dmxModeId = 139
        )
        val bytes = RnsProtocolCodec.encodeCompactBinary(mod)
        assertEquals(24, bytes.size)
        assertTrue((bytes[5].toInt() and 0x20) != 0)
        assertEquals(139.toByte(), bytes[9])
    }

    @Test
    fun jsonPayloadContainsExpectedFields() {
        val mod = LedModule(
            id = "m2",
            slotNumber = 1,
            name = "ESP",
            effectEngine = EffectEngineMode.DMX210,
            dmxModeId = 42
        )
        val json = RnsProtocolCodec.encodeModuleState(mod)
        assertTrue(json.contains("\"cmd\":\"SET\""))
        assertTrue(json.contains("\"drv\":\"WS2812B\""))
        assertTrue(json.contains("\"engine\":\"DMX210\""))
        assertTrue(json.contains("\"dmx\":42"))
    }

    @Test
    fun customColorPaletteEncodesBinaryFlagAndJsonFields() {
        val mod = LedModule(
            id = "m-custom",
            slotNumber = 4,
            name = "CustomPal",
            splitZonesIndependent = true,
            combinationEnabled = true,
            leftZone = ZoneConfig(
                color = RgbwColor(12, 34, 56, 0),
                secondaryColor = RgbwColor(200, 100, 50, 0),
                customColorEnabled = true,
                animationId = 5
            )
        )
        val bytes = RnsProtocolCodec.encodeCompactBinary(mod)
        assertEquals(24, bytes.size)
        assertTrue((bytes[5].toInt() and 0x40) != 0)
        val json = RnsProtocolCodec.encodeModuleState(mod)
        assertTrue(json.contains("\"ce\":true"))
        assertTrue(json.contains("\"combo\":true"))
        assertTrue(json.contains("\"pal\":"))
    }

    @Test
    fun ledDmxBleProfileProducesValid9Byte7eFrames() {
        val mod = LedModule(
            id = "m-ble-dmx",
            slotNumber = 1,
            name = "LED DMX Motor",
            powerOn = true,
            masterBrightness = 255,
            speed = 75,
            effectEngine = EffectEngineMode.DMX210,
            dmxModeId = 5,
            leftZone = ZoneConfig(color = RgbwColor(255, 80, 10, 0), customColorEnabled = true)
        )
        val frames = RnsProtocolCodec.encodeBleFramesForProfile(mod, BleControllerProfile.LED_DMX_BLE)
        assertEquals(5, frames.size)
        frames.forEach { f ->
            assertEquals(9, f.size)
            assertEquals(0x7E.toByte(), f[0])
            assertEquals(0xEF.toByte(), f[8])
        }
        // Brightness frame: 100%
        assertEquals(0x01.toByte(), frames[1][2])
        assertEquals(100.toByte(), frames[1][3])
        // Speed frame: 75
        assertEquals(0x02.toByte(), frames[2][2])
        assertEquals(75.toByte(), frames[2][3])
        // Custom RGB color frame: 255, 80, 10
        assertEquals(0x05.toByte(), frames[4][2])
        assertEquals(255.toByte(), frames[4][4])
        assertEquals(80.toByte(), frames[4][5])
        assertEquals(10.toByte(), frames[4][6])
    }

    @Test
    fun ledLampElkProfileAppendsTrionesFrame() {
        val mod = LedModule(
            id = "m-elk",
            slotNumber = 2,
            name = "ELK-BLEDOM",
            powerOn = true,
            leftZone = ZoneConfig(color = RgbwColor(15, 220, 95, 40), customColorEnabled = true)
        )
        val frames = RnsProtocolCodec.encodeBleFramesForProfile(mod, BleControllerProfile.LED_LAMP_ELK)
        assertTrue(frames.size >= 5)
        val triones = frames.last()
        assertEquals(7, triones.size)
        assertEquals(0x56.toByte(), triones[0])
        assertEquals(15.toByte(), triones[1])
        assertEquals(220.toByte(), triones[2])
        assertEquals(95.toByte(), triones[3])
        assertEquals(40.toByte(), triones[4])
        assertEquals(0xF0.toByte(), triones[5])
        assertEquals(0xAA.toByte(), triones[6])
    }
}
