package com.example.streamingback.config

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFprobe
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import java.io.IOException
import kotlin.jvm.Throws

class FFmpegConfig(
    @Value("\${ffmpeg.path}")
    private var ffmpegPath: String,

    @Value("\${ffprobe.path")
    private var ffprobePath: String
) {


    @Bean
    @Throws(IOException::class)
    fun ffMpeg(): FFmpeg {
        return FFmpeg(ffmpegPath)
    }

    @Bean
    @Throws(IOException::class)
    fun ffProbe(): FFprobe {
        return FFprobe(ffprobePath)
    }

}