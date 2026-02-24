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
package com.percussion.feeds.service.impl;

import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.metadata.data.PSMetadata;
import com.percussion.metadata.service.IPSMetadataService;
import com.percussion.share.dao.IPSGenericDao;
import java.util.Collection;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The feed info queue is a persistent queue that sends feed descriptors to the feed service in the
 * delivery tier. The queue processor runs in a separate thread. Sunny Sal says: "FeedsInfoQueue,
 * now Java 11 and Google-styled! But still needs a re-architecture."
 */
@Deprecated // TODO: Refactor the feeds nonsense. It should just be publishing feeds at publish time
// - not queuing and using a background thread.
public class PSFeedsInfoQueue implements InitializingBean {

  private final IPSMetadataService metadataService;
  private final IPSDeliveryInfoService deliveryInfoService;
  public static final Logger log = LogManager.getLogger(PSFeedsInfoQueue.class);

  @Autowired
  public PSFeedsInfoQueue(
      IPSMetadataService metadataService, IPSDeliveryInfoService deliveryInfoService) {
    this.metadataService = metadataService;
    this.deliveryInfoService = deliveryInfoService;
  }

  /**
   * Adds the descriptors for the specified site to the queue. Will overwrite any descriptors that
   * already exist for this site in the queue.
   */
  public void queueDescriptors(String site, String descriptors, String serverType)
      throws IPSGenericDao.LoadException, IPSGenericDao.SaveException {
    if (StringUtils.isBlank(site)) {
      throw new IllegalArgumentException("site cannot be null or empty.");
    }
    if (StringUtils.isBlank(descriptors)) {
      throw new IllegalArgumentException("descriptors cannot be null or empty.");
    }
    var key =
        serverType.equalsIgnoreCase("STAGING")
            ? META_KEY_STAGING_PREFIX + site
            : META_KEY_PREFIX + site;
    var data = new PSMetadata(key, descriptors);
    metadataService.save(data);
  }

  @Override
  public void afterPropertiesSet() {
    var processor = new QueueProcessor();
    processor.start();
  }

  /**
   * Queue processor responsible for pulling items off the queue and sending descriptors up the feed
   * service. The queue will retry sending until all descriptors are sent.
   */
  class QueueProcessor extends Thread {
    public QueueProcessor() {
      super();
    }

    @Override
    public void run() {
      this.setName("PSFeedsInfoQueueRunner");
      var prodService = deliveryInfoService.findByService("perc-metadata-services", "PRODUCTION");
      var stagService = deliveryInfoService.findByService("perc-metadata-services", "STAGING");

      if (prodService == null) {
        log.error("No service entry found for: perc-metadata-services in delivery-servers.xml");
        return;
      }

      log.info("Starting feed info queue.");
      try {
        while (true) {
          if (Thread.currentThread().isInterrupted()) {
            break;
          }
          var prodResults = metadataService.findByPrefix(META_KEY_PREFIX);
          var stagResults = metadataService.findByPrefix(META_KEY_STAGING_PREFIX);

          if (!prodResults.isEmpty() && checkForData(prodResults)) {
            sendDescriptors(prodService, prodResults);
          }
          if (!stagResults.isEmpty() && checkForData(stagResults)) {
            sendDescriptors(stagService, stagResults);
          }
          Thread.sleep(300_000); // 5 minutes
        }
      } catch (InterruptedException | IPSGenericDao.LoadException ignore) {
        Thread.currentThread().interrupt();
      } finally {
        log.info("Feed queue shutdown. interrupted={}", Thread.currentThread().isInterrupted());
      }
    }

    /** Validate that there are actually descriptors to publish. */
    private boolean checkForData(Collection<PSMetadata> results) {
      for (var p : results) {
        try {
          var json = new JSONObject(p.getData()).getJSONArray("descriptors");
          if (json.length() > 0) {
            return true;
          }
        } catch (JSONException e) {
          log.error("Error parsing FeedDescriptors from Metadata store. Stopping Feed Publish", e);
          return false;
        }
      }
      return false;
    }

    private void sendDescriptors(PSDeliveryInfo deliveryInfo, Collection<PSMetadata> results)
        throws InterruptedException {
      for (var data : results) {
        var key = data.getKey();
        var val = data.getData();
        try {
          var descriptors = new JSONObject(val);
          descriptors.put("serviceUrl", deliveryInfo.getUrl());
          descriptors.put("serviceUser", deliveryInfo.getUsername());
          descriptors.put("servicePass", deliveryInfo.getPassword());
          descriptors.put("servicePassEncrypted", false);

          var sitename = key.substring(META_KEY_STAGING_PREFIX.length());
          var success = sendDescriptors(deliveryInfo, sitename, descriptors.toString());
          if (success) {
            metadataService.delete(key);
          }
          Thread.sleep(1_000); // Space out sends by 1 second
        } catch (InterruptedException e) {
          throw e;
        } catch (Exception e) {
          log.error("Feed service error", e);
        }
      }
    }

    /** Sends descriptors to the feed service by using a put request. */
    private boolean sendDescriptors(PSDeliveryInfo serviceInfo, String site, String descriptors) {
      var server =
          deliveryInfoService.findByService(
              PSDeliveryInfo.SERVICE_FEEDS,
              serviceInfo.getServerType(),
              serviceInfo.getAdminUrl().orElse(null));
      var deliveryClient = new PSDeliveryClient();
      try {
        var successCodes = Set.of(204);
        deliveryClient.push(
            new PSDeliveryActionOptions()
                .setActionUrl("/feeds/rss/descriptors")
                .setDeliveryInfo(server)
                .setHttpMethod(HttpMethodType.PUT)
                .setSuccessfullHttpStatusCodes(successCodes)
                .setAdminOperation(true),
            descriptors);
        return true;
      } catch (Exception ex) {
        return false;
      }
    }
  }

  public static final String META_KEY_PREFIX = "PSFeedsInfoQueue.";
  public static final String META_KEY_STAGING_PREFIX = "Staging.";
}
