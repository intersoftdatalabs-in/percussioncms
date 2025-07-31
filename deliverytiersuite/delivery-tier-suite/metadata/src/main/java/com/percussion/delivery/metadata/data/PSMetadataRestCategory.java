// REFACTORED: CP-JAVA11
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
package com.percussion.delivery.metadata.data;

import com.percussion.delivery.metadata.impl.utils.PSPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a category with its name, count, and children.
 */
public class PSMetadataRestCategory {

    private String category;
    private PSPair<Integer, Integer> count;
    private List<PSMetadataRestCategory> children;

    public PSMetadataRestCategory() {
        this.children = new ArrayList<>();
    }

    public PSMetadataRestCategory(String category) {
        this.category = category;
        this.count = new PSPair<>(0, 0);
        this.children = new ArrayList<>();
    }

    public PSMetadataRestCategory(String category, PSPair<Integer, Integer> count, List<PSMetadataRestCategory> children) {
        this.category = category;
        this.count = count;
        this.children = children;
    }

    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Optional<PSPair<Integer, Integer>> getCount() {
        return Optional.ofNullable(count);
    }

    public void setCount(PSPair<Integer, Integer> count) {
        this.count = count;
    }

    public List<PSMetadataRestCategory> getChildren() {
        return children;
    }

    public void setChildren(List<PSMetadataRestCategory> children) {
        this.children = children;
    }
}
