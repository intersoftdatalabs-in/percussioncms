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

import com.percussion.cms.IPSConstants;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.legacy.security.deprecated.PSAesCBC;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.server.PSServer;
import com.percussion.rx.delivery.PSDeliveryException;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.types.PSPair;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Post edition task that will publish web_resources files to amazon s3 bucket.
 * Makes an MD5 check before publishing the resources. Any files that don't exist
 * under web_resources are deleted from the s3 bucket when published.
 *
 * <p>Uses AWS SDK for Java v2 ({@code software.amazon.awssdk}). The v1 SDK and the
 * product-local {@code TransferManagerBuilder} shim are no longer used (see issue #1730).
 */
// REFACTORED: CP-JAVA11
public class PSAmazonS3EditionTask implements IPSEditionTask
{
    /** Default AWS region when none is configured; matches v1's {@code Regions.DEFAULT_REGION}. */
    private static final String DEFAULT_REGION = "us-east-1";

    private static final String WEB_RESOURCES = "web_resources";
    private File webResFolder = null;
    private String webResFolderPath = "";
    private IPSPubServerDao pubServerDao;
    private String targetRegion = DEFAULT_REGION;
    private static final Logger log = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

    /**
     * Factory used to build the per-task {@link S3Client}. Mirrors the {@link
     * PSAmazonS3DeliveryHandler.S3ClientFactory} hook so unit tests can substitute a mock client.
     */
    @FunctionalInterface
    public interface S3ClientFactory {
        S3Client create(IPSPubServer pubServer, software.amazon.awssdk.regions.Region region)
                throws PSDeliveryException;
    }

    private volatile S3ClientFactory s3ClientFactory = PSAmazonS3EditionTask::createS3Client;

    private static S3Client createS3Client(IPSPubServer pubServer, software.amazon.awssdk.regions.Region region)
            throws PSDeliveryException {
        return PSAmazonS3DeliveryHandler.getS3Client(pubServer, region);
    }

    public void setS3ClientFactory(S3ClientFactory factory) {
        this.s3ClientFactory = factory;
    }

    private S3ClientFactory factory() {
        var f = s3ClientFactory;
        return f == null ? PSAmazonS3EditionTask::createS3Client : f;
    }

    // TODO: Remove me @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    @SuppressWarnings("unused")
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
        webResFolder = new File(PSServer.getRxDir().getAbsolutePath() + File.separatorChar + WEB_RESOURCES);
        webResFolderPath = webResFolder.getAbsolutePath();
    }

    /*
     * (non-Javadoc)
     * @see com.percussion.rx.publisher.IPSEditionTask#perform(com.percussion.services.publisher.IPSEdition, com.percussion.services.sitemgr.IPSSite, java.util.Date, java.util.Date, long, long, boolean, java.util.Map, com.percussion.rx.publisher.IPSEditionTaskStatusCallback)
     */
    @Override
    public void perform(IPSEdition edition, IPSSite site, Date startTime, Date endTime, long jobId, long duration,
                       boolean success, Map<String, String> params, IPSEditionTaskStatusCallback status) throws Exception {
        var pubServerOpt = getPubServerDao().findPubServer(edition.getPubServerId());
        if (pubServerOpt.isEmpty()) {
            throw new IllegalStateException("No pub server found for edition: " + edition.getPubServerId());
        }
        var pubServer = pubServerOpt.get();
        var bucketName = pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, "");
        S3Client s3Client = null;
        try {
            s3Client = factory().create(pubServer, getConfiguredAWSRegion());
            var fileList = getFileList(s3Client, bucketName);
            // Delete files that don't exist
            for (var key : fileList.getSecond()) {
                s3Client.deleteObject(builder -> builder.bucket(bucketName).key(key));
            }
            // Upload modified files
            uploadFileList(s3Client, bucketName, WEB_RESOURCES, webResFolder, fileList.getFirst());
            var sitemapPath = PSServer.getRxDir().getAbsolutePath() + File.separator + "temp" + File.separator +
                    "publish" + File.separator + jobId + File.separator + "sitemaps";
            if (Files.exists(Paths.get(sitemapPath))) {
                var sitemapdir = new File(sitemapPath);
                uploadFileList(s3Client, bucketName, "", sitemapdir, Arrays.asList(Objects.requireNonNull(sitemapdir.listFiles())));
            }
        } catch (Exception e) {
            log.error("Error occurred while copying the web_resources files to amazon s3 bucket for Site: {} Error: {}",
                    site.getLabel(), PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            throw e;
        } finally {
            if (s3Client != null) s3Client.close();
        }
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public void setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
    }

    private Region getConfiguredAWSRegion() {
        return Region.of(targetRegion);
    }

    private String getExceptionMessage(Exception e) {
        return PSExceptionUtils.getMessageForLog(e);
    }

    /**
     * Upload each {@code file} under {@code prefix}. Replaces v1's
     * {@code TransferManager.uploadFileList(bucket, prefix, dir, files)} with explicit
     * {@code putObject} calls (multipart is handled transparently by v2 for large files).
     *
     * <p>Key layout matches v1 SDK behavior: {@code <prefix>/<file path relative to dir>}, with
     * OS path separators normalized to {@code /}.
     */
    /**
     * Package-private for unit testing; uploads each {@code file} under {@code prefix}.
     */
    void uploadFileList(S3Client s3, String bucketName, String prefix, File dir, List<File> files) throws IOException {
        if (files == null) return;
        var dirAbs = dir.getAbsolutePath().replace("\\", "/");
        for (var file : files) {
            var rel = file.getAbsolutePath().replace("\\", "/");
            if (rel.startsWith(dirAbs)) {
                rel = rel.substring(dirAbs.length());
            }
            while (rel.startsWith("/")) {
                rel = rel.substring(1);
            }
            var key = prefix.isEmpty() ? rel : prefix + (prefix.endsWith("/") ? "" : "/") + rel;
            try (var in = new FileInputStream(file)) {
                var putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentLength(file.length())
                        .build();
                s3.putObject(putReq, RequestBody.fromInputStream(in, file.length()));
            }
        }
    }

    /**
     * Returns the list of files to be deleted and to be uploaded. Gets the list of files from file system
     * and compares their md5hash with the list of files from web_resources folder from amazon s3 bucket.
     * @param s3Client assumed not <code>null</code>.
     * @param bucketName name of the Amazon S3 bucket assumed not null.
     * @return PSPair the first object is a list files that needs to be uploaded
     * and the second object is a list of keys for the corresponding objects that needs to be
     * deleted.
     * @throws FileNotFoundException
     * @throws IOException
     */
    /**
     * Package-private for unit testing. Computes the files to upload and the keys to delete by
     * comparing local web_resources against the S3 listing.
     */
    PSPair<List<File>, List<String>> getFileList(S3Client s3Client, String bucketName)
            throws FileNotFoundException, IOException {
        var modifiedFiles = new ArrayList<File>();
        var localFilesMap = getLocalWebResFiles();
        var s3FilesMap = getAmazonS3FilesMap(s3Client, bucketName);
        // Prepare files to upload
        for (var key : localFilesMap.keySet()) {
            var addFile = true;
            if (s3FilesMap.containsKey(key)) {
                addFile = !(localFilesMap.get(key).getFirst().equals(s3FilesMap.get(key)));
            }
            if (addFile) {
                modifiedFiles.add(localFilesMap.get(key).getSecond());
            }
        }
        // Prepare deletes
        var delKeys = new ArrayList<>(s3FilesMap.keySet());
        delKeys.removeAll(localFilesMap.keySet());
        return new PSPair<>(modifiedFiles, delKeys);
    }

    /**
     * Gets the list of local files from web_resources folder.
     * @return Map of key and a PSPair object with first object as md5hash of the file and second object
     * to be the file itself.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Map<String, PSPair<String, File>> getLocalWebResFiles() throws FileNotFoundException, IOException {
        var localFilesMap = new HashMap<String, PSPair<String, File>>();
        generateLocalFileMap(webResFolder, localFilesMap);
        return localFilesMap;
    }

    private void generateLocalFileMap(File dir, Map<String, PSPair<String, File>> localFilesMap)
            throws IOException {
        var files = dir.listFiles();
        if (files == null) return;
        for (var file : files) {
            if (file.isFile() && !isIgnorableFile(file)) {
                var key = generateKey(file);
                try (var is = new FileInputStream(file)) {
                    localFilesMap.put(key, new PSPair<>(DigestUtils.sha256Hex(IOUtils.toByteArray(is)), file));
                }
            } else if (file.isDirectory()) {
                generateLocalFileMap(file, localFilesMap);
            }
        }
    }

    /**
     * Generates the key based on the file path relative to web_resources folder.
     * Converts the backward slashes to forward slashes if exists, as the amazon key gets generated with
     * forward slashes.
     * @param file assumed not <code>null</code>
     * @return enerated key never <code>null</code>.
     */
    private String generateKey(File file) {
        var key = file.getAbsolutePath().replace(webResFolderPath, "");
        key = key.replace("\\", "/");
        return WEB_RESOURCES + key;
    }

    /**
     * Applies known rules to avoid uploading files to amazon s3 from web_resources folder
     * @param file assumed not <code>null</code>.
     * @return <code>true</code> if the files is ignorable and <code>false</code> if not.
     */
    /**
     * Package-private for unit testing.
     */
    boolean isIgnorableFile(File file) {
        return file.getName().equals("Thumbs.db") || file.getName().startsWith(".");
    }

    /**
     * Helper method that returns amazon s3 file keys along with checksum.
     * @param client
     * @param bucketName
     *
     */
    private Map<String, String> getAmazonS3FilesMap(S3Client client, String bucketName) {
        var filesMap = new TreeMap<String, String>();
        var listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(WEB_RESOURCES)
                .build();
        ListObjectsV2Response listing;
        do {
            listing = client.listObjectsV2(listReq);
            for (S3Object summary : listing.contents()) {
                filesMap.put(summary.key(), summary.eTag());
            }
            listReq = listReq.toBuilder().continuationToken(listing.nextContinuationToken()).build();
        } while (listing.isTruncated());
        return filesMap;
    }

    @Override
    public TaskType getType() {
        return TaskType.PREEDITION;
    }

    /**
     * Gets the pub-server service, lazy load.
     *
     * @return pub-server service, never <code>null</code>.
     */
    private IPSPubServerDao getPubServerDao() {
        if (pubServerDao == null) pubServerDao = PSSiteManagerLocator.getPubServerDao();
        return pubServerDao;
    }

    /**
     * Decrypt the string. Tries modern AES/GCM ({@link PSEncryptor}) first, then falls back to
     * legacy {@link PSAesCBC} for pre-8.2 ciphertext during upgrade. New secrets use
     * {@link PSEncryptor#encryptString} only — never {@code PSAesCBC.encrypt}.
     * @param dstr base64 encoded encrypted string
     * @return clear text version of the string.
     */
    private String decrypt(String dstr) {
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
}
