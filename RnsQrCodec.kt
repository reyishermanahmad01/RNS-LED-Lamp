package com.rns.controller.data.protocol

import android.net.Uri
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.LedDriverType
import com.rns.controller.domain.model.LedModule
import com.rns.controller.domain.model.WifiTopology

data class ParsedModuleQr(
    val uniqueCode: String,
    val name: String,
    val target: HardwareTarget,
    val driver: LedDriverType,
    val wifiTopology: WifiTopology,
    val ipAddress: String,
    val udpPort: Int,
    val leftDataPin: Int,
    val rightDataPin: Int
)

object RnsQrCodec {

    fun generateUniqueCode(slotNumber: Int): String {
        val randHex = (0x1000..0xFFFF).random().toString(16).uppercase()
        return "RNS-M${slotNumber.toString().padStart(2, '0')}-$randHex"
    }

    fun encodeModuleQrUri(module: LedModule): String {
        return Uri.Builder()
            .scheme("rns")
            .authority("module")
            .appendQueryParameter("uid", module.uniqueCode)
            .appendQueryParameter("name", module.name)
            .appendQueryParameter("target", module.target.name)
            .appendQueryParameter("driver", module.driver.name)
            .appendQueryParameter("topo", module.wifiTopology.name)
            .appendQueryParameter("ip", module.ipAddress)
            .appendQueryParameter("port", module.udpPort.toString())
            .appendQueryParameter("lpin", module.pins.leftDataPin.toString())
            .appendQueryParameter("rpin", module.pins.rightDataPin.toString())
            .build()
            .toString()
    }

    fun parseModuleQr(raw: String): ParsedModuleQr? {
        val text = raw.trim()
        if (text.isBlank()) return null

        if (text.startsWith("rns://module")) {
            val uri = Uri.parse(text)
            val uid = uri.getQueryParameter("uid")?.takeIf { it.isNotBlank() } ?: return null
            val target = runCatching {
                HardwareTarget.valueOf(uri.getQueryParameter("target") ?: "")
            }.getOrDefault(HardwareTarget.ESP8266_WIFI)
            val driver = runCatching {
                LedDriverType.valueOf(uri.getQueryParameter("driver") ?: "")
            }.getOrDefault(LedDriverType.WS2812B)
            val topo = runCatching {
                WifiTopology.valueOf(uri.getQueryParameter("topo") ?: "")
            }.getOrDefault(WifiTopology.ACCESS_POINT)

            return ParsedModuleQr(
                uniqueCode = uid,
                name = uri.getQueryParameter("name")?.takeIf { it.isNotBlank() } ?: "Modul $uid",
                target = target,
                driver = driver,
                wifiTopology = topo,
                ipAddress = uri.getQueryParameter("ip")?.takeIf { it.isNotBlank() } ?: "192.168.4.1",
                udpPort = uri.getQueryParameter("port")?.toIntOrNull() ?: 4210,
                leftDataPin = uri.getQueryParameter("lpin")?.toIntOrNull() ?: 2,
                rightDataPin = uri.getQueryParameter("rpin")?.toIntOrNull() ?: 4
            )
        }

        if (Regex("^RNS-[A-Z0-9-]+$", RegexOption.IGNORE_CASE).matches(text)) {
            val upper = text.uppercase()
            return ParsedModuleQr(
                uniqueCode = upper,
                name = "Modul $upper",
                target = HardwareTarget.ESP8266_WIFI,
                driver = LedDriverType.WS2812B,
                wifiTopology = WifiTopology.ACCESS_POINT,
                ipAddress = "192.168.4.1",
                udpPort = 4210,
                leftDataPin = 2,
                rightDataPin = 4
            )
        }

        return null
    }
}
