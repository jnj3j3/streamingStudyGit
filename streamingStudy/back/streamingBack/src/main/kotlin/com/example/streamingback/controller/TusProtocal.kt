package com.example.streamingback.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.desair.tus.server.TusFileUploadService
import me.desair.tus.server.upload.UploadInfo
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate

@RestController
class TusUploadController(
    private val tusFileUploadService: TusFileUploadService
) {
    private val log = KotlinLogging.logger {}

    @RequestMapping(
        path = ["/tus/upload", "/tus/upload/**"],
        method = [RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS]
    )
    fun handleTusUpload(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Void> {
        val uri = request.requestURI.removePrefix(request.contextPath)

        // Tus 프로토콜 처리
        tusFileUploadService.process(request, response)

        if (response.isCommitted) {
            return ResponseEntity.status(response.status).build()
        }

        // 업로드 완료 시 처리
        try {
            val uploadInfo: UploadInfo? = tusFileUploadService.getUploadInfo(uri)

            if (uploadInfo != null && !uploadInfo.isUploadInProgress) {
                val fileId = uri.substringAfterLast("/")
                val inputStream = tusFileUploadService.getUploadedBytes(fileId)
                val fileName = uploadInfo.fileName ?: "uploaded_file"
                val today = LocalDate.now().toString()
                val outputDir = Paths.get("/home/ju/uploads/videos", today)
                Files.createDirectories(outputDir)

                val outputFile = outputDir.resolve(fileName)
                Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING)
                log.info { "Upload completed: $fileName" }
            }
        } catch (e: Exception) {
            log.error(e) { "Upload processing error" }
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/tus/upload/{uploadId}")
    fun getUploadOffset(
        @PathVariable uploadId: String,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        return try {
            val uploadInfo: UploadInfo? = tusFileUploadService.getUploadInfo("/tus/upload/$uploadId")

            if (uploadInfo != null) {
                response.setHeader("Upload-Offset", uploadInfo.offset.toString())
                response.setHeader("Tus-Resumable", "1.0.0")
                ResponseEntity.noContent().build()
            } else {
                log.warn { "No uploadInfo found for ID: $uploadId" }
                ResponseEntity.notFound().build()
            }
        } catch (e: Exception) {
            log.error(e) { "Error retrieving upload offset" }
            ResponseEntity.internalServerError().build()
        }
    }
}
