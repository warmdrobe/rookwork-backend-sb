package com.example.rookwork_backend_sb.services;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    /**
     * Uploads a file to AWS S3 and returns the generated stored name.
     */
    public String uploadFile(MultipartFile file, UUID projectId, UUID issueId) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Generate random unique name
        String fileUuid = UUID.randomUUID().toString();
        // Construct directory structure key: projects/{projectId}/issues/{issueId}/{uuid}{ext}
        String storedName = String.format("projects/%s/issues/%s/%s%s", 
                projectId, 
                issueId, 
                fileUuid, 
                fileExtension);

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, storedName, inputStream);
        }
        return storedName;
    }

    /**
     * Deletes a file from AWS S3.
     */
    public void deleteFile(String storedName) {
        s3Template.deleteObject(bucketName, storedName);
    }

    /**
     * Generates a temporary Presigned URL for viewing/downloading files.
     * Valid for 15 minutes.
     */
    public String generatePresignedUrl(String storedName) {
        if (storedName == null || storedName.isEmpty()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storedName)
                .build();

        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignedGetObjectRequest.url().toString();
    }

    /**
     * Uploads a user avatar to S3 under the "avatar" directory.
     */
    public String uploadAvatar(MultipartFile file, UUID userId) throws IOException {
        String fileExtension = getFileExtension(file.getOriginalFilename());

        // Key format: avatar/{userId}/{fileUuid}{ext}
        String fileUuid = UUID.randomUUID().toString();
        String storedName = String.format("avatar/%s/%s%s", userId.toString(), fileUuid, fileExtension);

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucketName, storedName, inputStream);
        }
        return storedName;
    }

    /**
     * Helper to get presigned URL for avatar if it is stored on S3.
     * If not stored on S3 (e.g. Google URL or null), returns the original value.
     */
    public String getAvatarUrl(String picture) {
        if (picture != null && picture.startsWith("avatar/")) {
            return generatePresignedUrl(picture);
        }
        return picture;
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
