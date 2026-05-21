package com.untitled.service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.untitled.config.TosProperties;
import com.untitled.dto.StorageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class TosStorageService {
    private final TosProperties properties;

    public TosStorageService(TosProperties properties) {
        this.properties = properties;
    }

    public StorageResult upload(MultipartFile file) throws IOException {
        validateConfig();
        String key = buildObjectKey(file.getOriginalFilename());
        AmazonS3 client = buildClient();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        try (InputStream input = file.getInputStream()) {
            client.putObject(new PutObjectRequest(properties.getBucket(), key, input, metadata));
        }
        String url = buildPublicUrl(key);
        return new StorageResult(url, key);
    }

    private void validateConfig() {
        if (!StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getRegion())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getPublicUrl())) {
            throw new IllegalArgumentException("请配置 TOS：access-key/secret-key/endpoint/region/bucket/public-url");
        }
    }

    private AmazonS3 buildClient() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(
                properties.getAccessKey(),
                properties.getSecretKey()
        );
        ClientConfiguration config = new ClientConfiguration();
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        properties.getEndpoint(),
                        properties.getRegion()
                ))
                .withPathStyleAccessEnabled(properties.isPathStyle())
                .withClientConfiguration(config)
                .build();
    }

    private String buildObjectKey(String originalName) {
        String ext = "";
        if (StringUtils.hasText(originalName) && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        return "uploads/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private String buildPublicUrl(String key) {
        String base = properties.getPublicUrl().trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + key;
    }
}
