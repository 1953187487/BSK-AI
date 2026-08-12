package com.floatai.capture

import java.nio.ByteBuffer

/**
 * 极简 IP/TCP 解析器 v1.0.5（无原生依赖，纯 Java）：
 *  - 解析 IPv4 header
 *  - 解析 TCP header
 *  - 重组 TCP 流（按 srcIp:srcPort-dstIp:dstPort 跟踪 seq/ack）
 *  - 检测 HTTP 请求/响应头（明文，未加密）
 *
 * 局限：
 *  - 仅处理 IPv4 + TCP（HTTPS 因为 TLS 加密看不到内容）
 *  - 不重组 TCP 重传 / 乱序
 */
object IpTcpParser {

    /** 解析后的 IP 包。 */
    data class IpPacket(
        val srcIp: String,
        val dstIp: String,
        val protocol: Int,
        val payload: ByteArray,
        val totalLen: Int
    )

    /** 解析后的 TCP 段。 */
    data class TcpSegment(
        val srcPort: Int,
        val dstPort: Int,
        val seq: Long,
        val ack: Long,
        val flags: Int,
        val payload: ByteArray
    )

    /** TCP 流重组器。 */
    class TcpStream {
        var buffered: ByteArray = ByteArray(0)
        var expectedSeq: Long = -1

        fun feed(seg: TcpSegment): ByteArray? {
            // 无 payload 直接返回
            if (seg.payload.isEmpty()) return null
            if (expectedSeq < 0) {
                expectedSeq = seg.seq + 1  // SYN 占用 1
                buffered = seg.payload
                return buffered
            }
            // 简化：把 payload 拼接到 buffered 末尾
            // 真实实现需要按 seq 排序、丢重传；此处仅追加
            buffered += seg.payload
            return buffered
        }
    }

    /** 解析 IPv4 包。返回 null 表示解析失败或非 IPv4。 */
    fun parseIp(data: ByteArray, offset: Int = 0, length: Int = data.size): IpPacket? {
        if (length < 20) return null
        val versionAndIhl = data[offset].toInt() and 0xFF
        val version = (versionAndIhl shr 4) and 0xF
        if (version != 4) return null
        val ihl = (versionAndIhl and 0xF) * 4
        if (ihl < 20 || length < ihl) return null
        val totalLen = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
        val protocol = data[offset + 9].toInt() and 0xFF
        val srcIp = "${data[offset + 12].toInt() and 0xFF}.${data[offset + 13].toInt() and 0xFF}.${data[offset + 14].toInt() and 0xFF}.${data[offset + 15].toInt() and 0xFF}"
        val dstIp = "${data[offset + 16].toInt() and 0xFF}.${data[offset + 17].toInt() and 0xFF}.${data[offset + 18].toInt() and 0xFF}.${data[offset + 19].toInt() and 0xFF}"
        val payload = data.copyOfRange(offset + ihl, offset + length)
        return IpPacket(srcIp, dstIp, protocol, payload, totalLen)
    }

    /** 解析 TCP 段。返回 null 表示解析失败或非 TCP。 */
    fun parseTcp(ip: IpPacket): TcpSegment? {
        if (ip.protocol != 6) return null  // TCP = 6
        val p = ip.payload
        if (p.size < 20) return null
        val srcPort = ((p[0].toInt() and 0xFF) shl 8) or (p[1].toInt() and 0xFF)
        val dstPort = ((p[2].toInt() and 0xFF) shl 8) or (p[3].toInt() and 0xFF)
        val seq = ((p[4].toLong() and 0xFF) shl 24) or
                  ((p[5].toLong() and 0xFF) shl 16) or
                  ((p[6].toLong() and 0xFF) shl 8) or
                  (p[7].toLong() and 0xFF)
        val ack = ((p[8].toLong() and 0xFF) shl 24) or
                  ((p[9].toLong() and 0xFF) shl 16) or
                  ((p[10].toLong() and 0xFF) shl 8) or
                  (p[11].toLong() and 0xFF)
        val dataOffset = ((p[12].toInt() shr 4) and 0xF) * 4
        if (dataOffset < 20 || p.size < dataOffset) return null
        val flags = p[13].toInt() and 0xFF
        val payload = if (p.size > dataOffset) p.copyOfRange(dataOffset, p.size) else ByteArray(0)
        return TcpSegment(srcPort, dstPort, seq, ack, flags, payload)
    }

    // TCP flags
    const val FIN = 0x01
    const val SYN = 0x02
    const val RST = 0x04
    const val PSH = 0x08
    const val ACK = 0x10
    const val URG = 0x20

    /** 简单 HTTP 头解析：从累计 buffer 中取首个完整 HTTP 头。 */
    fun extractHttpMessage(buffered: ByteArray): Pair<HttpMessage, Int>? {
        val delim = "\r\n\r\n".toByteArray(Charsets.UTF_8)
        val headerEnd = indexOf(buffered, delim)
        if (headerEnd < 0) return null
        val headerBytes = buffered.copyOfRange(0, headerEnd)
        val header = String(headerBytes, Charsets.UTF_8)
        val lines = header.split("\r\n")
        if (lines.isEmpty()) return null
        val startLine = lines[0]
        val method: String?
        val url: String?
        val statusCode: Int?
        val statusText: String?
        if (startLine.startsWith("HTTP/")) {
            method = null
            url = null
            val parts = startLine.split(" ", limit = 3)
            statusCode = parts.getOrNull(1)?.toIntOrNull()
            statusText = parts.getOrNull(2)
        } else {
            val parts = startLine.split(" ", limit = 3)
            method = parts.getOrNull(0)
            url = parts.getOrNull(1)
            statusCode = null
            statusText = null
        }
        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val colon = lines[i].indexOf(':')
            if (colon > 0) {
                val k = lines[i].substring(0, colon).trim()
                val v = lines[i].substring(colon + 1).trim()
                headers[k] = v
            }
        }
        val bodyStart = headerEnd + delim.size
        return HttpMessage(
            method = method,
            url = url,
            statusCode = statusCode,
            statusText = statusText,
            headers = headers,
            body = String(buffered.copyOfRange(bodyStart, buffered.size), Charsets.UTF_8)
        ) to bodyStart
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty()) return 0
        outer@ for (i in 0..haystack.size - needle.size) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    data class HttpMessage(
        val method: String?,
        val url: String?,
        val statusCode: Int?,
        val statusText: String?,
        val headers: Map<String, String>,
        val body: String
    )
}
