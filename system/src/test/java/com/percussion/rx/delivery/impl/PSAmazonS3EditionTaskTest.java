/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.pubserver.data.PSPubServer;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Unit tests for {@link PSAmazonS3EditionTask} with Mockito 5 mock construction of the v2 {@link
 * S3Client}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PSAmazonS3EditionTaskTest {

  private static final String BUCKET = "edition-bucket";
  private static final String REGION = "us-east-1";

  @TempDir File webResRoot;

  private PSAmazonS3EditionTask task;
  private IPSPubServer pubServer;
  private S3Client mockS3Client;
  private MockedStatic<PSSiteManagerLocator> locatorStatic;

  @BeforeEach
  public void setUp() throws Exception {
    task = new PSAmazonS3EditionTask();
    task.setTargetRegion(REGION);

    // The task's web_resources folder is the temp dir root (no nested web_resources subdir).
    // generateKey() will produce "<web_resources>/css/site.css" etc, matching the real layout.
    webResRoot.mkdirs();
    writeFile(new File(webResRoot, "css/site.css"), "body{color:red}");
    writeFile(new File(webResRoot, "js/site.js"), "console.log(1)");
    writeFile(new File(webResRoot, "Thumbs.db"), "ignore");
    writeFile(new File(webResRoot, ".hidden"), "ignore");
    // Replace the task's private webResFolder via reflection so it walks our temp dir.
    var wfField = PSAmazonS3EditionTask.class.getDeclaredField("webResFolder");
    wfField.setAccessible(true);
    wfField.set(task, webResRoot);
    var wfpField = PSAmazonS3EditionTask.class.getDeclaredField("webResFolderPath");
    wfpField.setAccessible(true);
    wfpField.set(task, webResRoot.getAbsolutePath());

    pubServer = mock(IPSPubServer.class);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, ""))
        .thenReturn(BUCKET);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, ""))
        .thenReturn(REGION);

    // Inject a mock S3Client via the factory so we never make a real AWS call.
    mockS3Client = mock(S3Client.class);
    lenient()
        .when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    task.setS3ClientFactory((ps, region) -> mockS3Client);
  }

  @AfterEach
  public void tearDown() {
    if (locatorStatic != null) locatorStatic.close();
  }

  private S3Client currentS3() {
    return mockS3Client;
  }

  private static void writeFile(File f, String content) throws Exception {
    f.getParentFile().mkdirs();
    try (var out = new FileOutputStream(f)) {
      out.write(content.getBytes());
    }
  }

  @Test
  public void isIgnorableFile_thumbsDbAndDotfiles_areIgnored() {
    assertTrue(task.isIgnorableFile(new File("Thumbs.db")));
    assertTrue(task.isIgnorableFile(new File(".hidden")));
    assertFalse(task.isIgnorableFile(new File("index.html")));
    assertFalse(task.isIgnorableFile(new File("image.png")));
  }

  @Test
  public void uploadFileList_uploadsEachFileWithCorrectKey() throws Exception {
    S3Client s3 = currentS3();
    File dir = webResRoot;
    File file1 = new File(webResRoot, "css/site.css");
    File file2 = new File(webResRoot, "js/site.js");

    task.uploadFileList(s3, BUCKET, "web_resources", dir, Arrays.asList(file1, file2));

    verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    // Capture keys to assert layout
    var captor = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3, times(2)).putObject(captor.capture(), any(RequestBody.class));
    var keys = captor.getAllValues().stream().map(PutObjectRequest::key).toList();
    assertTrue(keys.contains("web_resources/css/site.css"), "missing css key, got " + keys);
    assertTrue(keys.contains("web_resources/js/site.js"), "missing js key, got " + keys);
  }

  @Test
  public void uploadFileList_emptyPrefix_doesNotPrependSlash() throws Exception {
    S3Client s3 = currentS3();
    File file = new File(webResRoot, "sitemap.xml");
    Files.write(file.toPath(), "<urlset/>".getBytes());

    task.uploadFileList(s3, BUCKET, "", webResRoot, Collections.singletonList(file));

    var captor = org.mockito.ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3).putObject(captor.capture(), any(RequestBody.class));
    String key = captor.getValue().key();
    assertFalse(key.startsWith("/"), "key should not start with slash: " + key);
    assertTrue(key.endsWith("sitemap.xml"), "key should end with sitemap.xml: " + key);
  }

  @Test
  public void uploadFileList_nullFiles_doesNotInvokeS3() throws Exception {
    S3Client s3 = currentS3();
    task.uploadFileList(s3, BUCKET, "web_resources", webResRoot, null);
    verify(s3, org.mockito.Mockito.never())
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  public void getFileList_matchingEtag_isExcludedFromUploads() throws Exception {
    S3Client s3 = currentS3();
    // Compute the local sha256 of css/site.css and present it as the S3 etag -> no upload needed.
    byte[] body = Files.readAllBytes(new File(webResRoot, "css/site.css").toPath());
    String etag = org.apache.commons.codec.digest.DigestUtils.sha256Hex(body);

    ListObjectsV2Response resp =
        ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(
                S3Object.builder().key("web_resources/css/site.css").eTag(etag).build(),
                S3Object.builder().key("web_resources/old.txt").eTag("deadbeef").build())
            .build();
    when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);

    PSPair<List<File>, List<String>> result = task.getFileList(s3, BUCKET);
    // css matches, js differs => only js uploaded; old.txt is local-missing => deleted.
    assertEquals(1, result.getFirst().size(), "expected 1 upload, got " + result.getFirst().size());
    assertEquals(new File(webResRoot, "js/site.js").getAbsoluteFile(), result.getFirst().get(0));
    assertEquals(List.of("web_resources/old.txt"), result.getSecond());
  }

  @Test
  public void getFileList_etagMismatch_localMissing_addsBothBranches() throws Exception {
    S3Client s3 = currentS3();
    ListObjectsV2Response resp =
        ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(
                S3Object.builder().key("web_resources/css/site.css").eTag("not-the-etag").build())
            .build();
    when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(resp);

    PSPair<List<File>, List<String>> result = task.getFileList(s3, BUCKET);
    // css differs => upload; js is local-only (not in s3 listing) => no delete; no deletes.
    assertEquals(2, result.getFirst().size());
    assertTrue(result.getSecond().isEmpty());
  }

  @Test
  public void getFileList_pagination_continuesUntilNotTruncated() throws Exception {
    S3Client s3 = currentS3();
    ListObjectsV2Response page1 =
        ListObjectsV2Response.builder()
            .isTruncated(true)
            .nextContinuationToken("tok-1")
            .contents(S3Object.builder().key("web_resources/a").eTag("x").build())
            .build();
    ListObjectsV2Response page2 =
        ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(S3Object.builder().key("web_resources/b").eTag("y").build())
            .build();
    // Match the two successive requests with their respective continuation tokens.
    when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(page1).thenReturn(page2);

    PSPair<List<File>, List<String>> result = task.getFileList(s3, BUCKET);
    // Two S3 keys were seen; both are local-missing => both end up in deletes.
    assertEquals(2, result.getSecond().size());
    assertTrue(result.getSecond().contains("web_resources/a"));
    assertTrue(result.getSecond().contains("web_resources/b"));
    verify(s3, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
  }

  @Test
  public void perform_happyPath_uploadsAndDeletes() throws Exception {
    IPSEdition edition = mock(IPSEdition.class);
    IPSGuid pubServerId = new PSGuid(com.percussion.services.catalog.PSTypeEnum.EDITION, 123L);
    when(edition.getPubServerId()).thenReturn(pubServerId);
    IPSSite site = mock(IPSSite.class);
    IPSEditionTaskStatusCallback status = mock(IPSEditionTaskStatusCallback.class);
    IPSPubServerDao dao = mock(IPSPubServerDao.class);
    PSPubServer psPubServer = mock(PSPubServer.class);
    lenient()
        .when(psPubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, ""))
        .thenReturn(BUCKET);
    lenient()
        .when(psPubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, ""))
        .thenReturn(REGION);
    when(dao.findPubServer(pubServerId)).thenReturn(Optional.of(psPubServer));

    locatorStatic = mockStatic(PSSiteManagerLocator.class);
    locatorStatic.when(PSSiteManagerLocator::getPubServerDao).thenReturn(dao);

    S3Client s3 = currentS3();
    // Empty listing => all local files are uploaded; nothing to delete.
    ListObjectsV2Response listing = ListObjectsV2Response.builder().isTruncated(false).build();
    when(s3.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listing);

    task.perform(edition, site, new Date(), new Date(), 999L, 0L, true, java.util.Map.of(), status);

    // 2 local files in web_resources: css/site.css and js/site.js => 2 putObject calls.
    verify(s3, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
