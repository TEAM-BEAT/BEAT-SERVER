package com.beat.infrastructure.external.storage.s3

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration(proxyBeanMethods = false)
internal class S3InfraConfig {
    @field:Value("\${cloud.aws.credentials.access-key}") private lateinit var accessKey: String

    @field:Value("\${cloud.aws.credentials.secret-key}") private lateinit var secretKey: String

    @field:Value("\${cloud.aws.region}") private lateinit var region: String

    @Bean
    @Primary
    fun awsCredentialsProvider(): BasicAWSCredentials = BasicAWSCredentials(accessKey, secretKey)

    @Bean
    fun amazonS3(awsCredentials: BasicAWSCredentials): AmazonS3 =
        AmazonS3ClientBuilder.standard()
            .withRegion(region)
            .withCredentials(AWSStaticCredentialsProvider(awsCredentials))
            .build()
}
