# vcam Streaming Protocol Specification

## Overview

The **vcam** streaming protocol is an ultra-lightweight, zero-overhead binary framing protocol designed for sub-30ms glass-to-glass video streaming from Android to macOS over USB (ADB socket forward) or local Wi-Fi (TCP/UDP).

---

## Packet Structure

Each packet transmitted over the socket consists of a 17-byte fixed binary header followed by a variable-length payload:

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                      Magic (0x5643414D)                       |  4 bytes ("VCAM")
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Packet Type  |                                               |  1 byte Type +
+-+-+-+-+-+-+-+-+                                               +
|                                                               |
+                 Presentation Timestamp (PTS)                  +  8 bytes (Int64, microseconds)
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         Payload Size                          |  4 bytes (UInt32, Big-Endian)
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                                                               |
|                        Payload Data                           |  N bytes
|                                                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

---

## Field Descriptions

| Field | Size | Data Type | Description |
| :--- | :--- | :--- | :--- |
| `Magic` | 4 bytes | `0x56 0x43 0x41 0x4D` | ASCII `"VCAM"`. Used to detect stream sync and alignment. |
| `PacketType` | 1 byte | `UInt8` | Packet type enum (see below). |
| `PTS` | 8 bytes | `Int64` (Big-Endian) | Microseconds timestamp when the frame was captured by the camera sensor. Used for latency calculation on Mac. |
| `PayloadSize` | 4 bytes | `UInt32` (Big-Endian) | Byte length of the following payload data. |
| `Payload` | Variable | Raw Bytes | Video NAL units, configuration parameters, or control commands. |

---

## Packet Types

| Type Code | Name | Description |
| :--- | :--- | :--- |
| `0x01` | `PACKET_CONFIG` | SPS (Sequence Parameter Set) and PPS (Picture Parameter Set) metadata required by VideoToolbox decoder. |
| `0x02` | `PACKET_FRAME_KEY` | H.264 IDR / I-Frame (Instantaneous Decoder Refresh). |
| `0x03` | `PACKET_FRAME_DELTA`| H.264 P-Frame (Predicted frame). |
| `0x04` | `PACKET_PING_PONG` | Heartbeat and round-trip time (RTT) ping/pong message. |
| `0x05` | `PACKET_CONTROL` | Client-to-Server control message (e.g. switch lens, adjust bitrate, request keyframe). |

---

## Low-Latency Socket Tuning

1. **TCP No Delay**: `socket.tcpNoDelay = true` (disables Nagle's algorithm) on both Android and Mac.
2. **Buffer Limits**: Set socket send buffer to small bounded sizes (e.g., 64KB - 128KB) to prevent TCP buffer bloat.
3. **Drop On Lag**: If the socket cannot send within 1 frame interval (~16.6ms at 60fps), drop delta frames and wait for next keyframe rather than queueing.
