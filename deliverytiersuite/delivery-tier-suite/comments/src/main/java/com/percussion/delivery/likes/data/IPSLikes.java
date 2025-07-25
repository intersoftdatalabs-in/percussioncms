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
package com.percussion.delivery.likes.data;

/**
 * Represents a like entity for a page, comment, or image.
 */
public interface IPSLikes {

    /**
     * @return the site name; never null or empty.
     */
    String getSite();

    /**
     * @return the type of like (page, comment, image).
     */
    String getType();

    /**
     * @return the like identifier.
     */
    String getLikeId();

    /**
     * @return the total number of likes.
     */
    int getTotal();

    /**
     * @return the unique ID for this like.
     */
    String getId();

    void setId(String id);

    void setSite(String site);

    void setType(String type);

    void setLikeId(String id);

    void setTotal(int total);

    /**
     * Like types.
     */
    enum Type {
        PAGE, COMMENT, IMAGE
    }
}
