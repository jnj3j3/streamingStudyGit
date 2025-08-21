package com.example.streamingback.controller

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.Progress
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@RestController
@RequestMapping("/convert")
class FFmpegController(
    @Value("\${ffmpeg.path}") private val ffmpegPath: String,
    @Value("\${ffprobe.path}") private val ffprobePath: String,
    @Value("\${tus.save.path}") private val savedPath: String,
    @Value("\${tus.output.path.hls}") private val hlsOutputPath: String,
    @Value("\${tus.output.path.mp4}") private val mp4OutputPath: String

) {
    private val log = LoggerFactory.getLogger(FFmpegController::class.java)
    private val ffmpeg: FFmpeg by lazy { FFmpeg(ffmpegPath) }
    private val fFprobe: FFprobe by lazy { FFprobe(ffprobePath) }

    @PostMapping("/hls")
    fun convertToHls(
        @RequestParam("date") date: String,
        @RequestParam("fileName") fileName: String,
    ): String {
        val inputPath: Path = Paths.get("$savedPath/$date/$fileName")
        val outputPath: Path = Paths.get("$hlsOutputPath/${fileName.substringBeforeLast(".")}")
        val prefix: File = outputPath.toFile()
        val _1080: File = File(prefix, "1080")
        val _720: File = File(prefix, "720")
        val _480: File = File(prefix, "480")
        if (!_1080.exists()) _1080.mkdirs()
        if (!_720.exists()) _720.mkdirs()
        if (!_480.exists()) _480.mkdirs()

        val builder = FFmpegBuilder()
            .setInput(inputPath.toAbsolutePath().toString()) // 변환할 입력 영상 경로
            .overrideOutputFiles(true) // 출력 파일이 이미 존재하면 덮어쓰기
            .addOutput(outputPath.toAbsolutePath().toString() + "/%v/playlist.m3u8")
            .setFormat("hls")// 출력 포맷: HLS
            .addExtraArgs("-hls_time", "10") // chunk 시간
            .addExtraArgs("-hls_list_size", "0")// playlist 크기 제한 없음 (전체 세그먼트 리스트 유지)
            .addExtraArgs(
                "-hls_segment_filename",
                outputPath.toAbsolutePath().toString() + "/%v/output_%03d.ts"
            ) // ts 파일 이름 (ex: output_000.ts)
            .addExtraArgs("-master_pl_name", "master.m3u8") // 마스터 재생 파일
            //비디오 +오디오 매핑
            //3번 → 입력 영상(0번 입력의 비디오 트랙)을 세 번 복사해서 각각 다른 해상도로 인코딩.
            .addExtraArgs("-map", "0:v:0").addExtraArgs("-map", "0:a:0")  // 1080p용 비디오와 오디오
            .addExtraArgs("-map", "0:v:0").addExtraArgs("-map", "0:a:0")  // 720p용 비디오와 오디오
            .addExtraArgs("-map", "0:v:0").addExtraArgs("-map", "0:a:0")  // 480p용 비디오와 오디오


            .addExtraArgs("-var_stream_map", "v:0,a:0,name:1080 v:1,a:1,name:720 v:2,a:2,name:480") // 출력 매핑

            // 1080 화질 옵션
            .addExtraArgs("-b:v:0", "5000k")     // 비디오 비트레이트
            .addExtraArgs("-maxrate:v:0", "5000k") // 최대 비트레이트
            .addExtraArgs("-bufsize:v:0", "10000k") // 버퍼 크기
            .addExtraArgs("-s:v:0", "1920x1080")  // 해상도
            .addExtraArgs("-crf:v:0", "15")       // 품질 지표 (낮을수록 화질 높음)
            .addExtraArgs("-b:a:0", "128k")       // 오디오 비트레이트


            // 720 화질 옵션
            .addExtraArgs("-b:v:1", "2500k")
            .addExtraArgs("-maxrate:v:1", "2500k")
            .addExtraArgs("-bufsize:v:1", "5000k")
            .addExtraArgs("-s:v:1", "1280x720")
            .addExtraArgs("-crf:v:1", "22")
            .addExtraArgs("-b:a:1", "96k")

            // 480 화질 옵션
            .addExtraArgs("-b:v:2", "1000k")
            .addExtraArgs("-maxrate:v:2", "1000k")
            .addExtraArgs("-bufsize:v:2", "2000k")
            .addExtraArgs("-s:v:2", "854x480")
            .addExtraArgs("-crf:v:2", "28")
            .addExtraArgs("-b:a:2", "64k")
            .done();
        runFFmpeg(builder)
        return "HLS converted successfully : ${outputPath}"
    }

    @PostMapping("/mp4")
    fun convertToMp4(
        @RequestParam date: String,
        @RequestParam filename: String
    ): String {
        val path = "$savedPath/$date/$filename"
        val outputFile = File("$mp4OutputPath/${filename.substringBeforeLast(".")}.mp4")

        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        val builder = FFmpegBuilder()
            .setInput(path)
            .overrideOutputFiles(true)
            .addOutput(outputFile.absolutePath)
            .setFormat("mp4")
            .setVideoCodec("libx264")
            .setAudioCodec("aac")
            .done()

        runFFmpeg(builder)
        return "MP4 converted successfully : ${outputFile.absolutePath}"
    }

    private fun runFFmpeg(builder: FFmpegBuilder) {
        val executor = FFmpegExecutor(ffmpeg, fFprobe)
        executor.createJob(builder) { progress ->
            log.info("progress=>{}", progress)
            if (progress.status == Progress.Status.END) {
                log.info("Job Finished")
            }
        }.run()
    }
}