package com.code.minio;

import io.minio.*;
import io.minio.messages.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@SpringBootTest
class MinioApplicationTests {


    @Autowired
    private MinioClient minioClient;

    @Test
    void contextLoads() {
        System.out.println(minioClient);
    }


    @Test
    void test1() throws  Exception{
        System.out.println(minioClient.bucketExists(BucketExistsArgs.builder().bucket("myfile").build()));
    }


    @Test
    void test2() throws  Exception{
        minioClient.makeBucket(MakeBucketArgs.builder().bucket("myfile").build());
    }

    @Test
    void test3() throws  Exception{
        List<Bucket> buckets = minioClient.listBuckets();
        buckets.forEach(b -> System.out.println(b.name()));
        System.out.println("--------------");
        List<Bucket> bucketList = minioClient.listBuckets(ListBucketsArgs.builder().extraHeaders(Map.of("file", "mock.txt")).build());
        bucketList.forEach(b -> System.out.println(b.name()));
    }


    @Test
    void test4() throws  Exception{
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket("myfile").build());
    }


    @Test
    void test5() throws  Exception{
        InputStream inputStream = new FileInputStream("src/main/resources/mock.txt");
        minioClient.putObject(PutObjectArgs.builder()
                .bucket("dev").object("mock.txt").stream(inputStream, -1, 10485760).build());
    }


    @Test
    void test6() throws  Exception{
        InputStream inputStream = new FileInputStream("src/main/resources/mock.txt");
        minioClient.uploadObject(UploadObjectArgs.builder().bucket("dev").filename("src/main/resources/mock.txt").build());
    }


    @Test
    void test7() throws  Exception{
        String dev = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket("dev").object("file.txt").build());
        System.out.println(dev);
    }
}
