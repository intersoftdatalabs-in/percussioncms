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

/**
 * Service interface for managing likes in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public interface IPSLikesService {

    /**
     * Returns the total number of likes for the given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes
     */
    int getTotalLikes(String site, String likeId, String type);

    /**
     * Increments the total number of likes for the given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes after increment
     */
    int like(String site, String likeId, String type);

    /**
     * Decrements the total number of likes for the given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return total number of likes after decrement
     */
    int unlike(String site, String likeId, String type);

    /**
     * Updates likes for a page after a site rename in CM1.
     *
     * @param prevSiteName the old site name
     * @param newSiteName the new site name
     */
    void updateLikesForSiteAfterRename(String prevSiteName, String newSiteName);
}
