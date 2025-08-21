package com.example.streamingback.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/rtmp2")
class Rtmp2Controller(
    @Value("\${rtmp.allowed.keys}") private val allowedKeysCsv: String,
    @Value("\${hls.root:/var/www/hls}") private val hlsRoot: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val allowedKeys = allowedKeysCsv.split(",").map { it.trim() }.toSet()

    //실행 중인 FFmpeg 프로세스를 관리하기 위한 맵
    // Key: 스트림 키, Value: 실행된 Process 객체
    private val runningProcesses: MutableMap<String, Process> = ConcurrentHashMap()

    @PostMapping("/auth")
    fun onPublishAuth(
        @RequestParam(name = "name") streamKey: String?,
    ): ResponseEntity<String> {
        if (streamKey == null || streamKey !in allowedKeys) {
            return ResponseEntity.status(403).body("Invalid stream key")
        }

        runningProcesses[streamKey]?.destroyForcibly()

        try {
            // @Value 변수인 hlsRoot를 사용하여 경로를 동적으로 설정합니다.
            val hlsPath = File("$hlsRoot/$streamKey")

            if (!hlsPath.exists()) {
                val created = hlsPath.mkdirs()
                if (!created) {
                    logger.error("Failed to create directory: ${hlsPath.absolutePath}")
                    return ResponseEntity.status(500).body("Server error: cannot create directory")
                }
            }

            // 로그 파일 디렉터리 경로도 hlsRoot 변수를 사용합니다.
            val logDir = File("$hlsRoot/$streamKey/logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val commandList = listOf(
                "/usr/bin/ffmpeg", //FFmpeg 실행 파일 경로
                "-i", "rtmp://127.0.0.1:1935/live/$streamKey",
                "-c:v", "libx264", "-preset", "ultrafast", "-s", "854x480",
                "-c:a", "aac", "-b:a", "128k",
                "-f", "hls", "-hls_list_size", "3", "-hls_time", "3",
                "-hls_segment_filename", "${hlsPath.absolutePath}/segment_%03d.ts",
                "${hlsPath.absolutePath}/playlist.m3u8"
            )

            val commandString = commandList.joinToString(" ")
            val processBuilder = ProcessBuilder("/bin/sh", "-c", commandString)

            processBuilder.redirectErrorStream(true)
            processBuilder.redirectOutput(File(logDir, "${streamKey}.log"))

            val process = processBuilder.start()
            runningProcesses[streamKey] = process

            logger.info("FFmpeg process started for stream KEY $streamKey : PID : ${process.pid()} with command: $commandString")
            return ResponseEntity.ok("Successfully processed")

        } catch (e: Exception) {
            logger.error("Failed to start FFmpeg for stream key $streamKey", e)
            return ResponseEntity.status(500).body("Failed to start FFmpeg for stream KEY $streamKey")
        }
    }

    @PostMapping("/done")
    fun onPublishDone(
        @RequestParam(name = "name") streamKey: String?,
    ): ResponseEntity<String> {
        logger.info("on_publish : key = $streamKey")
        if (streamKey != null) {
            val process = runningProcesses.remove(streamKey)
            if (process != null) {
                process.destroyForcibly()
                logger.info("FFmpeg process for stream key $streamKey terminated")
            } else logger.info("No such FFmpeg process for stream key $streamKey")
        }
        return ResponseEntity.ok("Done")
    }

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