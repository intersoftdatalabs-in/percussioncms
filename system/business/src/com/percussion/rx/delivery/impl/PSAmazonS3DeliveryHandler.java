/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rx.delivery.impl;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.legacy.security.deprecated.PSAesCBC;
import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.percussion.rx.delivery.IPSDeliveryResult;
import com.percussion.rx.delivery.IPSDeliveryResult.Outcome;
import com.percussion.rx.delivery.PSDeliveryException;
import com.percussion.rx.delivery.data.PSDeliveryResult;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.server.PSServer;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.types.PSPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

/**
 * This handler delivers content to the amazon s3.
 *
 * <p>Uses AWS SDK for Java v2 ({@code software.amazon.awssdk}). The v1 SDK and the product-local
 * {@code com.amazonaws.services.s3.transfer.TransferManagerBuilder} shim are no longer used (see
 * issue #1730).
 *
 * <p>EC2 detection and region resolution use {@link PSEc2MetadataClient} (IMDSv2 with IMDSv1
 * fallback). See that class for Amazon Linux 2023+ / container hop-limit operator notes (issue
 * #2284).
 */
public class PSAmazonS3DeliveryHandler extends PSBaseDeliveryHandler
{
    private static final String CREDS_WRONG_MSG = "Either bucket {} doesn't exist or the credentials to access the bucket are wrong. Error: {}";
    /** Default AWS region when none is configured; matches v1's {@code Regions.DEFAULT_REGION}. */
    private static final String DEFAULT_REGION = "us-east-1";

    private String targetRegion = DEFAULT_REGION;
    /** Per-job S3 client (v2 {@link S3Client}) - replaces v1's cached TransferManager. */
    private final ConcurrentHashMap<Long, S3Client> jobTransferManagers = new ConcurrentHashMap<>();
    /** JVM-lifetime cache of the EC2 probe result (null until first probe). */
    private static Boolean isEC2Instance = null;
    /** Shared metadata client; replaceable in unit tests via {@link #setEc2MetadataClientForTests}. */
    private static volatile PSEc2MetadataClient ec2MetadataClient = new PSEc2MetadataClient();
    private static final Logger log = LogManager.getLogger(PSAmazonS3DeliveryHandler.class);

    /**
     * Factory used to build the per-job {@link S3Client}. Hooks the v2 SDK v1's implicit
     * {@code AmazonS3ClientBuilder.build()} chain so unit tests can substitute a mock client.
     * Defaults to the static {@link #getS3Client(IPSPubServer, Region)} implementation.
     */
    @FunctionalInterface
    public interface S3ClientFactory {
        S3Client create(IPSPubServer pubServer, Region region) throws PSDeliveryException;
    }

    private volatile S3ClientFactory s3ClientFactory = PSAmazonS3DeliveryHandler::getS3Client;

    /**
     * Replace the factory used to build the per-job S3 client. Intended for unit tests; the
     * factory should not be changed concurrently with delivery.
     */
    public void setS3ClientFactory(S3ClientFactory factory) {
        this.s3ClientFactory = factory;
    }

    private S3ClientFactory factory() {
        var f = s3ClientFactory;
        return f == null ? PSAmazonS3DeliveryHandler::getS3Client : f;
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public void setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
    }

