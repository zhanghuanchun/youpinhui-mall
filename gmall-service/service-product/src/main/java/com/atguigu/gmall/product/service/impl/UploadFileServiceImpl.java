package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.UploadFileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * @Author zhc
 * @Create 2024/7/23 15:51
 */
@Service
@RefreshScope
public class UploadFileServiceImpl implements UploadFileService {

    /**
     * minio:
     *     endpointUrl: http://192.168.129.134:9000
     *     accessKey: admin
     *     secreKey: admin123456
     *     bucketName: gmall
     */
    @Value("${minio.endpointUrl}")
    private String endpointUrl;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secreKey}")
    private String secreKey;

    @Value("${minio.bucketName}")
    private String bucketName;
    @Override
    public String fileUpload(MultipartFile file) {
        //  创建url 变量
        String url = "";
        try {
            // 创建minioClient 客户端
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();

            // 判断桶是否存在。
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // 如果不存在，则创建
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                //  这个桶已经存在.
                System.out.println("Bucket "+ bucketName + " already exists.");
            }

            //  生成文件名。
            String fileName =  UUID.randomUUID().toString().replaceAll("-","")+"."+ FilenameUtils.getExtension(file.getOriginalFilename());
            //  调用上传方法.
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //  endpointUrl = http://192.168.129.134:9000/gmalls/47ef53d6dc6e4e8b.jpg
            //  拼接url
            url=endpointUrl+"/"+bucketName+"/"+fileName;
            System.out.println(url);
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
        return url;
    }
}
