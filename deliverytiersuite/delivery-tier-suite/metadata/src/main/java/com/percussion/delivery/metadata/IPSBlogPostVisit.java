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

package com.percussion.delivery.metadata;

import java.math.BigInteger;
import java.util.Date;

/**
 * Represents a blog post visit entry.
 */
public interface IPSBlogPostVisit {

    /**
     * Gets the hit count for this blog post.
     * @return the hit count.
     */
    BigInteger getHitCount();

    /**
     * Sets the hit count for this blog post.
     * @param count the hit count.
     */
    void setHitCount(BigInteger count);

    /**
     * Gets the date of the hit.
     * @return the hit date.
     */
    Date getHitDate();

    /**
     * Sets the date of the hit.
     * @param date the hit date.
     */
    void setHitDate(Date date);

    /**
     * Gets the page path for this visit.
     * @return the page path.
     */
    String getPagepath();

    /**
     * Sets the page path for this visit.
     * @param path the page path.
     */
    void setPagepath(String path);
}
