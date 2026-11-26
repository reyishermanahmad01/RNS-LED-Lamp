package com.rns.controller.data.transport

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.rns.controller.data.protocol.RnsProtocolCodec
import com.rns.controller.domain.model.BleControllerProfile
import com.rns.controller.domain.model.DiscoveredDevice
import com.rns.controller.domain.model.HardwareTarget
import com.rns.controller.domain.model.LedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class Esp32BleTransport(private val context: Context) {

    companion object {
        // RNS Native ESP32 Service & Characteristics
        val RNS_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
        val RNS_RX_CHAR_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val RNS_TX_CHAR_UUID: UUID = UUID.fromString("beb5483f-36e1-4688-b7f5-ea07361b26a8")

        // Nordic UART Service (ESP32 BLE UART / DMX Bridges)
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

        // Commercial LED DMX / LED LAMP / ELK-BLEDOM / Motor Controller GATT UUIDs
        val FFF0_SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val FFF3_CHAR_UUID: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
        val FFF1_CHAR_UUID: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

        val FFE0_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val FFE1_CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

        val AE00_SERVICE_UUID: UUID = UUID.fromString("0000ae00-0000-1000-8000-00805f9b34fb")
        val AE01_CHAR_UUID: UUID = UUID.fromString("0000ae01-0000-1000-8000-00805f9b34fb")

        val FFD5_SERVICE_UUID: UUID = UUID.fromString("0000ffd5-0000-1000-8000-00805f9b34fb")
        val FFD9_CHAR_UUID: UUID = UUID.fromString("0000ffd9-0000-1000-8000-00805f9b34fb")

        // Classic Bluetooth SPP RFCOMM UUID (HC-05 / HC-06 / Classic DMX Motor Modules)
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        private val GENERIC_ACCESS_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        private val GENERIC_ATTRIB_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
    }

    private data class ResolvedGattEndpoint(
        val characteristic: BluetoothGattCharacteristic,
        val profile: BleControllerProfile,
        val writeType: Int
    )

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val mainHandler = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeGatts = ConcurrentHashMap<String, BluetoothGatt>()
    private val resolvedEndpoints = ConcurrentHashMap<String, ResolvedGattEndpoint>()
    private val negotiatedMtu = ConcurrentHashMap<String, Int>()
    private val writeQueues = ConcurrentHashMap<String, ConcurrentLinkedQueue<ByteArray>>()
    private val writingNow = ConcurrentHashMap<String, Boolean>()

    // Classic SPP sockets fallback for dual-mode or HC-05/DMX Bluetooth modules
    private val sppSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val sppStreams = ConcurrentHashMap<String, OutputStream>()
    private val sppProfiles = ConcurrentHashMap<String, BleControllerProfile>()

    private val _scannedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val scannedDevices: StateFlow<List<DiscoveredDevice>> = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bleStatusMessage = MutableStateFlow<String?>(null)
    val bleStatusMessage: StateFlow<String?> = _bleStatusMessage.asStateFlow()

    private var activeScanCallback: ScanCallback? = null
    private var autoStopRunnable: Runnable? = null

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isBluetoothReady(): Boolean = bluetoothAdapter?.isEnabled == true

    fun clearStatusMessage() {
        _bleStatusMessage.value = null
    }

    private fun inferProfileFromDevice(name: String, serviceUuids: List<UUID>): BleControllerProfile {
        if (serviceUuids.any { it == RNS_SERVICE_UUID } || name.contains("RNS", ignoreCase = true)) {
            return BleControllerProfile.RNS_NATIVE
        }
        if (serviceUuids.any { it == NUS_SERVICE_UUID }) {
            return BleControllerProfile.NORDIC_UART
        }
        if (serviceUuids.any { it == FFD5_SERVICE_UUID } ||
            name.contains("ELK", ignoreCase = true) ||
            name.contains("BLEDOM", ignoreCase = true) ||
            name.contains("TRIONES", ignoreCase = true)
        ) {
            return BleControllerProfile.LED_LAMP_ELK
        }
        if (serviceUuids.any { it == FFF0_SERVICE_UUID || it == FFE0_SERVICE_UUID || it == AE00_SERVICE_UUID } ||
            name.contains("DMX", ignoreCase = true) ||
            name.contains("LED", ignoreCase = true) ||
            name.contains("SP10", ignoreCase = true) ||
            name.contains("CAR", ignoreCase = true)
        ) {
            return BleControllerProfile.LED_DMX_BLE
        }
        return BleControllerProfile.AUTO_DETECT
    }

    private fun isLedOrDmxMatch(name: String, serviceUuids: List<UUID>): Boolean {
        val keywords = listOf("RNS", "DMX", "LED", "ELK", "BLEDOM", "TRIONES", "SP10", "SP11", "LAMP", "MOTOR", "CAR", "ESP32", "RGB", "BT-")
        if (keywords.any { name.contains(it, ignoreCase = true) }) return true
        val knownServices = setOf(RNS_SERVICE_UUID, NUS_SERVICE_UUID, FFF0_SERVICE_UUID, FFE0_SERVICE_UUID, AE00_SERVICE_UUID, FFD5_SERVICE_UUID)
        return serviceUuids.any { it in knownServices }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _bleStatusMessage.value = "Perangkat Android ini tidak memiliki modul Bluetooth."
            return
        }
        if (!adapter.isEnabled) {
            _bleStatusMessage.value = "Bluetooth nonaktif. Aktifkan Bluetooth di HP terlebih dahulu."
            return
        }
        if (!hasScanPermission()) {
            _bleStatusMessage.value = "Izin Bluetooth / Lokasi diperlukan untuk memindai modul DMX & ESP32."
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _bleStatusMessage.value = "BLE Scanner tidak siap. Pastikan Bluetooth menyala."
            return
        }
        if (_isScanning.value) return

        // Pre-populate bonded/paired devices (important for paired DMX Motor & Classic Bluetooth modules)
        val initialList = mutableListOf<DiscoveredDevice>()
        runCatching {
            adapter.bondedDevices?.forEach { bonded ->
                val bName = bonded.name ?: "Bluetooth Device"
                val addr = bonded.address ?: return@forEach
                val uuids = bonded.uuids?.mapNotNull { it.uuid } ?: emptyList()
                val profile = inferProfileFromDevice(bName, uuids)
                if (isLedOrDmxMatch(bName, uuids) || bonded.type != BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    initialList += DiscoveredDevice(
                        name = bName,
                        address = addr,
                        target = HardwareTarget.ESP32_BLE,
                        rssi = -45,
                        detectedProfile = profile,
                        isBonded = true
                    )
                }
            }
        }
        _scannedDevices.value = initialList
        _isScanning.value = true
        _bleStatusMessage.value = "Memindai modul BLE RNS, LED DMX Motor, & ESP32..."

        // Open BLE scan (NO strict ScanFilter) so LED DMX motor, ELK-BLEDOM, FFF0/FFE0 & ESP32 all appear
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val addr = device.address ?: return
                val rawName = result.scanRecord?.deviceName ?: device.name
                val displayName = rawName?.takeIf { it.isNotBlank() } ?: "BLE Device ($addr)"
                val advUuids = result.scanRecord?.serviceUuids?.mapNotNull { it.uuid } ?: emptyList()
                val profile = inferProfileFromDevice(displayName, advUuids)

                val current = _scannedDevices.value.toMutableList()
                val existingIdx = current.indexOfFirst { it.address == addr }
                val entry = DiscoveredDevice(
                    name = displayName,
                    address = addr,
                    target = HardwareTarget.ESP32_BLE,
                    rssi = result.rssi,
                    detectedProfile = profile,
                    isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                )
                if (existingIdx >= 0) {
                    current[existingIdx] = entry
                } else {
                    current.add(entry)
                }
                // Prioritize RNS / DMX / LED controllers at top, then by RSSI
                _scannedDevices.value = current.sortedWith(
                    compareByDescending<DiscoveredDevice> { isLedOrDmxMatch(it.name, advUuids) }
                        .thenByDescending { it.isBonded }
                        .thenByDescending { it.rssi }
                )
            }

            override fun onScanFailed(errorCode: Int) {
                _isScanning.value = false
                _bleStatusMessage.value = "Scan BLE gagal (kode $errorCode). Pastikan Lokasi/GPS HP aktif."
            }
        }

        activeScanCallback = callback
        try {
            scanner.startScan(null, settings, callback)
            autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
            autoStopRunnable = Runnable { stopScan() }.also {
                mainHandler.postDelayed(it, 15000L)
            }
        } catch (se: SecurityException) {
            _isScanning.value = false
            _bleStatusMessage.value = "Izin Bluetooth ditolak: ${se.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        autoStopRunnable?.let { mainHandler.removeCallbacks(it) }
        autoStopRunnable = null
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        activeScanCallback?.let { cb ->
            runCatching { scanner?.stopScan(cb) }
        }
        activeScanCallback = null
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(
        address: String,
        preferredProfile: BleControllerProfile = BleControllerProfile.AUTO_DETECT,
        onConnected: (BleControllerProfile) -> Unit,
        onDisconnected: (String?) -> Unit
    ) {
        val adapter = bluetoothAdapter ?: run {
            onDisconnected("Adapter Bluetooth tidak tersedia")
            return
        }
        if (!adapter.isEnabled) {
            onDisconnected("Bluetooth mati. Aktifkan Bluetooth terlebih dahulu.")
            return
        }
        if (!hasConnectPermission()) {
            onDisconnected("Izin BLUETOOTH_CONNECT belum diberikan.")
            return
        }

        stopScan()

        val cleanAddr = address.trim().uppercase()
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(cleanAddr)
        } catch (e: IllegalArgumentException) {
            onDisconnected("Alamat MAC Bluetooth tidak valid: $address")
            return
        }

        disconnect(cleanAddr)

        // If Classic-only DMX Bluetooth module (HC-05/HC-06), go straight to RFCOMM SPP
        if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            connectClassicSpp(device, preferredProfile, onConnected, onDisconnected)
            return
        }

        var notifiedConnected = false
        val gatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        negotiatedMtu[cleanAddr] = 23
                        gatt.requestMtu(256)
                        mainHandler.postDelayed({ gatt.discoverServices() }, 180L)
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        resolvedEndpoints.remove(cleanAddr)
                        activeGatts.remove(cleanAddr)
                        writeQueues.remove(cleanAddr)
                        writingNow.remove(cleanAddr)
                        runCatching { gatt.close() }

                        // Error 133 or dual-mode classic fallback when GATT hasn't connected yet
                        if (!notifiedConnected && (status == 133 || device.type == BluetoothDevice.DEVICE_TYPE_DUAL)) {
                            connectClassicSpp(device, preferredProfile, onConnected, onDisconnected)
                            return
                        }

                        onDisconnected(
                            if (status != BluetoothGatt.GATT_SUCCESS && status != 0) {
                                "Bluetooth BLE terputus (kode $status). Pastikan jarak dekat & modul menyala."
                            } else null
                        )
                    }
                }

                override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        negotiatedMtu[cleanAddr] = mtu.coerceAtLeast(23)
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        onDisconnected("Gagal membaca service GATT BLE ($status)")
                        return
                    }
                    val resolved = resolveCompatibleEndpoint(gatt, preferredProfile)
                    if (resolved != null) {
                        resolvedEndpoints[cleanAddr] = resolved
                        writeQueues[cleanAddr] = ConcurrentLinkedQueue()
                        writingNow[cleanAddr] = false
                        notifiedConnected = true
                        onConnected(resolved.profile)
                    } else {
                        runCatching { gatt.disconnect(); gatt.close() }
                        if (device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
                            connectClassicSpp(device, preferredProfile, onConnected, onDisconnected)
                        } else {
                            onDisconnected("Tidak ditemukan karakteristik kontrol LED/DMX pada perangkat BLE ini.")
                        }
                    }
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    writingNow[cleanAddr] = false
                    drainNextWrite(cleanAddr)
                }
            },
            BluetoothDevice.TRANSPORT_LE
        )

        activeGatts[cleanAddr] = gatt
    }

    @SuppressLint("MissingPermission")
    private fun connectClassicSpp(
        device: BluetoothDevice,
        preferredProfile: BleControllerProfile,
        onConnected: (BleControllerProfile) -> Unit,
        onDisconnected: (String?) -> Unit
    ) {
        ioScope.launch {
            runCatching {
                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                val out = socket.outputStream
                val addr = device.address
                sppSockets[addr] = socket
                sppStreams[addr] = out
                val resolvedProfile = if (preferredProfile == BleControllerProfile.AUTO_DETECT) {
                    BleControllerProfile.LED_DMX_BLE
                } else preferredProfile
                sppProfiles[addr] = resolvedProfile
                mainHandler.post { onConnected(resolvedProfile) }
            }.onFailure { err ->
                mainHandler.post {
                    onDisconnected("Koneksi Bluetooth DMX/SPP gagal: ${err.message ?: "perangkat tidak merespons"}")
                }
            }
        }
    }

    private fun resolveCompatibleEndpoint(
        gatt: BluetoothGatt,
        preferred: BleControllerProfile
    ): ResolvedGattEndpoint? {
        fun pickWriteType(ch: BluetoothGattCharacteristic): Int {
            val props = ch.properties
            return if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
        }

        fun hasWrite(ch: BluetoothGattCharacteristic): Boolean {
            val p = ch.properties
            return (p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
        }

        // 1. Check RNS Native ESP32 first (or when explicitly preferred)
        if (preferred == BleControllerProfile.RNS_NATIVE || preferred == BleControllerProfile.AUTO_DETECT) {
            gatt.getService(RNS_SERVICE_UUID)?.getCharacteristic(RNS_RX_CHAR_UUID)?.let { ch ->
                return ResolvedGattEndpoint(ch, BleControllerProfile.RNS_NATIVE, pickWriteType(ch))
            }
        }

        // 2. Check Standard LED DMX / LED LAMP BLE controllers (FFF0/FFE0/AE00)
        if (preferred == BleControllerProfile.LED_DMX_BLE || preferred == BleControllerProfile.AUTO_DETECT) {
            gatt.getService(FFF0_SERVICE_UUID)?.let { svc ->
                val ch = svc.getCharacteristic(FFF3_CHAR_UUID)
                    ?: svc.getCharacteristic(FFF1_CHAR_UUID)
                    ?: svc.characteristics.firstOrNull { hasWrite(it) }
                if (ch != null) return ResolvedGattEndpoint(ch, BleControllerProfile.LED_DMX_BLE, pickWriteType(ch))
            }
            gatt.getService(FFE0_SERVICE_UUID)?.let { svc ->
                val ch = svc.getCharacteristic(FFE1_CHAR_UUID)
                    ?: svc.characteristics.firstOrNull { hasWrite(it) }
                if (ch != null) return ResolvedGattEndpoint(ch, BleControllerProfile.LED_DMX_BLE, pickWriteType(ch))
            }
            gatt.getService(AE00_SERVICE_UUID)?.let { svc ->
                val ch = svc.getCharacteristic(AE01_CHAR_UUID)
                    ?: svc.characteristics.firstOrNull { hasWrite(it) }
                if (ch != null) return ResolvedGattEndpoint(ch, BleControllerProfile.LED_DMX_BLE, pickWriteType(ch))
            }
        }

        // 3. Check ELK-BLEDOM / Triones FFD5
        if (preferred == BleControllerProfile.LED_LAMP_ELK || preferred == BleControllerProfile.AUTO_DETECT) {
            gatt.getService(FFD5_SERVICE_UUID)?.let { svc ->
                val ch = svc.getCharacteristic(FFD9_CHAR_UUID)
                    ?: svc.characteristics.firstOrNull { hasWrite(it) }
                if (ch != null) return ResolvedGattEndpoint(ch, BleControllerProfile.LED_LAMP_ELK, pickWriteType(ch))
            }
        }

        // 4. Check Nordic UART (NUS)
        if (preferred == BleControllerProfile.NORDIC_UART || preferred == BleControllerProfile.AUTO_DETECT) {
            gatt.getService(NUS_SERVICE_UUID)?.getCharacteristic(NUS_RX_CHAR_UUID)?.let { ch ->
                return ResolvedGattEndpoint(ch, BleControllerProfile.NORDIC_UART, pickWriteType(ch))
            }
        }

        // 5. Fallback: any writable characteristic in non-system GATT services
        for (svc in gatt.services) {
            if (svc.uuid == GENERIC_ACCESS_UUID || svc.uuid == GENERIC_ATTRIB_UUID) continue
            val writable = svc.characteristics.firstOrNull { hasWrite(it) }
            if (writable != null) {
                val fallbackProfile = if (preferred == BleControllerProfile.AUTO_DETECT) {
                    BleControllerProfile.LED_DMX_BLE
                } else preferred
                return ResolvedGattEndpoint(writable, fallbackProfile, pickWriteType(writable))
            }
        }

        return null
    }

    @SuppressLint("MissingPermission")
    fun disconnect(address: String) {
        val clean = address.trim().uppercase()
        activeGatts.remove(clean)?.let {
            runCatching {
                it.disconnect()
                it.close()
            }
        }
        resolvedEndpoints.remove(clean)
        writeQueues.remove(clean)
        writingNow.remove(clean)

        sppStreams.remove(clean)?.let { runCatching { it.close() } }
        sppSockets.remove(clean)?.let { runCatching { it.close() } }
        sppProfiles.remove(clean)
    }

    @SuppressLint("MissingPermission")
    fun sendModuleCommand(module: LedModule): Boolean {
        val addr = module.bleDeviceAddress.trim().uppercase()
        if (addr.isBlank()) return false

        // Classic SPP socket active?
        val sppOut = sppStreams[addr]
        if (sppOut != null) {
            val profile = if (module.bleProfile == BleControllerProfile.AUTO_DETECT) {
                sppProfiles[addr] ?: BleControllerProfile.LED_DMX_BLE
            } else module.bleProfile
            val frames = RnsProtocolCodec.encodeBleFramesForProfile(module, profile)
            ioScope.launch {
                runCatching {
                    frames.forEach { frame ->
                        sppOut.write(frame)
                        sppOut.flush()
                    }
                }
            }
            return true
        }

        val endpoint = resolvedEndpoints[addr] ?: return false
        if (activeGatts[addr] == null) return false

        val effectiveProfile = if (module.bleProfile == BleControllerProfile.AUTO_DETECT) {
            endpoint.profile
        } else module.bleProfile

        val frames = RnsProtocolCodec.encodeBleFramesForProfile(module, effectiveProfile)
        val queue = writeQueues.getOrPut(addr) { ConcurrentLinkedQueue() }
        val maxChunk = (negotiatedMtu[addr] ?: 23).minus(3).coerceAtLeast(20)

        // Avoid flooding queue if slider drags rapidly
        if (queue.size > 8) queue.clear()

        frames.forEach { frame ->
            if (frame.size <= maxChunk) {
                queue.add(frame)
            } else {
                var offset = 0
                while (offset < frame.size) {
                    val end = (offset + maxChunk).coerceAtMost(frame.size)
                    queue.add(frame.copyOfRange(offset, end))
                    offset = end
                }
            }
        }

        drainNextWrite(addr)
        return true
    }

    @SuppressLint("MissingPermission")
    private fun drainNextWrite(address: String) {
        if (writingNow[address] == true) return
        val gatt = activeGatts[address] ?: return
        val endpoint = resolvedEndpoints[address] ?: return
        val queue = writeQueues[address] ?: return
        val nextChunk = queue.poll() ?: return

        writingNow[address] = true
        val char = endpoint.characteristic
        val ok = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, nextChunk, endpoint.writeType) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = nextChunk
            @Suppress("DEPRECATION")
            char.writeType = endpoint.writeType
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
        if (!ok) {
            writingNow[address] = false
        } else if (endpoint.writeType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
            // Pace consecutive NO_RESPONSE writes cleanly across all DMX BLE modules
            mainHandler.postDelayed({
                writingNow[address] = false
                drainNextWrite(address)
            }, 22L)
        }
    }
}
