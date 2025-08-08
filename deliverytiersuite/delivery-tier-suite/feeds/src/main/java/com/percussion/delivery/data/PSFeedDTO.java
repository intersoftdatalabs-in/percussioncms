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

package com.percussion.delivery.data;

import java.util.Objects;

public class PSFeedDTO {

    private String feedsUrl;
    private String hostName;

    public PSFeedDTO() {
        super();
    }

    public PSFeedDTO(String feedsUrl, String hostName) {
        this.feedsUrl = Objects.requireNonNull(feedsUrl, "feedsUrl cannot be null");
        this.hostName = Objects.requireNonNull(hostName, "hostName cannot be null");
    }

    public String getFeedsUrl() {
        return feedsUrl;
    }

    public void setFeedsUrl(String feedsUrl) {
        this.feedsUrl = feedsUrl;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PSFeedDTO psFeedDTO = (PSFeedDTO) o;
        return Objects.equals(feedsUrl, psFeedDTO.feedsUrl) &&
                Objects.equals(hostName, psFeedDTO.hostName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feedsUrl, hostName);
    }

    @Override
    public String toString() {
        return "PSFeedDTO{" +
                "feedsUrl='" + feedsUrl + '\'' +
                ", hostName='" + hostName + '\'' +
                '}';
    }
}
