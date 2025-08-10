package com.code.minio.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {

    @Autowired
    private MinioProperties minioProperties;

    @Bean
    public AmazonS3 amazonS3Client() {
        // 创建AWS凭证
        AWSCredentials credentials = new BasicAWSCredentials(
                minioProperties.getUsername(),
                minioProperties.getPassword()
        );

        // 客户端配置
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        // 构建S3客户端，配置为使用MinIO
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        minioProperties.getEndpoint(), "us-east-1"))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true) // MinIO使用路径样式访问
                .withClientConfiguration(clientConfiguration)
                .build();
    }
}