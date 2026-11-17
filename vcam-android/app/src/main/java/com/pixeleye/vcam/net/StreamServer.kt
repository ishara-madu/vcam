package com.pixeleye.vcam.net

import android.util.Log
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class StreamServer(
    private val port: Int = 8088,
    private val onClientConnected: (String) -> Unit = {},
    private val onClientDisconnected: () -> Unit = {}
) {
    companion object {
        private const val TAG = "VCamStreamServer"
        private const val QUEUE_CAPACITY = 3 // Minimal queue to drop late frames and eliminate lag
    }

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var clientOutputStream: OutputStream? = null

    private val isRunning = AtomicBoolean(false)
    private var acceptThread: Thread? = null
    private var sendThread: Thread? = null

    // Holds packets ready to be transmitted over socket
    private val packetQueue = ArrayBlockingQueue<ByteArray>(QUEUE_CAPACITY)

    // Cached SPS/PPS config to transmit immediately to newly connected Mac clients
    private var cachedConfigPacket: ByteArray? = null

    fun setCachedConfig(configData: ByteArray) {
        this.cachedConfigPacket = PacketFraming.buildPacket(
            type = PacketFraming.TYPE_CONFIG,
            ptsUs = 0L,
            payload = configData
        )
    }

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        acceptThread = Thread({
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress("0.0.0.0", port))
                this.serverSocket = server
                Log.i(TAG, "VCam StreamServer listening on port $port")

                while (isRunning.get()) {
                    val socket = server.accept()
                    socket.tcpNoDelay = true // Disable Nagle's algorithm for zero latency
                    socket.sendBufferSize = 64 * 1024

                    val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
                    Log.i(TAG, "Mac client connected from $clientIp")

                    // Disconnect previous client if any
                    disconnectClient()

                    this.clientSocket = socket
                    this.clientOutputStream = socket.getOutputStream()
                    packetQueue.clear()

                    // Send cached SPS/PPS config immediately upon connection
                    cachedConfigPacket?.let { cfg ->
                        try {
                            clientOutputStream?.write(cfg)
                            clientOutputStream?.flush()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send initial config to client: ${e.message}")
                        }
                    }

                    onClientConnected(clientIp)
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Server socket error: ${e.message}")
                }
            }
        }, "VCamServerAccept").apply { start() }

        sendThread = Thread({
            while (isRunning.get()) {
                try {
                    val packet = packetQueue.take()
                    val out = clientOutputStream
                    if (out != null) {
                        out.write(packet)
                        out.flush()
                    }
                } catch (_: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Socket write error: ${e.message}")
                    disconnectClient()
                }
            }
        }, "VCamServerSend").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun enqueueFrame(isKey: Boolean, ptsUs: Long, data: ByteArray) {
        val type = if (isKey) PacketFraming.TYPE_FRAME_KEY else PacketFraming.TYPE_FRAME_DELTA
        val packet = PacketFraming.buildPacket(type, ptsUs, data)

        // Drop-on-lag strategy: If queue is full, drop delta frame immediately
        if (!packetQueue.offer(packet)) {
            if (isKey) {
                // If it's an IDR keyframe, drop oldest frame and insert keyframe
                packetQueue.poll()
                packetQueue.offer(packet)
            }
            // If it's a P-frame and queue is full, drop it to prevent delay build-up
        }
    }

    private fun disconnectClient() {
        try {
            clientOutputStream?.close()
            clientSocket?.close()
        } catch (_: Exception) {}
        clientOutputStream = null
        clientSocket = null
        onClientDisconnected()
    }

    fun stop() {
        isRunning.set(false)
        disconnectClient()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        acceptThread?.interrupt()
        sendThread?.interrupt()
    }
}
