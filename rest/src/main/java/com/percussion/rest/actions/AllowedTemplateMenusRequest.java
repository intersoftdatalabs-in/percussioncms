/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

package com.percussion.rest.actions;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Request object for allowed template menus.
 */
@XmlRootElement
@Schema
public class AllowedTemplateMenusRequest {

    private int[] contentIds;

    public AllowedTemplateMenusRequest() {}

    public Optional<int[]> getContentIds() {
        return Optional.ofNullable(contentIds);
    }

    public void setContentIds(int[] contentIds) {
        this.contentIds = contentIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AllowedTemplateMenusRequest)) return false;
        var that = (AllowedTemplateMenusRequest) o;
        return Arrays.equals(contentIds, that.contentIds);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(contentIds);
    }

    @Override
    public String toString() {
        return "AllowedTemplateMenusRequest{" +
                "contentIds=" + Arrays.toString(contentIds) +
                '}';
    }
}
