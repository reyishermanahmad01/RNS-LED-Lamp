package com.rns.controller.data.transport

import com.rns.controller.data.protocol.RnsProtocolCodec
import com.rns.controller.domain.model.DiscoveredDevice
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.LedModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URL

class WifiUdpTransport {

    suspend fun sendModuleCommand(module: LedModule, controllerId: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = RnsProtocolCodec.encodeModuleState(module, controllerId)
            val bytes = payload.toByteArray(Charsets.UTF_8)
            DatagramSocket().use { socket ->
                socket.soTimeout = 1500
                val address = InetAddress.getByName(module.ipAddress)
                val packet = DatagramPacket(bytes, bytes.size, address, module.udpPort)
                socket.send(packet)
            }
        }
    }

    suspend fun checkDeviceHealth(ipAddress: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("http://$ipAddress/api/status")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 2500
                readTimeout = 2500
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code dari $ipAddress")
            }
            conn.inputStream.bufferedReader().use { it.readText() }
        }
    }

    suspend fun provisionWifiCredentials(
        ipAddress: String,
        mode: String,
        ssid: String,
        password: String,
        controllerId: String = "",
        authToken: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        postJson(
            ipAddress = ipAddress,
            path = "/api/wifi",
            bodyJson = RnsProtocolCodec.encodeWifiProvision(mode, ssid, password, controllerId, authToken)
        )
    }

    suspend fun pairWithModule(
        ipAddress: String,
        controllerId: String,
        controllerName: String,
        pinCode: String
    ): Result<com.rns.controller.data.protocol.PairResponsePayload> = withContext(Dispatchers.IO) {
        postJson(
            ipAddress = ipAddress,
            path = "/api/pair",
            bodyJson = RnsProtocolCodec.encodePairRequest(controllerId, controllerName, pinCode)
        ).mapCatching { body ->
            val parsed = RnsProtocolCodec.decodePairResponse(body)
                ?: throw IllegalStateException("Respons pairing modul tidak valid")
            if (!parsed.ok || parsed.token.isBlank()) {
                throw IllegalStateException(parsed.error ?: "Pairing ditolak oleh modul")
            }
            parsed
        }
    }

    suspend fun revokeControllerOnModule(
        ipAddress: String,
        controllerId: String,
        authToken: String,
        targetControllerId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        postJson(
            ipAddress = ipAddress,
            path = "/api/security/revoke",
            bodyJson = RnsProtocolCodec.encodeRevokeRequest(controllerId, authToken, targetControllerId)
        )
    }

    suspend fun recoverModuleSecurity(
        ipAddress: String,
        recoveryKey: String,
        newPin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        postJson(
            ipAddress = ipAddress,
            path = "/api/security/recover",
            bodyJson = RnsProtocolCodec.encodeRecoverRequest(recoveryKey, newPin)
        )
    }

    private fun postJson(ipAddress: String, path: String, bodyJson: String): Result<String> = runCatching {
        val url = URL("http://$ipAddress$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(conn.outputStream).use { writer ->
            writer.write(bodyJson)
            writer.flush()
        }
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw IllegalStateException("HTTP $code: $body")
        body
    }

    suspend fun discoverLanModules(udpPort: Int = 4210): List<DiscoveredDevice> =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<DiscoveredDevice>()
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    socket.soTimeout = 1200
                    val probe = "RNS_DISCOVER".toByteArray(Charsets.UTF_8)
                    val broadcastPacket = DatagramPacket(
                        probe,
                        probe.size,
                        InetAddress.getByName("255.255.255.255"),
                        udpPort
                    )
                    socket.send(broadcastPacket)

                    val buffer = ByteArray(512)
                    val deadline = System.currentTimeMillis() + 1800L
                    while (System.currentTimeMillis() < deadline) {
                        val responsePacket = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(responsePacket)
                            val msg = String(responsePacket.data, 0, responsePacket.length, Charsets.UTF_8)
                            // Format: RNS_HELLO|<BOARD>|<NAME>|<DRIVER>
                            if (msg.startsWith("RNS_HELLO|")) {
                                val parts = msg.split("|")
                                val board = parts.getOrNull(1) ?: "ESP8266"
                                val name = parts.getOrNull(2) ?: "RNS Module"
                                val driver = parts.getOrNull(3) ?: "WS2812B"
                                val target = if (board.contains("ESP32", ignoreCase = true)) {
                                    HardwareTarget.ESP32_WIFI
                                } else {
                                    HardwareTarget.ESP8266_WIFI
                                }
                                val ip = responsePacket.address.hostAddress ?: continue
                                if (found.none { it.address == ip }) {
                                    found.add(
                                        DiscoveredDevice(
                                            name = name,
                                            address = ip,
                                            target = target,
                                            driver = driver
                                        )
                                    )
                                }
                            }
                        } catch (_: SocketTimeoutException) {
                            break
                        }
                    }
                }
            }
            found
        }
}
