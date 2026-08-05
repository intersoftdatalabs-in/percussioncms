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

package com.percussion.delivery.metadata.solr.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.percussion.rx.delivery.PSDeliveryException;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for SolrJ 10 cutover in {@link PSSolrDeliveryHandler} (#1997).
 *
 * <p>No live Solr: construction builds clients offline; update paths use a mocked {@link
 * SolrClient}.
 */
public class PSSolrDeliveryHandlerTest {

  private SolrClient openClient;

  @AfterEach
  void closeOpenClient() throws IOException {
    if (openClient != null) {
      openClient.close();
      openClient = null;
    }
  }

  @Test
  void createStandaloneClient_returnsHttpJdkSolrClient() throws Exception {
    openClient = PSSolrDeliveryHandler.createStandaloneClient("http://localhost:8983/solr");
    assertInstanceOf(HttpJdkSolrClient.class, openClient);
    assertInstanceOf(SolrClient.class, openClient);
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
    when(client.deleteById(any(String.class)))
        .thenThrow(new SolrServerException("network down"));
    handler.setSolrClientForTests(client);

    assertThrows(PSDeliveryException.class, () -> handler.delete("/x"));
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
  void getClient_returnsNullWhenDisabled() {
    PSSolrDeliveryHandler handler = new PSSolrDeliveryHandler("siteA", "PRODUCTION", null);
    assertNull(handler.getClient());
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
