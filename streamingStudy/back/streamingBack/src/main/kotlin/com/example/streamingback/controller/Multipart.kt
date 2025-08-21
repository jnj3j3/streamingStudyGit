package com.example.streamingback.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths

data class PostData(
    val title: String,
    val content: String
)

@RestController
@RequestMapping("/streaming")
class StoryController(
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadVideoStory(
        @RequestPart("postData") postDataJson: String,
        @RequestPart("videoFile") videoFile: MultipartFile
    ): ResponseEntity<String> {
        val postData = objectMapper.readValue(postDataJson, PostData::class.java)

        // MIME 타입 검사
        if (!videoFile.contentType.orEmpty().startsWith("video/")) {
            return ResponseEntity.badRequest().body("비디오 파일만 업로드 가능합니다.")
        }

        println("제목: ${postData.title}, 내용: ${postData.content}")
        println("업로드한 영상: ${videoFile.originalFilename}, 타입: ${videoFile.contentType}")

        val uploadDir = Paths.get("/home/ju/uploads/videos")
        Files.createDirectories(uploadDir)
        val targetPath = uploadDir.resolve(videoFile.originalFilename!!)
        videoFile.transferTo(targetPath)

        return ResponseEntity.ok("영상 업로드 성공")
    }
}
