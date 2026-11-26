package com.rns.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rns.controller.ui.RnsAppRoot
import com.rns.controller.ui.theme.RnsTheme
import com.rns.controller.ui.viewmodel.RnsViewModel
import com.rns.controller.ui.viewmodel.RnsViewModelFactory

class MainActivity : ComponentActivity() {

    private var pendingBleAction: (() -> Unit)? = null

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        val action = pendingBleAction
        pendingBleAction = null
        if (allGranted) {
            action?.invoke()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RnsApplication

        setContent {
            RnsTheme {
                val vm: RnsViewModel = viewModel(
                    factory = RnsViewModelFactory(app.repository)
                )
                RnsAppRoot(
                    viewModel = vm,
                    onRequestBlePermissions = { onGranted ->
                        ensureBlePermissions(onGranted)
                    }
                )
            }
        }
    }

    private fun requiredBlePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun ensureBlePermissions(onGranted: () -> Unit) {
        val perms = requiredBlePermissions()
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onGranted()
        } else {
            pendingBleAction = onGranted
            blePermissionLauncher.launch(perms)
        }
    }
}
