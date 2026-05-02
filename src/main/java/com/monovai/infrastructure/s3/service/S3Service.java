package com.monovai.infrastructure.s3.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Template s3Template;
    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 바이트 배열을 S3 에 업로드. 반환은 객체의 S3 key (URL 아님).
     * URL 이 필요하면 호출자가 별도로 getPreSignedUrlForDownload 호출.
     */
    public String uploadBytes(String key, byte[] data, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(data));
        } catch (Exception e) {
            throw new RuntimeException("S3 업로드 실패: " + key, e);
        }
        return key;
    }

    /**
     * 임의 TTL 의 조회용 presigned URL.
     */
    public String getPreSignedUrlForDownload(String key, Duration ttl) {
        URL preSignedUrl = s3Template.createSignedGetURL(bucketName, key, ttl);
        return preSignedUrl.toString();
    }

    /**
     * S3에서 파일을 업로드하기 위한 Presigned URL 생성
     * @param key 파일 경로 및 이름 (예: "images/profile.png")
     * @return 생성된 URL
     */
    public String getPreSignedUrlForUpload(String key) {
        // 10분 동안 유효한 업로드용 URL 생성
        URL preSignedUrl = s3Template.createSignedPutURL(bucketName, key, Duration.ofMinutes(10));
        return preSignedUrl.toString();
    }

//    public String getPublicKey(String key){
//        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucketName, key);
//    }

    public String getPublicKey(String key){
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build())
                .toString();
    }

    /**
     * S3에 있는 파일을 조회하기 위한 Presigned URL 생성
     */
    public String getPreSignedUrlForDownload(String key) {
        // 10분 동안 유효한 조회용 URL 생성
        URL preSignedUrl = s3Template.createSignedGetURL(bucketName, key, Duration.ofMinutes(10));
        return preSignedUrl.toString();
    }
}
