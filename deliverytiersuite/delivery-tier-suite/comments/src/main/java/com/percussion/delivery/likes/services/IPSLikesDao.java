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

import com.percussion.delivery.likes.data.IPSLikes;
import java.util.Collection;
import java.util.List;

/**
 * DAO interface for likes persistence in Percussion CMS.
 * All methods are thread-safe and follow Google Java Style.
 */
public interface IPSLikesDao {

    /**
     * Finds likes by site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return list of matching likes
     * @throws Exception if query fails
     */
    List<IPSLikes> find(String site, String likeId, String type) throws Exception;

    /**
     * Finds likes for a given site.
     *
     * @param site the site name
     * @return list of likes
     * @throws Exception if query fails
     */
    List<IPSLikes> findLikesForSite(String site) throws Exception;

    /**
     * Deletes likes by their IDs.
     *
     * @param ids collection of like IDs
     * @throws Exception if deletion fails
     */
    void delete(Collection<String> ids) throws Exception;

    /**
     * Saves a single like.
     *
     * @param like the like to save
     * @throws Exception if save fails
     */
    void save(IPSLikes like) throws Exception;

    /**
     * Saves a list of likes.
     *
     * @param likes list of likes to save
     * @throws Exception if save fails
     */
    void save(List<IPSLikes> likes) throws Exception;

    /**
     * Creates a new like.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new like instance
     * @throws Exception if creation fails
     */
    IPSLikes create(String site, String likeId, String type) throws Exception;

    /**
     * Increments the total likes for a given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new total after increment
     * @throws Exception if update fails
     */
    int incrementTotal(String site, String likeId, String type) throws Exception;

    /**
     * Decrements the total likes for a given site, likeId, and type.
     *
     * @param site the site name
     * @param likeId the like identifier
     * @param type the like type
     * @return new total after decrement
     * @throws Exception if update fails
     */
    int decrementTotal(String site, String likeId, String type) throws Exception;
}
