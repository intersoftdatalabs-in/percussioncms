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
 * Holds summary information for likes on a page.
 */
public class PSLikesSummary {

    /**
     * Total number of likes.
     */
    private int total;

    private String likeId;

    public PSLikesSummary() {
        // Default constructor
    }

    public PSLikesSummary(int total, String likeId) {
        this.total = total;
        this.likeId = likeId;
    }

    /**
     * @return the total number of likes
     */
    public int getTotal() {
        return total;
    }

    /**
     * @param total the total to set
     */
    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * @return the likeId
     */
    public String getLikeId() {
        return likeId;
    }

    /**
     * @param likeId the likeId to set
     */
    public void setLikeId(String likeId) {
        this.likeId = likeId;
    }
}
