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

package com.percussion.delivery.metadata.impl;

import com.percussion.delivery.metadata.IPSBlogPostVisit;
import com.percussion.delivery.metadata.IPSBlogPostVisitDao;
import com.percussion.delivery.metadata.IPSBlogPostVisitService;
import com.percussion.delivery.metadata.IPSCookieConsentService;
import com.percussion.delivery.metadata.data.PSBlogPostVisit;
import com.percussion.delivery.metadata.data.PSCookieConsentQuery;
import com.percussion.delivery.metadata.data.PSVisitQuery;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

@Component
public class PSBlogPostVisitService implements IPSBlogPostVisitService, InitializingBean {
    private final Map<String, IPSBlogPostVisit> inMemoryVisitMap = new ConcurrentHashMap<>();
    private final List<PSCookieConsentQuery> inMemoryCookieConsentMap = new ArrayList<>();
    private final ScheduledExecutorService visitExecutor = Executors.newScheduledThreadPool(1);
    private volatile long lastSave;
    private final IPSBlogPostVisitDao visitDao;
    private final IPSCookieConsentService cookieService;
    private static final Logger log = LogManager.getLogger(PSBlogPostVisitService.class);
    private final Integer schedulerInitialDelay;
    private final Integer schedulerSaveInterval;

    @Autowired
    public PSBlogPostVisitService(
            IPSBlogPostVisitDao visitDao,
            IPSCookieConsentService cookieService,
            Integer schedulerSaveInterval) {
        this.visitDao = visitDao;
        this.cookieService = cookieService;
        this.schedulerInitialDelay = INTIAL_DELAY_SECONDS;
        this.schedulerSaveInterval = schedulerSaveInterval != null ? schedulerSaveInterval : SAVE_INTERVAL_SECONDS;
        log.debug("Save Interval: {}", this.schedulerSaveInterval);
        log.debug("Initial Delay: {}", this.schedulerInitialDelay);
    }

    @Override
    public void afterPropertiesSet() {
        startScheduler();
    }

    @Override
    public void startScheduler() {
        Runnable scheduledTask = () -> {
            lastSave = System.currentTimeMillis();
            saveVisits();
            saveCookieConsentEntries();
        };
        visitExecutor.scheduleAtFixedRate(
                scheduledTask,
                schedulerInitialDelay,
                schedulerSaveInterval,
                TimeUnit.SECONDS);
    }

    private void saveVisits() {
        try {
            if (inMemoryVisitMap.isEmpty()) {
                return;
            }
            var visits = new ArrayList<>(inMemoryVisitMap.values());
            inMemoryVisitMap.clear();
            log.debug("Saving visits");
            log.debug("Visits size: {}", visits.size());
            visitDao.save(visits);
        } catch (Exception e) {
            log.error("Failed to save hit counts, Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    private void saveCookieConsentEntries() {
        try {
            if (inMemoryCookieConsentMap.isEmpty()) {
                return;
            }
            cookieService.save(inMemoryCookieConsentMap);
            inMemoryCookieConsentMap.clear();
        } catch (Exception e) {
            log.error("Error saving cookie consent entries. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @Override
    public List<String> getTopVisitedBlogPosts(PSVisitQuery visitQuery) throws Exception {
        var tp = TIMEPERIOD.fromName(visitQuery.getTimePeriod());
        if (tp == null) {
            tp = TIMEPERIOD.WEEK;
        }
        return visitDao.getTopVisitedPages(
                visitQuery.getSectionPath(),
                tp.getDays(),
                convertToLimit(visitQuery.getLimit()),
                visitQuery.getSortOrder());
    }

    @Override
    public void trackBlogPost(String pagePath) {
        inMemoryVisitMap.compute(pagePath, (key, visit) -> {
            if (visit == null) {
                return new PSBlogPostVisit(pagePath, new Date(), BigInteger.ONE);
            } else {
                visit.setHitCount(visit.getHitCount().add(BigInteger.ONE));
                return visit;
            }
        });
    }

    @Override
    public void logCookieConsentEntry(PSCookieConsentQuery query) {
        inMemoryCookieConsentMap.add(query);
    }

    @Override
    public void delete(Collection<String> pagepaths) {
        visitDao.delete(pagepaths);
    }

    public int convertToLimit(String limit) {
        if (StringUtils.isBlank(limit)) {
            return 0;
        }
        var normalizedLimit = limit.toUpperCase().replace("R-", "");
        int res = 5;
        try {
            res = Integer.parseInt(normalizedLimit);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse the limit parameter, defaulting to 5");
        }
        return res;
    }

    @Override
    public boolean visitSchedulerStatus() {
        // Returns true if the scheduler is running within expected interval.
        return (System.currentTimeMillis() - lastSave) < (2L * schedulerSaveInterval * 1000);
    }

    @PreDestroy
    public void beandestroy() {
        log.debug("Calling most-read-blog-posts thread shutdown.");
        visitExecutor.shutdown();
        try {
            log.debug("Calling most-read-blog-posts thread await termination.");
            visitExecutor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.debug("Interrupting most-read-blog-posts thread.");
            Thread.currentThread().interrupt();
        } finally {
            if (!visitExecutor.isShutdown()) {
                log.debug("Calling shutdownNow for most-read-blog-posts thread.");
                visitExecutor.shutdownNow();
            }
        }
    }

    @Override
    public void updatePostsAfterSiteRename(String prevSiteName, String newSiteName) {
        try {
            visitDao.updatePostsAfterSiteRename(prevSiteName, newSiteName);
        } catch (Exception e) {
            log.error("Error updating blog post visit updates after site rename. Error: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }
}
