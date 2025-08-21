package com.example.streamingback.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.io.FileInputStream

@RestController
@RequestMapping("/rtmp")
class RtmpController(
    @Value("\${rtmp.allowed.keys}") private val allowedKeysCsv: String,
    @Value("\${hls.root:/var/www/hls}") private val hlsRoot: String
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val allowedKeys = allowedKeysCsv.split(",").map { it.trim() }.toSet()

    /**
     * Nginx-RTMP on_publish → 인증
     * - name = stream key
     * - app  = "live"
     */
    @PostMapping("/auth")
    fun onPublishAuth(
        @RequestParam(required = false) app: String?,
        @RequestParam(required = false, name = "name") streamKey: String?,
        @RequestParam(required = false) tcurl: String?
    ): ResponseEntity<String> {
        log.info("on_publish: app={}, key={}, tcurl={}", app, streamKey, tcurl)
        if (streamKey != null && streamKey in allowedKeys) {
            log.info("on_publish: streamKey={}", streamKey)
            return ResponseEntity.ok("OK")
        }
        log.info("not_on_publish: streamKey={}", streamKey)
        return ResponseEntity.status(403).body("Invalid stream key")
    }

    /**
     * Nginx-RTMP on_publish_done → 방송 종료 훅(선택)
     * 여기서 로그 남기거나 정리 작업 수행 가능
     */
    @PostMapping("/done")
    fun onPublishDone(
        @RequestParam(required = false) app: String?,
        @RequestParam(required = false, name = "name") streamKey: String?
    ): ResponseEntity<String> {
        log.info("done_publish_done: app={}, key={}", app, streamKey)
        return ResponseEntity.ok("DONE")
    }

    /**
     * (옵션) HLS 프록시 서빙: Nginx 8081이 이미 서빙 중이면 안 써도 됩니다.
     * GET /rtmp/hls/{key}/master.m3u8 or /playlist.m3u8 or /segment_xxx.ts
     */
    @GetMapping("/hls/{key}/{fileName}")
    fun hlsProxy(
        @PathVariable key: String,
        @PathVariable fileName: String
    ): ResponseEntity<InputStreamResource> {
        val file = File("$hlsRoot/$key/$fileName")
        if (!file.exists()) return ResponseEntity.notFound().build()

        val mediaType = if (fileName.endsWith(".m3u8", true))
            MediaType.parseMediaType("application/x-mpegURL")
        else
            MediaType.parseMediaType("video/mp2t")

        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(InputStreamResource(FileInputStream(file)))
    }
}