    @Override
    public void init(long jobid, IPSSite site, IPSPubServer pubServer) throws PSDeliveryException {
        super.init(jobid, site, pubServer);
        jobTransferManagers.computeIfAbsent(jobid, k -> {
            try {
                return factory().create(pubServer, getConfiguredAWSRegion());
            } catch (PSDeliveryException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /** Resolves the configured target region as a v2 {@link Region}. */
    private Region getConfiguredAWSRegion() {
        return Region.of(targetRegion);
    }

    @Override
    protected void releaseForDelivery(long jobId) {
        super.releaseForDelivery(jobId);
        var s3 = jobTransferManagers.remove(jobId);
        if (s3 != null) {
            s3.close();
        }
    }

    /**
     * Remove the single item specified by location. This method can be
     * overridden in a subclass.
     *
     * @param item The item to be removed
     * @param jobId The current jobId
     * @param location the location, never <code>null</code> or empty.
     * @return the result of the removal operation
     */
    @Override
    protected IPSDeliveryResult doRemoval(Item item, long jobId, String location) {
        var job = m_jobData.get(jobId);
        var pubServer = job.m_pubServer;
        PSDeliveryException de = null;
        var destPath = location.substring(1);
        var bucketName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "");
        try {
            var s3Client = factory().create(pubServer, getConfiguredAWSRegion());
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(destPath)
                    .build());
        } catch (PSDeliveryException e) {
            de = e;
        } catch (Exception e) {
            de = new PSDeliveryException(
                    DeliveryErrorCodes.COULD_NOT_DELETE_FROM_AMAZON, e, location, bucketName, getExceptionMessage(e));
        }
        if (de != null) {
            return getItemResult(Outcome.FAILED, item, jobId, de.getLocalizedMessage());
        }
        return getItemResult(Outcome.DELIVERED, item, jobId, null);
    }

    @Override
    protected IPSDeliveryResult doDelivery(Item item, long jobId, String location) throws PSDeliveryException {
        if (StringUtils.isBlank(location)) {
            throw new IllegalArgumentException("location may not be null or empty");
        }
        var job = m_jobData.get(jobId);
        var pubServer = job.m_pubServer;
        PSDeliveryException de = null;
        var key = location.substring(1);
        var bucketName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "");
        try {
            var s3Client = jobTransferManagers.computeIfAbsent(jobId,
                    k -> {
                        try {
                            return factory().create(pubServer, getConfiguredAWSRegion());
                        } catch (PSDeliveryException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
            if (item.getFile() != null) {
                var checksum = "";
                try (var is = new FileInputStream(item.getFile())) {
                    if (PSServer.getServerProps().getProperty("optimizePublishWithChecksum", "false").equalsIgnoreCase("true")) {
                        checksum = calculateChecksum(is);
                        var checksumValueChanged = true;
                        log.debug("local CheckSum value -> {}", checksum);
                        try {
                            var headResp = s3Client.headObject(HeadObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(key)
                                    .build());
                            if (headResp != null) {
                                var s3CheckSum = headResp.metadata() == null ? null
                                        : headResp.metadata().get("Perc-Content-Checksum");
                                log.debug("S3 Checksum  property -> {}", s3CheckSum);
                                if (checksum != null && checksum.equalsIgnoreCase(s3CheckSum)) {
                                    checksumValueChanged = false;
                                }
                            }
                        } catch (NoSuchKeyException e) {
                            log.debug("The object {} was not found so this is a new item.", key);
                            checksumValueChanged = true;
                        } catch (S3Exception e) {
                            if (e.statusCode() == 404) {
                                log.debug("The object {} was not found so this is a new item.", key);
                            } else {
                                log.error(PSExceptionUtils.getMessageForLog(e));
                            }
                            checksumValueChanged = true;
                        }
                        if (checksumValueChanged) {
                            copyToAmazonDirect(s3Client, bucketName, key, item.getFile(), item.getMimeType(), item.getLength(), checksum);
                        }
                    } else {
                        copyToAmazonDirect(s3Client, bucketName, key, item.getFile(), item.getMimeType(), item.getLength(), checksum);
                    }
                }
            } else {
                try (var is = item.getResultStream()) {
                    copyToAmazon(s3Client, bucketName, key, is, item.getMimeType(), item.getLength());
                }
            }
        } catch (Exception e) {
            de = new PSDeliveryException(
                    DeliveryErrorCodes.COULD_NOT_COPY_TO_AMAMZON, e, location, bucketName, getExceptionMessage(e));
        }
        if (de != null) {
            return getItemResult(Outcome.FAILED, item, jobId, de.getLocalizedMessage());
        }
        return new PSDeliveryResult(Outcome.DELIVERED, null, item.getId(), jobId, item.getReferenceId(), location.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * calculate the checksum of provided InputStream
     *
     * @param originalInputStream the result data stream, should not be null. The input stream should
     * be closed by the caller.
     *
     * @return return the checksum value
     */

    public String calculateChecksum(InputStream originalInputStream) {
        var result = "";
        try {
            var byteArray = IOUtils.toByteArray(originalInputStream);
            result = DigestUtils.sha256Hex(byteArray);
        } catch (Exception e) {
            log.error("Exception occurred while calculateChecksum -- > {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        return result;
    }

    private void copyToAmazon(S3Client s3, String bucketName, String key, InputStream is, String mimeType, long contentLength) throws SdkException {
        var putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(mimeType)
                .contentLength(contentLength)
                .cacheControl("max-age=0")
                .build();
        s3.putObject(putReq, RequestBody.fromInputStream(is, contentLength));
    }

    private void copyToAmazonDirect(S3Client s3, String bucketName, String key, File file, String mimeType, long contentLength, String checksum) throws IOException {
        var putReq = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(mimeType)
                .contentLength(contentLength)
                .cacheControl("max-age=20")
                .metadata(Map.of("Perc-Content-Checksum", checksum))
                .build();
        try (var fileInputStream = new FileInputStream(file)) {
            s3.putObject(putReq, RequestBody.fromInputStream(fileInputStream, contentLength));
        }
    }

    /**
     * Replace the EC2 metadata client used by {@link #isEC2Instance()} / {@link
     * #getCurrentEc2Region()}. Intended for unit tests only; clears the JVM-lifetime EC2 cache.
     *
     * @param client client to use, or {@code null} to restore the production default
     */
    static void setEc2MetadataClientForTests(PSEc2MetadataClient client) {
        ec2MetadataClient = client != null ? client : new PSEc2MetadataClient();
        isEC2Instance = null;
    }

    /**
     * Clears the JVM-lifetime EC2 detection cache. Intended for unit tests.
     */
    static void clearEc2InstanceCacheForTests() {
        isEC2Instance = null;
    }

    /**
     * @return {@code true} if this JVM appears to be running on EC2 (IMDSv2-aware probe with
     *     IMDSv1 fallback). Result is cached for the JVM lifetime after the first call.
     */
    public static boolean isEC2Instance() {
        if (isEC2Instance != null) {
            return isEC2Instance;
        }
        try {
            isEC2Instance = Boolean.valueOf(ec2MetadataClient.isAvailable());
        } catch (Exception e) {
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            isEC2Instance = Boolean.FALSE;
        }
        return isEC2Instance;
    }

    /**
     * Returns the EC2 instance region by querying the EC2 instance metadata service (IMDSv2-aware)
     * for the availability zone and stripping the trailing AZ letter. Returns {@code null} if the
     * host is not on EC2 or the metadata call fails.
     *
     * <p>Replaces v1's {@code com.amazonaws.regions.Regions.getCurrentRegion()} which has no
     * direct v2 equivalent.
     */
    public static String getCurrentEc2Region() {
        if (!isEC2Instance()) {
            return null;
        }
        try {
            return ec2MetadataClient.getRegion();
        } catch (Exception e) {
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return null;
        }
    }

    /**
     * Build a v2 {@link S3Client} for the given publishing server. Replaces the v1
     * {@code getAmazonS3Client(IPSPubServer, Region)} factory and supports:
     *
     * <ul>
     *   <li>Assume-role (STS) with either static keys or EC2 instance profile</li>
     *   <li>EC2 instance profile (no role assumption)</li>
     *   <li>Static access/secret key pair</li>
     * </ul>
     */
    public static S3Client getS3Client(IPSPubServer pubServer, Region configuredRegion) throws PSDeliveryException {
        var selectedRegionName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, "");
        if (StringUtils.isBlank(selectedRegionName)) {
            // Fall back to EC2 IMDS-resolved "current" region, else to the configured target.
            try {
                var ec2Region = getCurrentEc2Region();
                if (StringUtils.isNotBlank(ec2Region)) {
                    selectedRegionName = ec2Region;
                }
            } catch (Exception e) {
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            if (StringUtils.isBlank(selectedRegionName) && configuredRegion != null) {
                selectedRegionName = configuredRegion.id();
            }
        }
        if (StringUtils.isBlank(selectedRegionName)) {
            selectedRegionName = DEFAULT_REGION;
        }
        var region = Region.of(selectedRegionName);

        AwsCredentialsProvider creds;
        if (useAssumeRole(pubServer)) {
            creds = buildAssumeRoleCredentialsProvider(pubServer, region);
        } else if (isEC2Instance()) {
            log.debug("EC2 Instance Running");
            creds = InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build();
        } else {
            var accessKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, "");
            var secretKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, "");
            try {
                accessKey = decrypt(accessKey);
                secretKey = decrypt(secretKey);
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                throw new PSDeliveryException(DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS, e, getExceptionMessage(e));
            }
            creds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }

        return S3Client.builder()
                .region(region)
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(false).build())
                .build();
    }

    private static boolean useAssumeRole(IPSPubServer pubServer) {
        var assumeVal = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_USE_ASSUME_ROLE, "false");
        return Boolean.parseBoolean(assumeVal);
    }

    /**
     * Build an STS-backed credentials provider that assumes the configured role. The STS client
     * itself is credentialed by either the EC2 instance profile or static access/secret keys,
     * matching the v1 behavior of {@code getS3FromAssumeRole}.
     */
    private static AwsCredentialsProvider buildAssumeRoleCredentialsProvider(IPSPubServer pubServer, Region region)
            throws PSDeliveryException {
        try {
            var roleARN = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ARN_ROLE, "");
            AwsCredentialsProvider stsCreds;
            if (isEC2Instance()) {
                log.debug("EC2 Instance Running");
                stsCreds = InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build();
            } else {
                var accessKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, "");
                var secretKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, "");
                try {
                    accessKey = decrypt(accessKey);
                    secretKey = decrypt(secretKey);
                } catch (Exception e) {
                    log.error(PSExceptionUtils.getMessageForLog(e));
                    throw new PSDeliveryException(DeliveryErrorCodes.COULD_NOT_DECRYPT_CREDENTIALS, e, getExceptionMessage(e));
                }
                stsCreds = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
            }
            var stsClient = StsClient.builder()
                    .region(region)
                    .credentialsProvider(stsCreds)
                    .build();
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(b -> b.roleArn(roleARN)
                            .roleSessionName("CMS-S3Publishing-UsingAssumeRole"))
                    .build();
        } catch (SdkException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            throw new PSDeliveryException(DeliveryErrorCodes.COULD_NOT_COPY_TO_AMAMZON, e, getExceptionMessage(e));
        }
    }

    /**
     * Decrypt the string. Tries modern AES/GCM ({@link PSEncryptor}) first, then falls back to
     * legacy {@link PSAesCBC} for pre-8.2 ciphertext during upgrade. New secrets use
     * {@link PSEncryptor#encryptString} only — never {@code PSAesCBC.encrypt}.
     * @param dstr base64 encoded encrypted string
     * @return clear text version of the string.
     */
    private static String decrypt(String dstr) {
        try {
            return PSEncryptor.decryptString(PSServer.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), dstr);
        } catch (PSEncryptionException e) {
            log.warn("Decryption failed: {}. Attempting to decrypt with legacy algorithm", PSExceptionUtils.getMessageForLog(e));
            try {
                // Decrypt-only upgrade fallback (accepted-risk T047/T048)
                var aes = new PSAesCBC();
                // Fallback: use a static key or document that legacy decryption is not supported
                // TODO: Replace "legacyKey" with the actual key if available, or handle gracefully
                return aes.decrypt(dstr, "legacyKey");
            } catch (PSEncryptionException psEncryptionException) {
                log.error("Unable to decrypt string. Error: {}", PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                return dstr;
            }
        }
    }

    private static String getExceptionMessage(Exception e) {
        return (StringUtils.isBlank(e.getLocalizedMessage()) ? e.getClass().getName() : e.getLocalizedMessage());
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.rx.delivery.impl.PSBaseDeliveryHandler#checkConnection(com.percussion.services.pubserver.IPSPubServer, com.percussion.services.sitemgr.IPSSite)
     */
    @Override
    public boolean checkConnection(IPSPubServer pubServer, IPSSite site) {
        var result = true;
        var bucketName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "");
        S3Client s3Client = null;
        try {
            s3Client = factory().create(pubServer, getConfiguredAWSRegion());
            // Auth probe: listBuckets requires ListBucket permission; matches v1's getS3AccountOwner().
            ListBucketsResponse listResp = s3Client.listBuckets();
            if (listResp == null) {
                result = false;
            } else {
                // Bucket existence check via headBucket.
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                result = true;
            }
        } catch (Exception e) {
            log.error(CREDS_WRONG_MSG, bucketName, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            result = false;
        } finally {
            if (s3Client != null) {
                s3Client.close();
            }
        }
        return result;
    }

    public PSPair<Boolean, String> publishTestImage(IPSPubServer pubServer, IPSSite site, String token) {
        if (!checkConnection(pubServer, site)) {
            return new PSPair<>(Boolean.FALSE, CREDS_WRONG_MSG);
        }
        var result = new PSPair<>(Boolean.TRUE, "Successfully published, accessed and deleted image to amazon s3");
        var key = "Assets/uploads/" + generateTestImageKey(token);
        var bucketName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "");
        S3Client tm = null;
        try (var in = new FileInputStream(PSServer.getRxDir().getAbsolutePath() + PERC_TEST_IMG_DIR + PERC_TEST_IMG)) {
            tm = factory().create(pubServer, getConfiguredAWSRegion());
            copyToAmazon(tm, bucketName, key, in, "image/jpeg", in.available());
            var getResp = tm.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build());
            getResp.close();
            tm.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (Exception e) {
            log.error("Error copying image to amazon s3 bucket. {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            result = new PSPair<>(Boolean.FALSE, e.getLocalizedMessage());
        } finally {
            if (tm != null)
                tm.close();
        }
        return result;
    }

    public static String generateTestImageKey(String token) {
        return FilenameUtils.getBaseName(PSAmazonS3DeliveryHandler.PERC_TEST_IMG) + "-" + token
                + "." + FilenameUtils.getExtension(PSAmazonS3DeliveryHandler.PERC_TEST_IMG);
    }

    public static final String PERC_TEST_IMG = "percussion_test_image_donotuse.jpg";
    public static final String PERC_TEST_IMG_DIR = "/sys_resources/images/";
}
