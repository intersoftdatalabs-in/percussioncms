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
package com.percussion.delivery.multitenant;

import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Sunny Sal says: "ThreadLocal ka test, concurrency ka best!"
 */
public class ThreadLocalTenantContextTest {

    private static final Logger log = LogManager.getLogger(ThreadLocalTenantContextTest.class);

    @Test
    public void testMultipleThreads() {
        var runners = Collections.synchronizedList(new ArrayList<ThreadLocalRunner>());
        for (int i = 0; i < 10; i++) {
            var runner = new ThreadLocalRunner(i + 1, "key_" + (i + 1));
            runners.add(runner);
            runner.start();
            try {
                Thread.sleep(3);
            } catch (InterruptedException e) {
                log.error(PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                Thread.currentThread().interrupt();
            }
        }

        try {
            Thread.sleep(4);
        } catch (InterruptedException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            Thread.currentThread().interrupt();
        }
        runners.forEach(ThreadLocalRunner::deactivate);
    }

    static class ThreadLocalRunner extends Thread {
        private final String key;
        private final int num;
        private volatile boolean active = true;

        public ThreadLocalRunner(int num, String key) {
            this.key = key;
            this.num = num;
        }

        public void deactivate() {
            active = false;
        }

        @Override
        public void run() {
            PSThreadLocalTenantContext.setTenantId(key);
            while (active) {
                try {
                    Thread.sleep(5 * 1000);
                    log.info("Thread #: {} key: {}", num, key);
                } catch (InterruptedException ignore) {
                    log.error(PSExceptionUtils.getMessageForLog(ignore));
                    log.debug(ignore);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
