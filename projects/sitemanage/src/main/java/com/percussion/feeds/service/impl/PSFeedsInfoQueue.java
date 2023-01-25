/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * The feed info queue is a persistent queue that sends feed descriptors to the feed service in
 * the delivery tier. The queue processor runs in a separate thread.
 * @author erikserating
 *
 */
@Deprecated //TODO: Refactor the feeds nonsense.  It should just be publishing feeds at publish time - not queuing and using a background thread.
public class PSFeedsInfoQueue implements InitializingBean
{
    /**
     * The metadata service, initialized in the ctor, never <code>null</code>
     * after that.
     */
    private final IPSMetadataService metadataService;

    /**
     * The delivery info service, initialized in the ctor, never <code>null</code>
     * after that.
     */
    private final IPSDeliveryInfoService deliveryInfoService;

    /**
     * Logger for this service.
     */
    public static final Logger log = LogManager.getLogger(PSFeedsInfoQueue.class);

    @Autowired
    public PSFeedsInfoQueue(IPSMetadataService metadataService, IPSDeliveryInfoService deliveryInfoService)
    {
        this.metadataService = metadataService;
        this.deliveryInfoService = deliveryInfoService;
    }

    /**
     * Adds the descriptors for the specified site to the queue. Will overwrite any descriptors
     * that already exist for this site in the queue.
     * @param site the sitename, cannot be <code>null</code> or empty.
     * @param descriptors the descriptors json object string, cannot be <code>null</code>
     * or empty.
     */
    public void queueDescriptors(String site, String descriptors, String serverType) throws IPSGenericDao.LoadException, IPSGenericDao.SaveException {
        if(StringUtils.isBlank(site)) {
            throw new IllegalArgumentException("site cannot be null or empty.");
        }
        if(StringUtils.isBlank(descriptors)) {
            throw new IllegalArgumentException("descriptors cannot be null or empty.");
        }
        if(serverType.equalsIgnoreCase("STAGING")){
            PSMetadata data = new PSMetadata(META_KEY_STAGING_PREFIX + site, descriptors);
            metadataService.save(data);
        }
        else
        {
            PSMetadata data = new PSMetadata(META_KEY_PREFIX + site, descriptors);
            metadataService.save(data);
        }

    }


    @Override
    public void afterPropertiesSet() throws Exception {

        QueueProcessor processor = new QueueProcessor();
        processor.start();
    }

    /**
     * Queue processor responsible for pulling items off the queue and sending
     * descriptors up the feed service. The queue will retry sending until all descriptors
     * are sent.
     * @author erikserating
     *
     */
    class QueueProcessor extends Thread
    {

        public QueueProcessor(){
            super();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {

            this.setName("PSFeedsInfoQueueRunner");
            PSDeliveryInfo prodService =  deliveryInfoService.findByService("perc-metadata-services","PRODUCTION");
            PSDeliveryInfo stagService =  deliveryInfoService.findByService("perc-metadata-services","STAGING");

            if(prodService == null)
            {
                log.error("No service entry found for: perc-metadata-services in delivery-servers.xml");
                return;
            }

            log.info("Starting feed info queue.");
            try
            {
                while (true)// Main process loop that never ends
                {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }

                    Collection<PSMetadata> prodResults = metadataService.findByPrefix(META_KEY_PREFIX);
                    Collection<PSMetadata> stagResults = metadataService.findByPrefix(META_KEY_STAGING_PREFIX);

                    if (!prodResults.isEmpty()){
                        if(checkForData(prodResults)){
                            sendDescriptors(prodService, prodResults);
                        }}
                    if (!stagResults.isEmpty()){
                        if(checkForData(stagResults)){
                            sendDescriptors(stagService, stagResults);
                        }}


                    //Increased time - TODO: Re-architect this service
                    Thread.sleep(300000);
                }

            } catch (InterruptedException | IPSGenericDao.LoadException ignore){
                Thread.currentThread().interrupt();
            }
            finally
            {
                log.info("Feed queue shutdown. interrupted="+Thread.currentThread().isInterrupted());
            }

        }

        /***
         * Validate that there are actually descriptors to publish.
         * @param prodResults
         * @return true if there are descriptors false if not.
         */
        private boolean checkForData(Collection<PSMetadata> prodResults) {

            for(PSMetadata p : prodResults){
                JSONArray json;
                try {
                    json = new JSONObject(p.getData()).getJSONArray("descriptors");
                } catch (JSONException e) {
                    log.error("Error parsing FeedDescriptors from Metadata store. Stopping Feed Publish",e);
                    return false;
                }
                if(json.length()>0) {
                    return true;
                }
            }
            return false;

        }

        private void sendDescriptors(PSDeliveryInfo deliveryInfo, Collection<PSMetadata> results ) throws InterruptedException
        {
            for (PSMetadata data : results)
            {
                String key = data.getKey();
                String val = data.getData();
                try
                {
                    JSONObject descriptors = new JSONObject(val);
                    //Add connection info
                    descriptors.put("serviceUrl", deliveryInfo.getUrl());
                    descriptors.put("serviceUser", deliveryInfo.getUsername());
                    descriptors.put("servicePass", deliveryInfo.getPassword());
                    descriptors.put("servicePassEncrypted", false);

                    String sitename = key.substring(META_KEY_STAGING_PREFIX.length());
                    boolean success = sendDescriptors(deliveryInfo, sitename, descriptors.toString());
                    if (success)
                    {
                        // dequeue entry
                        metadataService.delete(key);
                    }
                    Thread.sleep(1000); //Space out sends by 1 second
                }
                catch (InterruptedException e)
                {
                    throw e;
                }
                catch (Exception e)
                {

                    log.error("Feed service error", e);

                }
            }
        }

        /**
         * Sends descriptors to the feed service by using a put request.
         * @param serviceInfo assumed not <code>null</code>.
         * @param site assumed not <code>null</code> or empty.
         * @param descriptors assumed not <code>null</code> or empty.
         * @return <code>true</code> if successful.
         */
        private boolean sendDescriptors(PSDeliveryInfo serviceInfo, String site, String descriptors)
        {
            PSDeliveryInfo server = deliveryInfoService.findByService(PSDeliveryInfo.SERVICE_FEEDS, serviceInfo.getServerType(),serviceInfo.getAdminUrl());
            PSDeliveryClient deliveryClient = new PSDeliveryClient();

            try
            {
                Set<Integer> successfullHttpStatusCodes = new HashSet<>();
                successfullHttpStatusCodes.add(204);
                deliveryClient.push(
                        new PSDeliveryActionOptions()
                                .setActionUrl("/feeds/rss/descriptors")
                                .setDeliveryInfo(server)
                                .setHttpMethod(HttpMethodType.PUT)
                                .setSuccessfullHttpStatusCodes(successfullHttpStatusCodes )
                                .setAdminOperation(true),
                        descriptors);

                return true;

            }
            catch(Exception ex)
            {
                return false;
            }
        }
    }

    /**
     * Constant for the the feeds metadata service key prefix.
     */
    public static final String META_KEY_PREFIX = "PSFeedsInfoQueue.";

    public static final String META_KEY_STAGING_PREFIX = "Staging.";
}
