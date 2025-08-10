package com.code.minio.model;

import io.minio.MinioClient;
import org.springframework.stereotype.Component;

@Component
public class CustomMinioClient extends MinioClient {

    public CustomMinioClient(MinioClient client) {
        super(client);
    }

//    public CreateMultipartUploadResponse initMultiPart(String bucket, String region, String object, Multimap<String, String> headers, Multimap<String, String> extraQueryParams) throws Exception {
//        return this.in(bucket, region, object, headers, extraQueryParams);
//    }
//
//    public ObjectWriteResponse mergeMultipartUpload(String bucketName, String region, String objectName, String uploadId, Part[] parts, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception {
//        return this.mergeMultipartUpload(bucketName, region, objectName, uploadId, parts, extraHeaders, extraQueryParams);
//    }
//
//    public ListPartsResponse listMultipart(String bucketName, String region, String objectName, Integer maxParts, Integer partNumberMarker, String uploadId, Multimap<String, String> extraHeaders, Multimap<String, String> extraQueryParams) throws Exception{
//        return this.listMultipart(bucketName, region, objectName, maxParts, partNumberMarker, uploadId, extraHeaders, extraQueryParams);
//    }

}
