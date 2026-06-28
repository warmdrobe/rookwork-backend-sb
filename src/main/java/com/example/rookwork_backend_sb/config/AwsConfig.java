package com.example.rookwork_backend_sb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
public class AwsConfig {

    @Bean
    public SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
        return SesClient.builder()
            .region(Region.AP_SOUTHEAST_1)
            .credentialsProvider(credentialsProvider)
            .build();
    }
}