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

package com.percussion.delivery.metadata.solr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intsof.percussioncms.auditlog.codes.DeliveryErrorCodes;
import com.percussion.delivery.metadata.extractor.data.PSMetadataEntry;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import com.percussion.rx.delivery.PSDeliveryException;
import com.percussion.util.PSPurgableTempFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit / mock verification for {@link PSSolrDeliveryHandler} (#1999 / parent #1788) and SolrJ 10
 * cutover (#1997) / packaging policy (#1998).
 *
 * <p><strong>No live Solr and no network I/O</strong> on update paths — tests inject a mocked
 * {@link SolrClient}. Client construction tests build clients offline and close them without
 * issuing requests. Live Solr 9.x / 10.x + Tika Server smoke is human-gated (see verification plan
 * under {@code docs/ai-generated/tasks/1788-solrj-10/}).
 */
public class PSSolrDeliveryHandlerTest {

  private SolrClient openClient;
  private PSPurgableTempFile tempFile;

  @AfterEach
  void tearDown() throws IOException {
    if (openClient != null) {
      openClient.close();
      openClient = null;
    }
    if (tempFile != null) {
      tempFile.release();
      tempFile = null;
    }
  }

  @Test
  void createStandaloneClient_returnsHttpJdkSolrClient() throws Exception {
    openClient = PSSolrDeliveryHandler.createStandaloneClient("http://localhost:8983/solr");
    assertInstanceOf(HttpJdkSolrClient.class, openClient);
    assertInstanceOf(SolrClient.class, openClient);
  }

  /**
   * Packaging policy (#1998): product declares only {@code solr-solrj} core. Optional modules
   * {@code solr-solrj-jetty} and {@code solr-solrj-zookeeper} stay off the test/runtime classpath
   * (managed in root POM but not module dependencies; ZK also enforcer-banned).
   */
  @Test
  void packagingPolicy_optionalSolrModulesNotOnClasspath() {
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("org.apache.solr.client.solrj.jetty.HttpJettySolrClient"));
    assertThrows(
        ClassNotFoundException.class, () -> Class.forName("org.apache.zookeeper.ZooKeeper"));
  }

  @Test
  void createCloudClient_returnsCloudSolrClient_withDefaultCollection() throws Exception {
    // Solr URL constructor (not ZK). No network until first request.
    openClient =
        PSSolrDeliveryHandler.createCloudClient(
            "http://localhost:8983/solr", "metadata_collection");
    assertInstanceOf(CloudSolrClient.class, openClient);
  }

  @Test
  void createCloudClient_allowsBlankDefaultCollection() throws Exception {
    openClient = PSSolrDeliveryHandler.createCloudClient("http://localhost:8983/solr", null);
    assertInstanceOf(CloudSolrClient.class, openClient);
  }

  @Test
  void isEnabled_falseWhenNoServerConfig() {
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", null);
    assertFalse(handler.isEnabled());
  }

  @Test
  void isEnabled_trueWithServerConfig() {
    PSSolrDeliveryHandler handler =
        new PSSolrDeliveryHandler("siteA", "PRODUCTION", standaloneConfig("siteA"));
    assertTrue(handler.isEnabled());
  }

  @Test
  void delete_invokesDeleteByIdOnInjectedClient() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.deleteById(eq("/folder/page"))).thenReturn(new UpdateResponse());
    handler.setSolrClientForTests(client);

    handler.delete("/folder/page");

    verify(client).deleteById("/folder/page");
    assertTrue(config.isDelivered());
  }

  @Test
  void delete_disabledHandler_doesNotCallClient() throws Exception {
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", null);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    handler.delete("/folder/page");

    verify(client, never()).deleteById(any(String.class));
  }

  @Test
  void delete_propagatesSolrExceptionAsDeliveryException() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.deleteById(any(String.class))).thenThrow(new SolrServerException("network down"));
    handler.setSolrClientForTests(client);

    PSDeliveryException ex = assertThrows(PSDeliveryException.class, () -> handler.delete("/x"));
    assertSame(DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION, ex.getTypedErrorCode());
    assertEquals(
        DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION.numericCode(), ex.getErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void delete_throwsWhenServerInactive() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setMaxErrors(1);
    config.incrError(); // errorCount == maxErrors → inactive
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    PSDeliveryException ex = assertThrows(PSDeliveryException.class, () -> handler.delete("/x"));
    assertSame(DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
    verify(client, never()).deleteById(any(String.class));
  }

  @Test
  void commit_invokesCommitWhenDelivered() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setDelivered(true);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.commit()).thenReturn(new UpdateResponse());
    handler.setSolrClientForTests(client);

    handler.commit();

    verify(client).commit();
    verify(client).close();
    assertNull(handler.getServerConfigForTests());
  }

  @Test
  void commit_skipsWhenNothingDelivered() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setDelivered(false);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    handler.commit();

    verify(client, never()).commit();
  }

  @Test
  void rollback_invokesRollbackWhenDelivered() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setDelivered(true);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.rollback()).thenReturn(new UpdateResponse());
    handler.setSolrClientForTests(client);

    handler.rollback();

    verify(client).rollback();
    verify(client).close();
    assertNull(handler.getServerConfigForTests());
  }

  @Test
  void rollback_skipsWhenNothingDelivered() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setDelivered(false);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    handler.rollback();

    verify(client, never()).rollback();
  }

  @Test
  void rollback_nullSafeWhenConfigCleared() {
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", null);
    // Must not NPE when serverConfig is null (disabled / after commit cleanup)
    handler.rollback();
  }

  @Test
  void deleteAll_invokesDeleteByQueryWhenCleanAllOnFullPublish() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setCleanAllOnFullPublish(true);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.deleteByQuery(eq("*:*"))).thenReturn(new UpdateResponse());
    handler.setSolrClientForTests(client);

    handler.deleteAllSolrEntries();

    verify(client).deleteByQuery("*:*");
    assertTrue(config.isDelivered());
  }

  @Test
  void deleteAll_skipsWhenCleanAllDisabled() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setCleanAllOnFullPublish(false);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    handler.deleteAllSolrEntries();

    verify(client, never()).deleteByQuery(any(String.class));
  }

  @Test
  void sendMetadata_page_addsDocumentAndExtractRequest() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.add(any(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    when(client.request(any(SolrRequest.class))).thenReturn(new NamedList<>());
    handler.setSolrClientForTests(client);

    PSMetadataEntry entry = pageEntry();
    entry.addProperty(new PSMetadataProperty("dcterms:title", "Hello"));
    tempFile = writeTempBody("page body");

    handler.sendMetadataToSolr("/siteA/folder/page.html", entry, tempFile);

    ArgumentCaptor<SolrInputDocument> docCaptor = ArgumentCaptor.forClass(SolrInputDocument.class);
    verify(client).add(docCaptor.capture());
    SolrInputDocument doc = docCaptor.getValue();
    assertEquals("/siteA/folder/page.html", doc.getFieldValue("id"));
    assertEquals("page.html", doc.getFieldValue("name"));
    assertEquals("page", doc.getFieldValue("type"));
    assertEquals("Hello", doc.getFieldValue("dcterms:title"));

    ArgumentCaptor<SolrRequest> reqCaptor = ArgumentCaptor.forClass(SolrRequest.class);
    verify(client).request(reqCaptor.capture());
    assertInstanceOf(ContentStreamUpdateRequest.class, reqCaptor.getValue());
    ContentStreamUpdateRequest extract = (ContentStreamUpdateRequest) reqCaptor.getValue();
    assertEquals("/update/extract", extract.getPath());
    assertEquals("/siteA/folder/page.html", extract.getParams().get("literal.id"));
    assertEquals("Hello", extract.getParams().get("literal.dcterms:title"));
    assertTrue(config.isDelivered());
  }

  @Test
  void sendMetadata_page_appliesMetaMapping() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.addMetaMapEntry("srcField", "dstField");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.add(any(SolrInputDocument.class))).thenReturn(new UpdateResponse());
    when(client.request(any(SolrRequest.class))).thenReturn(new NamedList<>());
    handler.setSolrClientForTests(client);

    PSMetadataEntry entry = pageEntry();
    entry.addProperty(new PSMetadataProperty("srcField", "mapped-value"));
    tempFile = writeTempBody("x");

    handler.sendMetadataToSolr("/siteA/folder/page.html", entry, tempFile);

    ArgumentCaptor<SolrInputDocument> docCaptor = ArgumentCaptor.forClass(SolrInputDocument.class);
    verify(client).add(docCaptor.capture());
    assertEquals("mapped-value", docCaptor.getValue().getFieldValue("dstField"));
    assertNull(docCaptor.getValue().getFieldValue("srcField"));
  }

  @Test
  void sendMetadata_file_extractOnly_noDocumentAdd() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.request(any(SolrRequest.class))).thenReturn(new NamedList<>());
    handler.setSolrClientForTests(client);

    PSMetadataEntry entry =
        new PSMetadataEntry("asset.pdf", "/folder/", "/siteA/folder/asset.pdf", "file", "siteA");
    entry.setLinktext("Asset");
    entry.addProperty(new PSMetadataProperty("dcterms:format", "application/pdf"));
    tempFile = writeTempBody("%PDF-fake");

    handler.sendMetadataToSolr("/siteA/folder/asset.pdf", entry, tempFile);

    verify(client, never()).add(any(SolrInputDocument.class));
    ArgumentCaptor<SolrRequest> reqCaptor = ArgumentCaptor.forClass(SolrRequest.class);
    verify(client).request(reqCaptor.capture());
    ContentStreamUpdateRequest extract = (ContentStreamUpdateRequest) reqCaptor.getValue();
    assertEquals("/update/extract", extract.getPath());
    assertEquals("/siteA/folder/asset.pdf", extract.getParams().get("literal.id"));
    assertEquals("application/pdf", extract.getParams().get("literal.dcterms:format"));
  }

  @Test
  void sendMetadata_propagatesExtractFailureAsDeliveryException() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);
    SolrClient client = mock(SolrClient.class);
    when(client.request(any(SolrRequest.class))).thenThrow(new SolrServerException("extract fail"));
    handler.setSolrClientForTests(client);

    PSMetadataEntry entry =
        new PSMetadataEntry("asset.pdf", "/folder/", "/siteA/folder/asset.pdf", "file", "siteA");
    entry.setLinktext("Asset");
    tempFile = writeTempBody("x");

    PSDeliveryException ex =
        assertThrows(
            PSDeliveryException.class,
            () -> handler.sendMetadataToSolr("/siteA/folder/asset.pdf", entry, tempFile));
    assertSame(DeliveryErrorCodes.SOLR_COMMUNICATION_EXCEPTION, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void getClient_standaloneBranch_usesHttpJdkSolrClient() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setSolrHost("http://localhost:8983/solr");
    config.setServerCloudType(false);
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", config);

    openClient = handler.getClient();
    assertInstanceOf(HttpJdkSolrClient.class, openClient);
  }

  @Test
  void getClient_cloudBranch_usesCloudSolrClient() throws Exception {
    SolrServer config = standaloneConfig("siteA");
    config.setSolrHost("http://localhost:8983/solr");
    config.setServerCloudType(true);
    config.setDefaultCollection("col1");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "STAGING", config);

    openClient = handler.getClient();
    assertInstanceOf(CloudSolrClient.class, openClient);
  }

  @Test
  void getClient_returnsInjectedMockEvenWhenCloudConfigured() {
    // Update paths never need a real CloudSolrClient when tests inject a mock.
    SolrServer config = standaloneConfig("siteA");
    config.setServerCloudType(true);
    config.setDefaultCollection("col1");
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "STAGING", config);
    SolrClient client = mock(SolrClient.class);
    handler.setSolrClientForTests(client);

    assertEquals(client, handler.getClient());
  }

  @Test
  void getClient_returnsNullWhenDisabled() {
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", null);
    assertNull(handler.getClient());
  }

  private static PSMetadataEntry pageEntry() {
    PSMetadataEntry entry =
        new PSMetadataEntry("page.html", "/folder/", "/siteA/folder/page.html", "page", "siteA");
    entry.setLinktext("Page");
    return entry;
  }

  private static PSPurgableTempFile writeTempBody(String body) throws IOException {
    // Portable temp: PSPurgableTempFile uses java.io.tmpdir / NIO under the hood
    PSPurgableTempFile file = new PSPurgableTempFile("solr-verify-", ".bin", null);
    Files.writeString(file.toPath(), body, StandardCharsets.UTF_8);
    return file;
  }

  private static SolrServer standaloneConfig(String site) {
    SolrServer server = new SolrServer();
    server.setSolrHost("http://localhost:8983/solr");
    server.setServerType("PRODUCTION");
    server.setServerCloudType(false);
    server.addSiteEntry(site);
    server.addMetaMapEntry("src", "dst");
    return server;
  }
}
