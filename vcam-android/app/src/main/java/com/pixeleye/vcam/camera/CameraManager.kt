package com.pixeleye.vcam.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType

class CameraManager(private val context: Context) {
    companion object {
        private const val TAG = "VCamCameraManager"
    }

    private val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var currentConfig = CameraConfig()

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("VCamCameraBg").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join(500)
        } catch (_: Exception) {}
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    fun startCamera(config: CameraConfig, surface: Surface, onReady: () -> Unit = {}) {
        this.currentConfig = config
        this.previewSurface = surface

        startBackgroundThread()

        val cameraId = findCameraId(config.facing) ?: run {
            Log.e(TAG, "No matching camera ID found for facing: ${config.facing}")
            return
        }

        try {
            systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera, surface, onReady)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun createCaptureSession(camera: CameraDevice, surface: Surface, onReady: () -> Unit) {
        try {
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        applyRepeatingRequest()
                        onReady()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session: ${e.message}", e)
        }
    }

    private fun applyRepeatingRequest() {
        val camera = cameraDevice ?: return
        val session = captureSession ?: return
        val surface = previewSurface ?: return

        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(surface)

                // Continuous autofocus for video
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)

                // High framerate target
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(currentConfig.fps / 2, currentConfig.fps))

                // Torch mode
                if (currentConfig.facing == CameraFacing.BACK) {
                    val flashMode = if (currentConfig.isTorchOn) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                    set(CaptureRequest.FLASH_MODE, flashMode)
                }

                // Lens zoom ratio (0.5x, 1.0x, 2.0x)
                applyZoom(this, currentConfig.lens)
            }

            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply repeating request: ${e.message}", e)
        }
    }

    private fun applyZoom(builder: CaptureRequest.Builder, lens: LensType) {
        val zoomRatio = when (lens) {
            LensType.ULTRAWIDE -> 0.6f
            LensType.WIDE -> 1.0f
            LensType.TELEPHOTO -> 2.0f
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoomRatio)
            } catch (_: Exception) {}
        }
    }

    fun setTorch(enabled: Boolean) {
        currentConfig = currentConfig.copy(isTorchOn = enabled)
        applyRepeatingRequest()
    }

    fun setLens(lens: LensType) {
        currentConfig = currentConfig.copy(lens = lens)
        applyRepeatingRequest()
    }

    fun switchCamera(facing: CameraFacing, surface: Surface, onReady: () -> Unit = {}) {
        stopCamera()
        startCamera(currentConfig.copy(facing = facing), surface, onReady)
    }

    fun stopCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
        } catch (_: Exception) {}
        captureSession = null

        try {
            cameraDevice?.close()
        } catch (_: Exception) {}
        cameraDevice = null

        stopBackgroundThread()
    }

    private fun findCameraId(facing: CameraFacing): String? {
        val targetFacing = if (facing == CameraFacing.FRONT) {
            CameraCharacteristics.LENS_FACING_FRONT
        } else {
            CameraCharacteristics.LENS_FACING_BACK
        }

        return systemCameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = systemCameraManager.getCameraCharacteristics(id)
            characteristics.get(CameraCharacteristics.LENS_FACING) == targetFacing
        }
    }
}
