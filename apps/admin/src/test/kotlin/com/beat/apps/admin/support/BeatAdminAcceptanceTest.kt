package com.beat.apps.admin.support

import com.beat.apps.admin.AdminApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(classes = [AdminApplication::class])
@ActiveProfiles("test")
@Import(BeatAdminTestContainersConfig::class)
@AutoConfigureMockMvc
annotation class BeatAdminAcceptanceTest
