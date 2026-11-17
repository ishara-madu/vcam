package com.pixeleye.vcam.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.pixeleye.vcam.MainActivity
import com.pixeleye.vcam.camera.CameraConfig
import com.pixeleye.vcam.camera.CameraManager
import com.pixeleye.vcam.encoder.EncoderConfig
import com.pixeleye.vcam.encoder.VideoEncoder
import com.pixeleye.vcam.net.StreamServer
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VideoResolution

class VCamService : Service() {
    companion object {
        const val CHANNEL_ID = "vcam_stream_service"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_STREAM = "com.pixeleye.vcam.action.START"
        const val ACTION_STOP_STREAM = "com.pixeleye.vcam.action.STOP"

        var instance: VCamService? = null
            private set
    }

    private val binder = LocalBinder()
    private var wakeLock: PowerManager.WakeLock? = null

    private var cameraManager: CameraManager? = null
    private var videoEncoder: VideoEncoder? = null
    private var streamServer: StreamServer? = null

    var onClientConnectedListener: ((String) -> Unit)? = null
    var onClientDisconnectedListener: (() -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): VCamService = this@VCamService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VCam::StreamingLock").apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_STREAM -> stopStreaming()
        }
        return START_NOT_STICKY
    }

    fun startStreaming(
        resolution: VideoResolution,
        fps: Int,
        bitrateMbps: Int,
        facing: CameraFacing,
        lens: LensType,
        port: Int = 8088
    ) {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeLock?.acquire(4 * 60 * 60 * 1000L) // 4 hours maximum wake lock

        // 1. Start TCP Stream Server
        val server = StreamServer(
            port = port,
            onClientConnected = { ip ->
                onClientConnectedListener?.invoke(ip)
            },
            onClientDisconnected = {
                onClientDisconnectedListener?.invoke()
            }
        )
        server.start()
        this.streamServer = server

        // 2. Start Hardware Video Encoder
        val encoderConfig = EncoderConfig(
            width = resolution.width,
            height = resolution.height,
            bitrate = bitrateMbps * 1_000_000,
            fps = fps
        )

        val encoder = VideoEncoder(
            config = encoderConfig,
            onEncodedFrame = { isKey, ptsUs, data ->
                server.enqueueFrame(isKey, ptsUs, data)
            },
            onConfigAvailable = { configData ->
                server.setCachedConfig(configData)
            }
        )
        val inputSurface = encoder.start()
        this.videoEncoder = encoder

        // 3. Start Camera2 Hardware Pipeline
        val camManager = CameraManager(applicationContext)
        val camConfig = CameraConfig(
            facing = facing,
            lens = lens,
            resolution = resolution,
            fps = fps
        )
        camManager.startCamera(camConfig, inputSurface)
        this.cameraManager = camManager
    }

    fun switchCamera(facing: CameraFacing) {
        val surface = videoEncoder?.inputSurface ?: return
        cameraManager?.switchCamera(facing, surface)
    }

    fun setLens(lens: LensType) {
        cameraManager?.setLens(lens)
    }

    fun setTorch(enabled: Boolean) {
        cameraManager?.setTorch(enabled)
    }

    fun setBitrate(bitrateMbps: Int) {
        videoEncoder?.setBitrate(bitrateMbps * 1_000_000)
    }

    fun stopStreaming() {
        cameraManager?.stopCamera()
        cameraManager = null

        videoEncoder?.stop()
        videoEncoder = null

        streamServer?.stop()
        streamServer = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "vcam Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps camera stream running to macOS Virtual Camera"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("vcam Virtual Camera Active")
            .setContentText("Streaming camera to macOS system-wide virtual webcam")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopStreaming()
        instance = null
        super.onDestroy()
    }
}
