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

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.extractor.data.PSMetadataProperty;
import com.percussion.rx.delivery.IPSDeliveryErrors;
import com.percussion.rx.delivery.PSDeliveryException;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.util.PSPurgableTempFile;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;

/**
 * Publishes CMS metadata to an external Solr instance during delivery/publish.
 *
 * <p>SolrJ 10 cutover (#1997 / parent #1788):
 *
 * <ul>
 *   <li>Standalone clients use {@link HttpJdkSolrClient} (JDK {@code java.net.http}). The
 *       Apache-HttpClient {@code HttpSolrClient} was removed in SolrJ 10.
 *   <li>Single-file {@code /update/extract} posts go through {@link ContentStreamUpdateRequest}'s
 *       content writer (not multi-stream multipart). The removed {@code
 *       HttpSolrClient#setUseMultiPartPost(true)} has no equivalent; multi-stream multipart would
 *       require {@code solr-solrj-jetty} ({@code HttpJettySolrClient}) — deferred to packaging
 *       slice #1998. Product extract sends one file stream per request.
 *   <li>Cloud path uses {@link CloudSolrClient.Builder#Builder(List)} with <strong>Solr base
 *       URLs</strong> (must end in {@code /solr}), not ZooKeeper hosts. Product POM excludes {@code
 *       solr-solrj-zookeeper} and enforcer-bans ZK (#1673); CloudSolrClient falls back to JDK HTTP
 *       when Jetty client is absent. Prefer {@code withDefaultCollection} on the builder.
 * </ul>
 */
public class PSSolrDeliveryHandler {
  private static final String ENABLE_CLIENT_SASL_KEY = "zookeeper.sasl.client";

  private static final String LOGIN_CONTEXT_NAME_KEY = "zookeeper.sasl.clientconfig";

  private static String saslConfigName = null;

  /** Logger for this class */
  public static final Logger log = LogManager.getLogger(PSSolrDeliveryHandler.class);

  private boolean fatalError = false;

  private SolrServer serverConfig = null;

  private String serverType;

  private String siteName;

  private SolrClient solrClient = null;

  public PSSolrDeliveryHandler(String siteName, String serverType, boolean forceSolrClean)
      throws PSDeliveryException {
    // Without this zookeeper looks for sasl config in
    // TODO: Where is this config pulled from under jetty?
    // Default logging context name is "Client" but can be changed with server
    // properties "zookeeper.sasl.clientconfig"
    // If security is required we should use other mechanism. Use of server
    // property to set client config may make
    // having multiple configurations not be thread safe, so we synchronize
    // the access.

    System.setProperty(ENABLE_CLIENT_SASL_KEY, "false");

    this.siteName = siteName;
    this.serverType = serverType;

    PSSolrConfig config = SolrConfigLoader.getDeliveryServerConfig();

    if (config != null && config.getSolrServer() != null) {
      for (SolrServer solrConfig : config.getSolrServer()) {
        if (solrConfig.isEnabledSite(siteName)
            && (solrConfig.getServerType() == null && serverType.equalsIgnoreCase("PRODUCTION")
                || (solrConfig.getServerType() != null
                    && solrConfig.getServerType().equalsIgnoreCase(serverType)))) {
          serverConfig = solrConfig;
          break;
        }
      }
      if (serverConfig != null && forceSolrClean) deleteAllSolrEntries();
    }
  }

  /**
   * Test-only constructor that skips {@link SolrConfigLoader} and does not contact Solr.
   *
   * @param siteName site name for enabled-site checks
   * @param serverType PRODUCTION / STAGING / etc.
   * @param serverConfig pre-built config; may be {@code null} for disabled handler tests
   */
  PSSolrDeliveryHandler(String siteName, String serverType, SolrServer serverConfig) {
    System.setProperty(ENABLE_CLIENT_SASL_KEY, "false");
    this.siteName = siteName;
    this.serverType = serverType;
    this.serverConfig = serverConfig;
  }

