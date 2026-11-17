package com.pixeleye.vcam.camera

import com.pixeleye.vcam.ui.state.CameraFacing
import com.pixeleye.vcam.ui.state.LensType
import com.pixeleye.vcam.ui.state.VideoResolution

data class CameraConfig(
    val facing: CameraFacing = CameraFacing.BACK,
    val lens: LensType = LensType.WIDE,
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val fps: Int = 60,
    val isTorchOn: Boolean = false
)
