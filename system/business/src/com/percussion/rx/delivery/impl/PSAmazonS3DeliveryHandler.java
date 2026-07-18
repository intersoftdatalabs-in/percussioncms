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

import com.amazonaws.AmazonClientException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.legacy.security.deprecated.PSAesCBC;
import com.percussion.rx.delivery.IPSDeliveryErrors;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import static jakarta.ws.rs.client.ClientBuilder.newClient;

/**
 * This handler delivers content to the amazon s3.
 */
public class PSAmazonS3DeliveryHandler extends PSBaseDeliveryHandler
{
    private static final String CREDS_WRONG_MSG = "Either bucket {} doesn't exist or the credentials to access the bucket are wrong. Error: {}";
    private String targetRegion = Regions.DEFAULT_REGION.getName();
    private final ConcurrentHashMap<Long, TransferManager> jobTransferManagers = new ConcurrentHashMap<>();
    private static Boolean isEC2Instance = null;
    private static final Logger log = LogManager.getLogger(PSAmazonS3DeliveryHandler.class);

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
                var s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
                return TransferManagerBuilder.standard().withS3Client(s3Client).build();
            } catch (PSDeliveryException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Region getConfiguredAWSRegion() {
        return Region.getRegion(Regions.fromName(targetRegion));
    }

    @Override
    protected void releaseForDelivery(long jobId) {
        super.releaseForDelivery(jobId);
        var t = jobTransferManagers.remove(jobId);
        if (t != null) {
            t.shutdownNow(true);
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
            var s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
            s3Client.deleteObject(bucketName, destPath);
        } catch (PSDeliveryException e) {
            de = e;
        } catch (Exception e) {
            de = new PSDeliveryException(
                    IPSDeliveryErrors.COULD_NOT_DELETE_FROM_AMAZON, e, location, bucketName, getExceptionMessage(e));
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
            var s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
            var tm = jobTransferManagers.computeIfAbsent(jobId, k -> TransferManagerBuilder.standard().withS3Client(s3Client).build());
            if (item.getFile() != null) {
                var checksum = "";
                try (var is = new FileInputStream(item.getFile())) {
                    if (PSServer.getServerProps().getProperty("optimizePublishWithChecksum", "false").equalsIgnoreCase("true")) {
                        checksum = calculateChecksum(is);
                        var checksumValueChanged = true;
                        log.debug("local CheckSum value -> {}", checksum);
                        try {
                            var mreq = new GetObjectMetadataRequest(bucketName, key);
                            var retrieved_metadata = s3Client.getObjectMetadata(mreq);
                            if (retrieved_metadata != null) {
                                var s3CheckSum = retrieved_metadata.getUserMetaDataOf("Perc-Content-Checksum");
                                log.debug("S3 Checksum  property -> {}", s3CheckSum);
                                if (checksum != null && checksum.equalsIgnoreCase(s3CheckSum)) {
                                    checksumValueChanged = false;
                                }
                            }
                        } catch (AmazonS3Exception e) {
                            if (e.getStatusCode() == 404) {
                                log.debug("The object {} was not found so this is a new item.", key);
                            } else {
                                log.error(PSExceptionUtils.getMessageForLog(e));
                            }
                            checksumValueChanged = true;
                        }
                        if (checksumValueChanged) {
                            copyToAmazonDirect(tm, bucketName, key, item.getFile(), item.getMimeType(), item.getLength(), checksum);
                        }
                    } else {
                        copyToAmazonDirect(tm, bucketName, key, item.getFile(), item.getMimeType(), item.getLength(), checksum);
                    }
                }
            } else {
                try (var is = item.getResultStream()) {
                    copyToAmazon(tm, bucketName, key, is, item.getMimeType(), item.getLength());
                }
            }
        } catch (PSDeliveryException e) {
            de = e;
        } catch (Exception e) {
            de = new PSDeliveryException(
                    IPSDeliveryErrors.COULD_NOT_COPY_TO_AMAMZON, e, location, bucketName, getExceptionMessage(e));
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

    private void copyToAmazon(TransferManager tm, String bucketName, String key, InputStream is, String mimeType, long contentLength) throws AmazonClientException, InterruptedException {
        var metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        metadata.setContentLength(contentLength);
        metadata.setCacheControl("max-age=0");
        var myUpload = tm.upload(new PutObjectRequest(bucketName, key, is, metadata));
        myUpload.waitForCompletion();
    }

    private void copyToAmazonDirect(TransferManager tm, String bucketName, String key, File file, String mimeType, long contentLength, String checksum) throws IOException, InterruptedException {
        var metadata = new ObjectMetadata();
        metadata.setContentType(mimeType);
        metadata.setContentLength(contentLength);
        metadata.setCacheControl("max-age=20");
        metadata.addUserMetadata("Perc-Content-Checksum", checksum);
        try (var fileInputStream = new FileInputStream(file)) {
            var myUpload = tm.upload(new PutObjectRequest(bucketName, key, fileInputStream, metadata));
            myUpload.waitForCompletion();
        }
    }

    public static boolean isEC2Instance() {
        if (isEC2Instance != null) {
            return isEC2Instance;
        }
        try {
            var client = newClient();
            var resource = client.target("http://169.254.169.254/latest/meta-data/");
            var request = resource.request();
            request.accept(MediaType.APPLICATION_JSON);
            var response = request.get();
            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                isEC2Instance = Boolean.TRUE;
                return true;
            } else {
                isEC2Instance = Boolean.FALSE;
            }
        } catch (Exception e) {
            isEC2Instance = Boolean.FALSE;
        }
        return isEC2Instance;
    }

    public static AmazonS3 getAmazonS3Client(IPSPubServer pubServer, Region configuredRegion) throws PSDeliveryException {
        AmazonS3 s3 = null;
        var selectedRegionName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, "");
        if (selectedRegionName == null || selectedRegionName.trim().isEmpty()) {
            try {
                if (Regions.getCurrentRegion() != null) {
                    selectedRegionName = Regions.getCurrentRegion().getName();
                }
            } catch (Exception e) {
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            }
            if (selectedRegionName == null || selectedRegionName.trim().isEmpty()) {
                if (configuredRegion != null) {
                    selectedRegionName = configuredRegion.getName();
                }
            }
        }
        if (useAssumeRole(pubServer)) {
            s3 = getS3FromAssumeRole(pubServer);
        } else if (isEC2Instance()) {
            log.debug("EC2 Instance Running");
            s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(new InstanceProfileCredentialsProvider(false))
                    .withRegion(selectedRegionName)
                    .build();
        } else {
            var accessKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, "");
            var secretKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, "");
            try {
                accessKey = decrypt(accessKey);
                secretKey = decrypt(secretKey);
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                throw new PSDeliveryException(IPSDeliveryErrors.COULD_NOT_DECRYPT_CREDENTIALS, e, getExceptionMessage(e));
            }
            var awsCreds = new BasicAWSCredentials(accessKey, secretKey);
            s3 = AmazonS3ClientBuilder.standard().withRegion(selectedRegionName).withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        }
        return s3;
    }

    private static boolean useAssumeRole(IPSPubServer pubServer) {
        var assumeVal = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_USE_ASSUME_ROLE, "false");
        return Boolean.parseBoolean(assumeVal);
    }

    private static AmazonS3 getS3FromAssumeRole(IPSPubServer pubServer) throws PSDeliveryException {
        try {
            var selectedRegionName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, "");
            var roleARN = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ARN_ROLE, "");
            AWSSecurityTokenService stsClient = null;
            if (isEC2Instance()) {
                log.debug("EC2 Instance Running");
                stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                        .withCredentials(new InstanceProfileCredentialsProvider(false))
                        .withRegion(selectedRegionName)
                        .build();
            } else {
                var accessKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, "");
                var secretKey = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, "");
                try {
                    accessKey = decrypt(accessKey);
                    secretKey = decrypt(secretKey);
                } catch (Exception e) {
                    log.error(PSExceptionUtils.getMessageForLog(e));
                    throw new PSDeliveryException(IPSDeliveryErrors.COULD_NOT_DECRYPT_CREDENTIALS, e, getExceptionMessage(e));
                }
                var awsCreds = new BasicAWSCredentials(accessKey, secretKey);
                stsClient = AWSSecurityTokenServiceClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                        .withRegion(selectedRegionName)
                        .build();
            }
            var assumeRoleSessionBuilder =
                    new STSAssumeRoleSessionCredentialsProvider.Builder(roleARN, "CMS-S3Publishing-UsingAssumeRole");
            var s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(selectedRegionName)
                    .withCredentials(assumeRoleSessionBuilder.withStsClient(stsClient).build()).build();
            return s3Client;
        } catch (SdkClientException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            throw new PSDeliveryException(IPSDeliveryErrors.COULD_NOT_COPY_TO_AMAMZON, e, getExceptionMessage(e));
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
        try {
            var s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
            s3Client.getS3AccountOwner();
            result = s3Client.doesBucketExistV2(bucketName);
        } catch (Exception e) {
            log.error(CREDS_WRONG_MSG, bucketName, PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            result = false;
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
        TransferManager tm = null;
        try (var in = new FileInputStream(PSServer.getRxDir().getAbsolutePath() + PERC_TEST_IMG_DIR + PERC_TEST_IMG)) {
            var s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
            tm = TransferManagerBuilder.standard().withS3Client(s3Client).build();
            copyToAmazon(tm, bucketName, key, in, "image/jpeg", in.available());
            s3Client = getAmazonS3Client(pubServer, getConfiguredAWSRegion());
            s3Client.getObject(bucketName, key);
            s3Client.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("Error copying image to amazon s3 bucket. {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            result = new PSPair<>(Boolean.FALSE, e.getLocalizedMessage());
        } finally {
            if (tm != null)
                tm.shutdownNow();
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
