package com.beat.apps.api.support

import com.beat.apps.api.ApisApplication
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(classes = [ApisApplication::class])
@ActiveProfiles("test")
@Import(BeatTestContainersConfig::class)
@AutoConfigureMockMvc
annotation class BeatAcceptanceTest
