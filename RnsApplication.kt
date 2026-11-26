package com.rns.controller

import android.app.Application
import com.rns.controller.data.repository.ModuleRepository
import com.rns.controller.data.transport.Esp32BleTransport
import com.rns.controller.data.transport.WifiUdpTransport

class RnsApplication : Application() {
    lateinit var repository: ModuleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val wifiTransport = WifiUdpTransport()
        val bleTransport = Esp32BleTransport(this)
        repository = ModuleRepository(this, wifiTransport, bleTransport)
    }
}
