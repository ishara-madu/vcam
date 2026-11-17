package com.pixeleye.vcam

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pixeleye.vcam.net.NetworkUtils
import com.pixeleye.vcam.service.VCamService
import com.pixeleye.vcam.ui.MainScreen
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VideoResolution
import com.pixeleye.vcam.ui.theme.VcamTheme

class MainActivity : ComponentActivity() {

    private var vCamService: VCamService? = null
    private var isBound = false

    private var onMacClientConnected: (String) -> Unit = {}
    private var onMacClientDisconnected: () -> Unit = {}
    private var onIpAddressUpdated: (String) -> Unit = {}

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VCamService.LocalBinder
            vCamService = binder.getService()
            isBound = true

            vCamService?.onClientConnectedListener = { clientIp ->
                runOnUiThread {
                    onMacClientConnected(clientIp)
                }
            }
            vCamService?.onClientDisconnectedListener = {
                runOnUiThread {
                    onMacClientDisconnected()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vCamService = null
            isBound = false
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required to stream video to Mac", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissions()

        val serviceIntent = Intent(this, VCamService::class.java)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        val initialIp = NetworkUtils.getLocalIpAddress(this)
        setupNetworkMonitoring()

        setContent {
            VcamTheme {
                MainScreen(
                    initialIp = initialIp,
                    onStartStreamRequested = { resolution, fps, bitrate, facing, lens, port ->
                        startStreamingService(resolution, fps, bitrate, facing, lens, port)
                    },
                    onStopStreamRequested = {
                        vCamService?.stopStreaming()
                    },
                    onSwitchCameraRequested = { facing ->
                        vCamService?.switchCamera(facing)
                    },
                    onLensChanged = { lens ->
                        vCamService?.setLens(lens)
                    },
                    onTorchToggled = { enabled ->
                        vCamService?.setTorch(enabled)
                    },
                    onBitrateChanged = { bitrateMbps ->
                        vCamService?.setBitrate(bitrateMbps)
                    },
                    registerClientCallbacks = { onConnected, onDisconnected, onIpUpdated ->
                        this.onMacClientConnected = onConnected
                        this.onMacClientDisconnected = onDisconnected
                        this.onIpAddressUpdated = onIpUpdated
                    }
                )
            }
        }
    }

    private fun setupNetworkMonitoring() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        this.connectivityManager = cm

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshIp()
            }

            override fun onLost(network: Network) {
                refreshIp()
            }
        }

        this.networkCallback = callback
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}
    }

    private fun refreshIp() {
        val freshIp = NetworkUtils.getLocalIpAddress(this)
        runOnUiThread {
            onIpAddressUpdated(freshIp)
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startStreamingService(
        resolution: VideoResolution,
        fps: Int,
        bitrateMbps: Int,
        facing: CameraFacing,
        lens: LensType,
        port: Int
    ) {
        val intent = Intent(this, VCamService::class.java).apply {
            action = VCamService.ACTION_START_STREAM
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        vCamService?.startStreaming(resolution, fps, bitrateMbps, facing, lens, port)
    }

    override fun onDestroy() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
