package com.beat.global.external.s3.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${cloud.s3.bucket}")
    private String bucket;

    private final AmazonS3 amazonS3;

    public Map<String, Map<String, String>> getPresignedUrls(String posterImage, List<String> castImages, List<String> staffImages) {
        Map<String, Map<String, String>> presignedUrls = new HashMap<>();

        // Poster Image URL
        Map<String, String> posterUrl = new HashMap<>();
        String posterFilePath = createPath("poster", posterImage);
        URL posterPresignedUrl = amazonS3.generatePresignedUrl(getGeneratePresignedUrlRequest(bucket, posterFilePath));
        posterUrl.put(posterImage, posterPresignedUrl.toString());
        presignedUrls.put("poster", posterUrl);

        // Cast Images URLs
        Map<String, String> castUrls = new HashMap<>();
        for (String castImage : castImages) {
            String castFilePath = createPath("cast", castImage);
            URL castPresignedUrl = amazonS3.generatePresignedUrl(getGeneratePresignedUrlRequest(bucket, castFilePath));
            castUrls.put(castImage, castPresignedUrl.toString());
        }
        presignedUrls.put("cast", castUrls);

        // Staff Images URLs
        Map<String, String> staffUrls = new HashMap<>();
        for (String staffImage : staffImages) {
            String staffFilePath = createPath("staff", staffImage);
            URL staffPresignedUrl = amazonS3.generatePresignedUrl(getGeneratePresignedUrlRequest(bucket, staffFilePath));
            staffUrls.put(staffImage, staffPresignedUrl.toString());
        }
        presignedUrls.put("staff", staffUrls);

        return presignedUrls;
    }

    private GeneratePresignedUrlRequest getGeneratePresignedUrlRequest(String bucket, String fileName) {
        return new GeneratePresignedUrlRequest(bucket, fileName)
                .withMethod(HttpMethod.PUT)
                .withExpiration(getPresignedUrlExpiration());
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * 2;
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
}