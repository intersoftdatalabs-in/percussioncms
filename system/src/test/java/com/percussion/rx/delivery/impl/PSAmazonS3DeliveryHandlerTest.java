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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rx.delivery.IPSDeliveryResult.Outcome;
import com.percussion.rx.delivery.impl.PSBaseDeliveryHandler.Item;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.pubserver.IPSPubServer;
import com.percussion.services.pubserver.IPSPubServerDao;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.guid.IPSGuid;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Unit tests for {@link PSAmazonS3DeliveryHandler} using Mockito 5.
 *
 * <p>The v2 SDK {@link S3Client} is an interface (the concrete implementation is package-private
 * {@code DefaultS3Client}); to avoid going through a real network, each test installs a {@link
 * PSAmazonS3DeliveryHandler.S3ClientFactory} that returns a hand-rolled mock {@link S3Client}.
 * Static mocking of {@link PSAmazonS3DeliveryHandler#isEC2Instance()} makes the credentials-routing
 * branches deterministic on non-EC2 test hosts.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PSAmazonS3DeliveryHandlerTest {

  private static final long JOB_ID = 1001L;
  private static final long REF_ID = 2002L;
  private static final long PUB_SERVER_ID = 3003L;
  private static final int DELIVERY_CONTEXT = 1;
  private static final String BUCKET = "test-bucket";
  private static final String ACCESS_KEY = "AKIA-test";
  private static final String SECRET_KEY = "secret-test";
  private static final String REGION = "us-east-1";
  private static final String ARN_ROLE = "arn:aws:iam::123456789012:role/TestRole";

  private PSAmazonS3DeliveryHandler handler;
  private IPSPubServer pubServer;
  private IPSSite site;
  private S3Client mockS3Client;
  private MockedStatic<PSAmazonS3DeliveryHandler> handlerStatic;

  @BeforeEach
  public void setUp() {
    handler = new PSAmazonS3DeliveryHandler();
    handler.setTargetRegion(REGION);

    pubServer = mock(IPSPubServer.class);
    site = mock(IPSSite.class);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_BUCKET_PROPERTY, ""))
        .thenReturn(BUCKET);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, ""))
        .thenReturn(REGION);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_USE_ASSUME_ROLE, "false"))
        .thenReturn("false");
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ACCESSKEY_PROPERTY, ""))
        .thenReturn(ACCESS_KEY);
    lenient()
        .when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_SECURITYKEY_PROPERTY, ""))
        .thenReturn(SECRET_KEY);

    // Default: not running on EC2 so the static-key branch is exercised.
    handlerStatic =
        mockStatic(PSAmazonS3DeliveryHandler.class, org.mockito.Mockito.CALLS_REAL_METHODS);
    handlerStatic.when(PSAmazonS3DeliveryHandler::isEC2Instance).thenReturn(false);

    // Inject a mock S3Client via the factory so we never make a real AWS call.
    mockS3Client = mock(S3Client.class);
    lenient()
        .when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    lenient()
        .when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());
    // Production code calls the no-arg overload; stub both the no-arg and the request-arg forms.
    lenient().when(mockS3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().build());
    lenient()
        .when(mockS3Client.listBuckets(any(ListBucketsRequest.class)))
        .thenReturn(ListBucketsResponse.builder().build());
    lenient()
        .when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
        .thenReturn(HeadBucketResponse.builder().build());
    handler.setS3ClientFactory((ps, region) -> mockS3Client);
  }

  @AfterEach
  public void tearDown() {
    if (handlerStatic != null) handlerStatic.close();
    // Reset the static isEC2Instance cache so it doesn't leak between tests.
    try {
      Field f = PSAmazonS3DeliveryHandler.class.getDeclaredField("isEC2Instance");
      f.setAccessible(true);
      f.set(null, (Object) null);
    } catch (Exception e) {
      // ignore
    }
  }

  private PSBaseDeliveryHandler.JobData newJobData() {
    var jd = new PSBaseDeliveryHandler.JobData(site, pubServer);
    jd.m_pubServer = pubServer;
    return jd;
  }

  private void seedJobData() throws Exception {
    Field f = PSBaseDeliveryHandler.class.getDeclaredField("m_jobData");
    f.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map = (java.util.Map<Long, PSBaseDeliveryHandler.JobData>) f.get(handler);
    // Per-job S3Client is cached separately; seed the job data map.
    map.put(JOB_ID, newJobData());
  }

  private Item fileItem(PSPurgableTempFile tempFile, String mimeType) {
    IPSGuid id = new PSGuid(com.percussion.services.catalog.PSTypeEnum.LEGACY_CONTENT, 1);
    return handler
    .new Item(id, tempFile, null, mimeType, REF_ID, false, JOB_ID, PUB_SERVER_ID, DELIVERY_CONTEXT);
  }

  private Item streamItem(byte[] data, String mimeType) {
    IPSGuid id = new PSGuid(com.percussion.services.catalog.PSTypeEnum.LEGACY_CONTENT, 1);
    return handler
    .new Item(
        id,
        null,
        data,
        null,
        data.length,
        mimeType,
        REF_ID,
        false,
        JOB_ID,
        PUB_SERVER_ID,
        DELIVERY_CONTEXT);
  }

  @Test
  public void doDelivery_fromFile_uploadsObject() throws Exception {
    seedJobData();
    var tempFile = new PSPurgableTempFile("test", ".html", null);
    try (var out = new java.io.FileOutputStream(tempFile)) {
      out.write("<html>hi</html>".getBytes());
    }
    var item = fileItem(tempFile, "text/html");

    var result = handler.doDelivery(item, JOB_ID, "/index.html");

    assertEquals(Outcome.DELIVERED, result.getOutcome());
    verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  public void doDelivery_fromStream_uploadsObject() throws Exception {
    seedJobData();
    byte[] body = "streamed body".getBytes();
    var item = streamItem(body, "text/plain");

    var result = handler.doDelivery(item, JOB_ID, "/stream.txt");

    assertEquals(Outcome.DELIVERED, result.getOutcome());
    verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  public void doDelivery_blankLocation_throwsIllegalArgument() throws Exception {
    seedJobData();
    byte[] body = "x".getBytes();
    var item = streamItem(body, "text/plain");
    assertThrows(IllegalArgumentException.class, () -> handler.doDelivery(item, JOB_ID, ""));
  }

  @Test
  public void doDelivery_s3PutThrows_returnsFailedOutcome() throws Exception {
    seedJobData();
    byte[] body = "x".getBytes();
    var item = streamItem(body, "text/plain");

    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("boom").build());

    var result = handler.doDelivery(item, JOB_ID, "/fail.txt");
    assertEquals(Outcome.FAILED, result.getOutcome());
    assertNotNull(result.getFailureMessage());
  }

  @Test
  public void doRemoval_succeeds_callsDeleteObject() throws Exception {
    seedJobData();
    IPSGuid id = new PSGuid(com.percussion.services.catalog.PSTypeEnum.LEGACY_CONTENT, 1);
    var item =
        handler
        .new Item(
            id,
            null,
            "x".getBytes(),
            null,
            1,
            "text/plain",
            REF_ID,
            true,
            JOB_ID,
            PUB_SERVER_ID,
            DELIVERY_CONTEXT);

    var result = handler.doRemoval(item, JOB_ID, "/remove/me.txt");
    assertEquals(Outcome.DELIVERED, result.getOutcome());
    verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  public void doRemoval_s3Throws_returnsFailedOutcome() throws Exception {
    seedJobData();
    IPSGuid id = new PSGuid(com.percussion.services.catalog.PSTypeEnum.LEGACY_CONTENT, 1);
    var item =
        handler
        .new Item(
            id,
            null,
            "x".getBytes(),
            null,
            1,
            "text/plain",
            REF_ID,
            true,
            JOB_ID,
            PUB_SERVER_ID,
            DELIVERY_CONTEXT);

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(S3Exception.builder().message("nope").build());

    var result = handler.doRemoval(item, JOB_ID, "/remove/me.txt");
    assertEquals(Outcome.FAILED, result.getOutcome());
  }

  @Test
  public void checkConnection_success_returnsTrue() {
    boolean ok = handler.checkConnection(pubServer, site);
    assertTrue(ok);
    verify(mockS3Client).headBucket(any(HeadBucketRequest.class));
  }

  @Test
  public void checkConnection_listBucketsFails_returnsFalse() {
    when(mockS3Client.listBuckets())
        .thenThrow(S3Exception.builder().statusCode(403).message("forbidden").build());

    boolean ok = handler.checkConnection(pubServer, site);
    assertFalse(ok);
  }

  @Test
  public void checkConnection_bucketMissing_returnsFalse() {
    when(mockS3Client.headBucket(any(HeadBucketRequest.class)))
        .thenThrow(s3NotFound("bucket missing"));

    boolean ok = handler.checkConnection(pubServer, site);
    assertFalse(ok);
  }

  @Test
  public void releaseForDelivery_closesCachedClient() throws Exception {
    // Seed the jobTransferManagers cache manually (we bypass init() because that calls factory()
    // and would invoke the production code path).
    Field f = PSAmazonS3DeliveryHandler.class.getDeclaredField("jobTransferManagers");
    f.setAccessible(true);
    @SuppressWarnings("unchecked")
    var map = (java.util.concurrent.ConcurrentHashMap<Long, S3Client>) f.get(handler);
    map.put(JOB_ID, mockS3Client);

    handler.releaseForDelivery(JOB_ID);
    verify(mockS3Client, times(1)).close();
  }

  @Test
  public void getTargetRegion_setTargetRegion_roundTrip() {
    var h = new PSAmazonS3DeliveryHandler();
    assertEquals(REGION, h.getTargetRegion());
    h.setTargetRegion("eu-west-1");
    assertEquals("eu-west-1", h.getTargetRegion());
  }

  @Test
  public void generateTestImageKey_buildsExpectedName() {
    String token = UUID.randomUUID().toString();
    String key = PSAmazonS3DeliveryHandler.generateTestImageKey(token);
    assertTrue(key.contains(token));
    assertTrue(key.endsWith(".jpg"));
    assertTrue(key.startsWith(PSAmazonS3DeliveryHandler.PERC_TEST_IMG.split("\\.")[0]));
  }

  // --- factory-level tests against the static getS3Client() implementation ---

  @Test
  public void getS3Client_staticKeys_returnsNonNull() throws Exception {
    // Default scenario: isEC2Instance() is mocked to false, so the static-key branch is exercised.
    S3Client s3 =
        PSAmazonS3DeliveryHandler.getS3Client(
            pubServer, software.amazon.awssdk.regions.Region.of(REGION));
    assertNotNull(s3);
  }

  @Test
  public void getS3Client_assumeRole_returnsNonNull() throws Exception {
    when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_USE_ASSUME_ROLE, "false"))
        .thenReturn("true");
    when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_AS3_ARN_ROLE, "")).thenReturn(ARN_ROLE);
    S3Client s3 =
        PSAmazonS3DeliveryHandler.getS3Client(
            pubServer, software.amazon.awssdk.regions.Region.of(REGION));
    assertNotNull(s3);
  }

  @Test
  public void getS3Client_ec2Instance_returnsNonNull() throws Exception {
    handlerStatic.when(PSAmazonS3DeliveryHandler::isEC2Instance).thenReturn(true);
    // Mark the static cache field so the real call short-circuits as well.
    Field f = PSAmazonS3DeliveryHandler.class.getDeclaredField("isEC2Instance");
    f.setAccessible(true);
    f.set(null, (Object) Boolean.TRUE);
    S3Client s3 =
        PSAmazonS3DeliveryHandler.getS3Client(
            pubServer, software.amazon.awssdk.regions.Region.of(REGION));
    assertNotNull(s3);
  }

  @Test
  public void getS3Client_blankRegionAndNoEC2_fallsBackToDefault() throws Exception {
    when(pubServer.getPropertyValue(IPSPubServerDao.PUBLISH_EC2_REGION, "")).thenReturn("");
    S3Client s3 =
        PSAmazonS3DeliveryHandler.getS3Client(
            pubServer, software.amazon.awssdk.regions.Region.of(REGION));
    assertNotNull(s3);
  }

  private static S3Exception s3NotFound(String msg) {
    return (S3Exception) S3Exception.builder().statusCode(404).message(msg).build();
  }
}
