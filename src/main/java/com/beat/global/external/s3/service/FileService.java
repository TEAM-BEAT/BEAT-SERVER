package com.beat.global.external.s3.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.beat.domain.performance.domain.Performance;
import com.beat.global.external.s3.dao.FileRepository;
import com.beat.global.external.s3.domain.File;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${cloud.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;
    private final FileRepository fileRepository;

    public Map<String, String> getPresignedUrl(String prefix, String fileName) {
        String filePath = fileName;
        if (!prefix.isEmpty()) {
            filePath = createPath(prefix, fileName);
        }

        GeneratePresignedUrlRequest generatePresignedUrlRequest = getGeneratePresignedUrlRequest(bucket, filePath);
        URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);

        // 파일 정보를 데이터베이스에 저장
        saveFileToDB(fileName, filePath);

        return Map.of("url", url.toString());
    }

    private GeneratePresignedUrlRequest getGeneratePresignedUrlRequest(String bucket, String fileName) {
        return new GeneratePresignedUrlRequest(bucket, fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(getPresignedUrlExpiration());
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 2;
        expiration.setTime(expTimeMillis);

        return expiration;
    }

    private String createFileId() {
        return UUID.randomUUID().toString();
    }

    private String createPath(String prefix, String fileName) {
        String fileId = createFileId();
        return String.format("%s/%s", prefix, fileId + "-" + fileName);
    }

    private void saveFileToDB(String fileName, String filePath) {
//        Performance performance = new Performance();
//        performance.setPosterImage(filePath);
        File file = new File();
        file.setFileName(fileName);
        file.setFilePath(filePath);
        file.setUploadTime(new Date());

        fileRepository.save(file);
    }
}