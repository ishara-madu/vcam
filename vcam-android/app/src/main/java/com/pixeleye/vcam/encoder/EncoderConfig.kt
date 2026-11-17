package com.pixeleye.vcam.encoder

import android.media.MediaFormat

data class EncoderConfig(
    val width: Int = 1920,
    val height: Int = 1080,
    val bitrate: Int = 12_000_000,
    val fps: Int = 60,
    val iFrameInterval: Int = 1,
    val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC
)