  private void deleteAllSolrEntries() throws PSDeliveryException {
    if (!isEnabled()) return;

    log.debug("Deleting existing metadata entries");

    if (!serverConfig.isCleanAllOnFullPublish()) return;

    synchronized (this) {
      SolrClient solrClient = getClient();
      try {
        if (solrClient != null) {
          solrClient.deleteByQuery("*:*");
          serverConfig.setDelivered(true);
        }
      } catch (SolrException | SolrServerException | IOException e) {
        rollback();
        throw new PSDeliveryException(
            IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, e, PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  public void sendMetadataToSolr(
      String path, IPSMetadataEntry entry, PSPurgableTempFile psPurgableTempFile)
      throws PSDeliveryException {
    if (!isEnabled()) return;

    if (!serverConfig.isActive())
      throw new PSDeliveryException(
          IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION,
          null,
          "Skipped due to previous fatal error or max Solr errors reached");

    synchronized (this) {
      SolrClient client = getClient();

      if (entry.getType() != null && entry.getType().equals("page"))
        sendMetadata(path, serverConfig, entry, client, psPurgableTempFile);
      else {
        if (client != null) {
          sendFile(path, serverConfig, client, transform(serverConfig, entry), psPurgableTempFile);
        }
      }
    }
    if (!serverConfig.isDelivered()) serverConfig.setDelivered(true);
  }

  private void sendMetadata(
      String path,
      SolrServer solrConfig,
      IPSMetadataEntry entry,
      SolrClient client,
      PSPurgableTempFile psPurgableTempFile)
      throws PSDeliveryException {
    if (solrConfig.isEnabledSite(siteName)) {

      boolean success =
          sendMetadata(path, solrConfig, client, transform(solrConfig, entry), psPurgableTempFile);
      solrConfig.setDelivered(true);
      if (!solrConfig.isDelivered()) solrConfig.setDelivered(success);
    }
  }

  private void sendFile(
      String path,
      SolrServer solrConfig,
      SolrClient client,
      Set<IPSMetadataProperty> metaset,
      PSPurgableTempFile psPurgableTempFile)
      throws PSDeliveryException {

    try {

      ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");

      String type = null;
      log.debug("Sending File to Solr");
      req.setParam("literal.id", path);
      log.debug("literal.id: {}", path);
      for (IPSMetadataProperty property : metaset) {
        if (property.getName().equals("dcterms:format")) type = property.getValue();
        req.setParam("literal." + property.getName(), property.getValue());
        log.debug("literal. {}:{}", property.getName(), property.getValue());
      }

      // SolrJ 10: addFile takes Path; null content type is unsafe — default when no dcterms:format
      if (type == null || type.isBlank()) {
        type = "application/octet-stream";
      }
      req.addFile(psPurgableTempFile.toPath(), type);

      NamedList<Object> result;

      result = client.request(req);
      log.info("Solr Result: {}", result);
    } catch (SolrException | SolrServerException | IOException e) {
      solrConfig.incrError();
      throw new PSDeliveryException(
          IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, e, PSExceptionUtils.getMessageForLog(e));
    }
  }

  private boolean sendMetadata(
      String path,
      SolrServer solrConfig,
      SolrClient client,
      Set<IPSMetadataProperty> metaset,
      PSPurgableTempFile psPurgableTempFile)
      throws PSDeliveryException {
    SolrInputDocument doc = new SolrInputDocument();
    log.debug("Sending Page Metadata");
    doc.addField("id", path);
    log.debug("id:{}", path);

    for (IPSMetadataProperty meta : metaset) {
      log.debug("{}:{}", meta.getName(), meta.getValue());
      doc.addField(meta.getName(), meta.getValue());
    }

    UpdateResponse result;
    try {
      result = client.add(doc);
      sendFile(path, solrConfig, client, metaset, psPurgableTempFile);
    } catch (SolrException | SolrServerException | IOException e) {
      solrConfig.incrError();
      throw new PSDeliveryException(
          IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, e, PSExceptionUtils.getMessageForLog(e));
    }
    log.debug("Solr Result: {}", result);
    return true;
  }

  public void delete(String path) throws PSDeliveryException {

    if (!isEnabled()) return;

    if (!serverConfig.isActive())
      throw new PSDeliveryException(
          IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, null, "Max Solr Errors Reached");

    synchronized (this) {
      SolrClient client = getClient();

      try {
        if (client != null) {
          client.deleteById(path);
          if (!serverConfig.isDelivered()) serverConfig.setDelivered(true);
        }
      } catch (SolrException | SolrServerException | IOException e) {
        serverConfig.incrError();
        throw new PSDeliveryException(
            IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, e, PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  public void commit() throws PSDeliveryException {
    if (!isEnabled()) return;

    // No items were delivered. Nothing to commit

    if (!serverConfig.isDelivered()) return;

    if (!serverConfig.isActive())
      throw new PSDeliveryException(
          IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, null, "Max Solr Errors Reached");

    synchronized (this) {
      log.info(
          "Committing solr changes for site {} type={} solrUrl={}",
          this.siteName,
          this.serverType,
          serverConfig.getSolrHost());

      try (SolrClient client = getClient()) {
        if (client != null) {
          client.commit();
        }
      } catch (SolrException | SolrServerException | IOException e) {
        throw new PSDeliveryException(
            IPSDeliveryErrors.SOLR_COMMUNICATION_EXCEPTION, e, PSExceptionUtils.getMessageForLog(e));
      } finally {
        // Client was closed by try-with-resources; drop cached reference
        solrClient = null;
        serverConfig = null;
      }
    }
  }

  public void rollback() {

    // No items were delivered. Nothing to commit
    if (serverConfig == null || !serverConfig.isDelivered()) return;

    synchronized (this) {
      if (!serverConfig.isActive()) return;

      log.error(
          "Rolling back solr changes on error for site {} solrUrl={}",
          this.siteName,
          serverConfig.getSolrHost());

      try (SolrClient client = getClient()) {
        if (client != null) {
          client.rollback();
        }
      } catch (SolrException | SolrServerException | IOException e) {
        log.debug("Exception attempting to roll back Solr, continue anyway", e);
      } finally {
        solrClient = null;
        serverConfig = null;
      }
    }
  }

  public boolean isEnabled() {

    return !(fatalError || serverConfig == null);
  }

  private Set<IPSMetadataProperty> transform(SolrServer solrConfig, IPSMetadataEntry entry) {
    Set<IPSMetadataProperty> transformed = new HashSet<>();

    transformed.add(new PSMetadataProperty("name", entry.getName()));
    transformed.add(new PSMetadataProperty("linktext", entry.getLinktext()));
    transformed.add(new PSMetadataProperty("type", entry.getType()));
    transformed.add(new PSMetadataProperty("site", entry.getSite()));
    transformed.add(new PSMetadataProperty("folder", entry.getFolder()));
    transformed.add(new PSMetadataProperty("pagepath", entry.getPagepath()));

    for (IPSMetadataProperty property : entry.getProperties()) {
      if (solrConfig.hasMetaMapping(property.getName())) {
        transformed.add(
            new PSMetadataProperty(solrConfig.getMetaMapping(property.getName()), property.getValue()));
      } else {
        transformed.add(new PSMetadataProperty(property.getName(), property.getValue()));
      }
    }
    return transformed;
  }

  /**
   * Builds a standalone SolrJ 10 client for the given Solr root base URL.
   *
   * <p>Base URL must be a Solr root path ending in {@code /solr} (SolrJ 10 rule). Uses JDK {@link
   * HttpJdkSolrClient}; does not pull Jetty or Apache HttpClient.
   *
   * @param baseUrl solr root URL, e.g. {@code http://host:8983/solr}
   * @return open client; caller must close
   */
  static SolrClient createStandaloneClient(String baseUrl) {
    return new HttpJdkSolrClient.Builder(baseUrl).build();
  }

  /**
   * Builds a SolrCloud client using <strong>Solr base URLs</strong> (not ZooKeeper hosts).
   *
   * <p>Product excludes {@code solr-solrj-zookeeper} (#1673); the list constructor is the Solr-URL
   * form. When Jetty client is not on the classpath, CloudSolrClient uses {@link
   * HttpJdkSolrClient} as the delegate. ZK-host constructor remains unsupported under current
   * packaging.
   *
   * @param solrUrl solr base URL ending in {@code /solr}
   * @param defaultCollection optional default collection (may be null/blank)
   * @return open client; caller must close
   */
  static SolrClient createCloudClient(String solrUrl, String defaultCollection) {
    CloudSolrClient.Builder builder = new CloudSolrClient.Builder(List.of(solrUrl));
    if (defaultCollection != null && !defaultCollection.isBlank()) {
      builder.withDefaultCollection(defaultCollection);
    }
    return builder.build();
  }

  /** Package-private: inject a mock {@link SolrClient} for unit tests (no network). */
  void setSolrClientForTests(SolrClient client) {
    this.solrClient = client;
  }

  /** Package-private accessor for unit tests. */
  SolrServer getServerConfigForTests() {
    return serverConfig;
  }

  /**
   * Returns the cached Solr client, constructing it on first use for the configured server type.
   * Package-visible for unit tests (standalone vs cloud construction without network I/O until
   * first request).
   */
  synchronized SolrClient getClient() {
    if (!isEnabled()) return null;

    if (!serverConfig.isActive()) return null;

    boolean isCloudServer = serverConfig.isServerCloudType();

    // Depend upon server type Client object would be returned
    if (isCloudServer) {

      if (serverConfig.getSaslContextName() != null) {
        System.setProperty(ENABLE_CLIENT_SASL_KEY, "true");
        System.setProperty(LOGIN_CONTEXT_NAME_KEY, serverConfig.getSaslContextName());
      } else {
        if (saslConfigName != null) {
          System.setProperty(ENABLE_CLIENT_SASL_KEY, "false");
          saslConfigName = null;
        }
      }
      if (solrClient == null) {
        // Must close cloudClient in commit or rollback.
        // SolrJ 10: Builder(List) = Solr URLs; withDefaultCollection replaces setDefaultCollection.
        solrClient =
            createCloudClient(serverConfig.getSolrHost(), serverConfig.getDefaultCollection());
      }
    } else {

      if (solrClient == null) {
        // SolrJ 10: HttpJdkSolrClient replaces removed HttpSolrClient.
        // Multipart multi-stream posts are unsupported on the JDK client; product extract uses a
        // single content stream via ContentStreamUpdateRequest (content writer path).
        solrClient = createStandaloneClient(serverConfig.getSolrHost());
      }
    }

    return solrClient;
  }
}
