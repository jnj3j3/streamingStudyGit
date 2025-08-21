package com.example.streamingback

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StreamingBackApplication

fun main(args: Array<String>) {
    runApplication<StreamingBackApplication>(*args)
}
