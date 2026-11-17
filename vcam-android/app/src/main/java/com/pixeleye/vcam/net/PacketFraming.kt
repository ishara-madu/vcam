package com.pixeleye.vcam.net

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PacketFraming {
    const val MAGIC: Int = 0x5643414D // "VCAM"
    const val HEADER_SIZE: Int = 17

    const val TYPE_CONFIG: Byte = 0x01       // SPS / PPS parameter sets
    const val TYPE_FRAME_KEY: Byte = 0x02   // IDR Keyframe
    const val TYPE_FRAME_DELTA: Byte = 0x03 // P-frame
    const val TYPE_PING_PONG: Byte = 0x04   // Heartbeat / ping
    const val TYPE_CONTROL: Byte = 0x05     // Control command

    fun buildPacket(type: Byte, ptsUs: Long, payload: ByteArray, offset: Int = 0, length: Int = payload.size): ByteArray {
        val totalSize = HEADER_SIZE + length
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.putInt(MAGIC)
        buffer.put(type)
        buffer.putLong(ptsUs)
        buffer.putInt(length)
        buffer.put(payload, offset, length)

        return buffer.array()
    }
}
