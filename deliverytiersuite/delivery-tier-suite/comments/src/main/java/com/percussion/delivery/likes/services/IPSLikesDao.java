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

import java.util.Collection;
import java.util.List;

import com.percussion.delivery.likes.data.IPSLikes;

public interface IPSLikesDao 
{
    List<IPSLikes> find(String site, String likeId, String type) throws Exception;

    List<IPSLikes> findLikesForSite(String site) throws Exception;

    void delete(Collection<String> ids) throws Exception;

    void save(IPSLikes like) throws Exception;

    void save(List<IPSLikes> likes) throws Exception;

    IPSLikes create(String site, String likeId, String type) throws Exception;

    int incrementTotal(String site, String likeId, String type) throws Exception;

    int decrementTotal(String site, String likeId, String type) throws Exception;
}
