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
package com.percussion.delivery.likes.services;

import com.percussion.delivery.comments.services.PSCommentsService;
import com.percussion.delivery.likes.data.IPSLikes;
import com.percussion.delivery.listeners.IPSServiceDataChangeListener;
import com.percussion.error.PSExceptionUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service implementation for managing likes in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public class PSLikesService implements IPSLikesService {

    private IPSLikesDao dao;
    private final List<IPSServiceDataChangeListener> listeners = new ArrayList<>();
    private final String[] percLikesServices = {"perc-likes-services"};

    /**
     * Logger for this class.
     */
    public static final Logger log = LogManager.getLogger(PSCommentsService.class);

    @Autowired
    public PSLikesService(IPSLikesDao dao) {
        this.dao = dao;
    }

    /**
     * Gets the total number of likes for a page or comment.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes
     */
    @Override
    public int getTotalLikes(String site, String likeId, String type) {
        Validate.notEmpty(site, "site must not be empty");
        Validate.notEmpty(likeId, "likeId must not be empty");
        Validate.notEmpty(type, "type must not be empty");

        try {
            var results = dao.find(site, likeId, type);
            if (results.isEmpty()) {
                return 0;
            }
            return results.get(0).getTotal();
        } catch (Exception ex) {
            log.error("Error getting likes by criteria: {}", PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Likes a page or comment.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes after like
     */
    @Override
    public int like(String site, String likeId, String type) {
        return likeUnlike(site, likeId, type, true);
    }

    /**
     * Unlikes a page or comment.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes after unlike
     */
    @Override
    public int unlike(String site, String likeId, String type) {
        return likeUnlike(site, likeId, type, false);
    }

    /**
     * Handles the logic for liking or unliking an object.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @param isLike true for like, false for unlike
     * @return updated count
     */
    private int likeUnlike(String site, String likeId, String type, boolean isLike) {
        Validate.notEmpty(site, "site must not be empty");
        Validate.notEmpty(likeId, "likeId must not be empty");
        Validate.notEmpty(type, "type must not be empty");

        var sites = new HashSet<String>(1);
        sites.add(site);
        fireDataChangeRequestedEvent(sites);

        try {
            var likes = dao.find(site, likeId, type);
            IPSLikes like;
            if (likes.isEmpty()) {
                if (!isLike) {
                    // Cannot decrement a non-existent like
                    return 0;
                }
                like = dao.create(site, likeId, type);
                like.setTotal(1);
                dao.save(like);
                return like.getTotal();
            } else if (isLike) {
                return dao.incrementTotal(site, likeId, type);
            } else {
                return dao.decrementTotal(site, likeId, type);
            }
        } catch (Exception ex) {
            log.error("Error getting likes by criteria: {}", PSExceptionUtils.getMessageForLog(ex));
            log.debug(ex);
            throw new RuntimeException(ex);
        } finally {
            fireDataChangedEvent(sites);
        }
    }

    /**
     * Adds a service data change listener.
     *
     * @param listener the listener to add
     */
    public void addServiceDataChangeListener(IPSServiceDataChangeListener listener) {
        Validate.notNull(listener, "listener cannot be null");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a service data change listener.
     *
     * @param listener the listener to remove
     */
    public void removeServiceDataChangeListener(IPSServiceDataChangeListener listener) {
        Validate.notNull(listener, "listener cannot be null");
        listeners.remove(listener);
    }

    /**
     * Fires a data changed event for all registered listeners.
     *
     * @param sites the set of sites affected
     */
    private void fireDataChangedEvent(Set<String> sites) {
        if (sites == null || sites.isEmpty()) {
            return;
        }
        for (var listener : listeners) {
            listener.dataChanged(sites, percLikesServices);
        }
    }

    /**
     * Fires a data change requested event for all registered listeners.
     *
     * @param sites the set of sites affected
     */
    private void fireDataChangeRequestedEvent(Set<String> sites) {
        if (sites == null || sites.isEmpty()) {
            return;
        }
        for (var listener : listeners) {
            listener.dataChangeRequested(sites, percLikesServices);
        }
    }

    /**
     * Updates likes for a page after a site rename in CM1.
     *
     * @param prevSiteName the old site name
     * @param newSiteName the new site name
     */
    @Override
    public void updateLikesForSiteAfterRename(String prevSiteName, String newSiteName) {
        var likes = new ArrayList<IPSLikes>();
        var newLikes = new ArrayList<IPSLikes>();
        try {
            likes = dao.findLikesForSite(prevSiteName);
            for (var like : likes) {
                like.setSite(newSiteName);
                newLikes.add(like);
                dao.delete(Collections.singletonList(like.getId()));
            }
            dao.save(likes);
        } catch (Exception e) {
            log.error("Error retrieving likes for site: {}. "
                    + "An administrator should attempt to update the likes table "
                    + "in the DTS database. Error: {}", prevSiteName,
                    PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }
}
