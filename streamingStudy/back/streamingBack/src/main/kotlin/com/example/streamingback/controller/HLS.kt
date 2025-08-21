package com.example.streamingback.controller

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

@RestController
class HlsController(
    @Value("\${tus.output.path.hls}") private val outputPath: String
) {

    private val log = LoggerFactory.getLogger(HlsController::class.java)

    @GetMapping("/hls/{key}/{fileName}")
    fun getHlsMaster(
        @PathVariable key: String,
        @PathVariable fileName: String
    ): ResponseEntity<InputStreamResource> {
        return buildHlsResponse(File("$outputPath/$key/$fileName"), fileName)
    }

    @GetMapping("/hls/{key}/{resolution}/{fileName}")
    fun getHlsSegment(
        @PathVariable key: String,
        @PathVariable resolution: String,
        @PathVariable fileName: String
    ): ResponseEntity<InputStreamResource> {
        return buildHlsResponse(File("$outputPath/$key/$resolution/$fileName"), fileName)
    }

    @Throws(FileNotFoundException::class)
    private fun buildHlsResponse(file: File, fileName: String): ResponseEntity<InputStreamResource> {
        if (!file.exists()) {
            log.warn("HLS file not found: ${file.absolutePath}")
            throw FileNotFoundException(file.absolutePath)
        }

        val mediaType = when {
            fileName.endsWith(".m3u8", ignoreCase = true) -> MediaType.parseMediaType("application/x-mpegURL")
            else -> MediaType.parseMediaType("video/mp2t")
        }

        val resource = InputStreamResource(FileInputStream(file))
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(resource)
    }
}
