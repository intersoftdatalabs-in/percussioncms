// REFACTORED: CP-JAVA11
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

package com.percussion.utils;

import com.percussion.delivery.client.IPSDeliveryClient.HttpMethodType;
import com.percussion.delivery.client.IPSDeliveryClient.PSDeliveryActionOptions;
import com.percussion.delivery.client.PSDeliveryClient;
import com.percussion.delivery.data.PSDeliveryInfo;
import com.percussion.delivery.service.IPSDeliveryInfoService;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.integritymanagement.data.PSIntegrityTask.TaskStatus;
import com.percussion.utils.types.PSPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks and reports on the health status of the DTS and all of its services.
 * <p>
 * Sunny Sal says: "If your DTS is down, don't panic—just check the logs and grab a chai!"
 * </p>
 */
@Component("dtsStatusProvider")
public class PSDTSStatusProvider implements IPSDTSStatusProvider {

    private final String serverRoot;
    private final Map<String, String> externalServices = Map.of(
            "perc-polls-services", "/perc-polls-services/polls/version"
    );
    private final Map<String, String> services = Map.of(
            PSDeliveryInfo.SERVICE_FEEDS, "/feeds/rss/version",
            PSDeliveryInfo.SERVICE_INDEXER, "/perc-metadata-services/metadata/version",
            PSDeliveryInfo.SERVICE_COMMENTS, "/perc-comments-services/comment/version",
            PSDeliveryInfo.SERVICE_FORMS, "perc-form-processor/form/version",
            PSDeliveryInfo.SERVICE_MEMBERSHIP, "/perc-membership-services/membership/version"
    );

    private final IPSDeliveryInfoService deliveryService;
    private final PSDeliveryClient deliveryClient;

    /**
     * Default constructor with dependency injection.
     */
    @Autowired
    public PSDTSStatusProvider(IPSDeliveryInfoService service) {
        this.deliveryService = service;
        this.deliveryClient = new PSDeliveryClient();
        this.serverRoot = deliveryService.findBaseByServerType(null);
    }

    /**
     * Returns health status of DTS and all services.
     * If DTS is not running, no services are represented.
     * Services are represented as key-values in a map with a PSPair representing status and response message.
     */
    @Override
    public Map<String, PSPair<TaskStatus, String>> getDTSStatusReport() {
        var statusReport = new HashMap<String, PSPair<TaskStatus, String>>();

        // Check the status of the DTS - if down, return status of DTS only
        var dtsPair = getExternalTomcatServiceStatus(serverRoot);
        if (!dtsPair.getFirst()) {
            statusReport.put("dts", new PSPair<>(TaskStatus.FAILED, dtsPair.getSecond()));
            return statusReport;
        }
        statusReport.put("dts", new PSPair<>(TaskStatus.SUCCESS, dtsPair.getSecond()));

        // Check external services and add their status to the report
        externalServices.forEach((serviceName, path) -> {
            var extStatus = getExternalTomcatServiceStatus(serverRoot + path);
            var status = extStatus.getFirst() ? TaskStatus.SUCCESS : TaskStatus.FAILED;
            statusReport.put(serviceName, new PSPair<>(status, extStatus.getSecond()));
        });

        // Check internal services
        services.forEach((serviceName, path) -> {
            var pair = getServiceStatus(serviceName, path);
            statusReport.put(serviceName, pair);
        });

        return statusReport;
    }

    /**
     * Gets the status of the specified service.
     *
     * @param service    Name of service
     * @param serviceURL URL from service to /version (ping URL)
     * @return PSPair where first represents status and second is response message
     */
    private PSPair<TaskStatus, String> getServiceStatus(String service, String serviceURL) {
        try {
            var server = deliveryService.findByService(service);
            var message = deliveryClient.getString(new PSDeliveryActionOptions(server, serviceURL, HttpMethodType.GET, false));
            return new PSPair<>(TaskStatus.SUCCESS, message);
        } catch (RuntimeException e) {
            return new PSPair<>(TaskStatus.FAILED, PSExceptionUtils.getMessageForLog(e));
        }
    }

    /**
     * Gets the status of the external service such as polls or just root Tomcat.
     *
     * @param surl URL to check
     * @return PSPair&lt;Status, Message&gt;
     */
    private PSPair<Boolean, String> getExternalTomcatServiceStatus(String surl) {
        try {
            var url = new URL(surl);
            String response;
            boolean alive = false;
            if ("https".equalsIgnoreCase(url.getProtocol())) {
                var conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "*/*");
                response = conn.getResponseMessage();
                alive = response != null && response.contains("OK");
            } else {
                var conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "*/*");
                response = conn.getResponseMessage();
                alive = response != null && response.contains("OK");
            }
            return new PSPair<>(alive, response);
        } catch (ConnectException e) {
            return new PSPair<>(false, PSExceptionUtils.getMessageForLog(e));
        } catch (IOException e) {
            return new PSPair<>(false, PSExceptionUtils.getMessageForLog(e));
        }
    }
}
