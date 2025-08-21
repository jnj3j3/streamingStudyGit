package com.example.streamingback.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import me.desair.tus.server.TusFileUploadService

@Configuration
public class TusConfig {
    @Value("\${tus.data.path}") //tusDataPath에 application.properties에 있는 tus.data.path 값이 들어감
    private lateinit var tusDataPath: String

    @Value("\${tus.data.expiration}")
    private var tusDataExpiration: Long = 0

    @Bean
    fun tusFileUploadService(): TusFileUploadService {
        return TusFileUploadService()
            .withStoragePath(tusDataPath)
            .withDownloadFeature()
            .withUploadExpirationPeriod(tusDataExpiration)
            .withThreadLocalCache(true)
            .withUploadUri("/tus/upload")
    }

}