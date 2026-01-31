package com.amphi.server.configs

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy
import org.slf4j.LoggerFactory

object LogConfig {
    fun setup() {

        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        context.reset()

        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
            start()
        }

        val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
            this.context = context
            this.encoder = encoder
            start()
        }

        val fileAppender = RollingFileAppender<ILoggingEvent>().apply appender@{
            this.context = context
            file = "${AppConfig.storage.logs}/server.log"

            val rollingPolicy = TimeBasedRollingPolicy<ILoggingEvent>().apply {
                this.context = context
                this.setParent(this@appender)
                fileNamePattern = "${AppConfig.storage.logs}/server.%d{yyyy-MM-dd}.log.gz"
                maxHistory = 30
                start()
            }

            this.rollingPolicy = rollingPolicy
            this.encoder = encoder
            start()
        }

        context.getLogger("ROOT").apply {
            level = Level.INFO
            addAppender(consoleAppender)
            addAppender(fileAppender)
        }
    }
}