package de.thm.mni.backend.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Repository
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.util.UUID


@Repository
class FileStorageRepository(
    @Value("\${storage.s3.endpoint}") private val endpoint: String,
    @Value("\${storage.s3.bucket}") private val bucket: String,
    @Value("\${storage.s3.region}") private val region: String,
    @Value("\${storage.s3.access-key}") private val accessKey: String,
    @Value("\${storage.s3.secret-key}") private val secretKey: String,
) {
    private val s3Client: S3Client = S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build()
        )
        .httpClient(UrlConnectionHttpClient.builder().build())
        .build()

    @Volatile
    private var bucketReady = false

    fun saveFile(fileName: String, contentType: String?, content: ByteArray): StoredAttachment {
        ensureBucket()

        val objectKey = "attachments/${UUID.randomUUID()}"

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentLength(content.size.toLong())
            .contentType(contentType ?: "application/octet-stream")
            .metadata(mapOf("file-name" to fileName))
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(content))

        return StoredAttachment(
            size = content.size.toLong(),
            fileName = fileName,
            mimeType = contentType,
            path = objectKey
        )
    }

    fun deleteFile(objectKey: String) {
        ensureBucket()

        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build()
        )
    }

    fun load(objectKey: String): StoredFile {
        ensureBucket()

        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()
        val response: ResponseBytes<GetObjectResponse> = s3Client.getObjectAsBytes(request)
        val metadata = response.response()

        return StoredFile(
            resource = ByteArrayResource(response.asByteArray()),
            contentType = metadata.contentType(),
            contentLength = metadata.contentLength()
        )
    }

    private fun ensureBucket() {
        if (bucketReady) {
            return
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
        } catch (_: NoSuchBucketException) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build())
        }
        bucketReady = true
    }

}

data class StoredFile(
    val resource: Resource,
    val contentType: String?,
    val contentLength: Long?,
)
