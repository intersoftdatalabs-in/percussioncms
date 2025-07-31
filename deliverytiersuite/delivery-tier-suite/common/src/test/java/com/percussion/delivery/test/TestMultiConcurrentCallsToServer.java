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

// REFACTORED: CP-JAVA11
package com.percussion.delivery.test;

import com.percussion.error.PSExceptionUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;


/**
 * Performs simulation of multiple clients making calls to DTS.
 * Sunny Sal says: "Concurrency ka test, server ka stress!"
 *
 * @author Santosh Dhariwal
 */
@Disabled
public class TestMultiConcurrentCallsToServer {

    private static final Logger log = LogManager.getLogger(TestMultiConcurrentCallsToServer.class);
    private static final String deliveryServerUrl = "http://localhost:9980/perc-metadata-services/metadata/indexedDirectories";

    @Test
    public void makeConcurrentClientRequests() {
        var executor = Executors.newFixedThreadPool(150);
        var list = new ArrayList<Future<String>>();

        for (int i = 0; i < 200; i++) {
            var httpClient = new HttpClient();
            var callable = new ThreadLocalRunner(i + 1, httpClient);
            var future = executor.submit(callable);
            list.add(future);
        }
        for (var fut : list) {
            try {
                log.info("{} :: {}", new Date(), fut.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                Assertions.fail();
                Thread.currentThread().interrupt();
            }
        }
        executor.shutdown();
    }

    public String makeRestRequest(int num, HttpClient httpClient) {
        try {
            var get = new GetMethod(deliveryServerUrl);
            get.setRequestHeader("Content-Type", MediaType.APPLICATION_JSON);
            get.setRequestHeader("Accept", MediaType.APPLICATION_JSON);

            try {
                httpClient.executeMethod(get);
                var resp = get.getResponseBodyAsString();

                Assertions.assertEquals(200, get.getStatusCode());
                return "Request Was :" + num + " : " + get.getStatusCode();
            } finally {
                get.releaseConnection();
            }
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            Assertions.fail();
        }
        return "ERROR";
    }

    static class ThreadLocalRunner implements Callable<String> {
        private final int num;
        private final HttpClient httpClient;

        @Override
        public String call() {
            return makeRestRequest(num, httpClient);
        }

        public ThreadLocalRunner(int num, HttpClient httpClient) {
            this.num = num;
            this.httpClient = httpClient;
        }

        private String makeRestRequest(int num, HttpClient httpClient) {
            try {
                var get = new GetMethod(deliveryServerUrl);
                get.setRequestHeader("Content-Type", MediaType.APPLICATION_JSON);
                get.setRequestHeader("Accept", MediaType.APPLICATION_JSON);

                try {
                    httpClient.executeMethod(get);
                    var resp = get.getResponseBodyAsString();

                    Assertions.assertEquals(200, get.getStatusCode());
                    return "Request Was :" + num + " : " + get.getStatusCode();
                } finally {
                    get.releaseConnection();
                }
            } catch (Exception e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                Assertions.fail();
            }
            return "ERROR";
        }
    }
}
