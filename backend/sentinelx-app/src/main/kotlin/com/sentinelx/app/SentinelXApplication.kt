package com.sentinelx.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.sentinelx"])
@EnableAsync
@EnableScheduling
class SentinelXApplication

fun main(args: Array<String>) {
    runApplication<SentinelXApplication>(*args)
}