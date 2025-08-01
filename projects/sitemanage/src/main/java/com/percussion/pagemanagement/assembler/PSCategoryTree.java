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
package com.percussion.pagemanagement.assembler;

import com.percussion.utils.types.PSPair;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category tree node with a count and child categories.
 */
public class PSCategoryTree {

    private String category;
    private PSPair<Integer, Integer> count;
    private List<PSCategoryTree> children;

    public PSCategoryTree() {
        // Default constructor
    }

    /**
     * Constructs a category tree node with the specified category.
     *
     * @param category the category name
     */
    public PSCategoryTree(String category) {
        this.category = category;
        this.count = new PSPair<>(0, 0);
        this.children = new ArrayList<>();
    }

    /**
     * Constructs a category tree node with the specified category, count, and children.
     *
     * @param category the category name
     * @param count the count pair
     * @param children the child category nodes
     */
    public PSCategoryTree(String category, PSPair<Integer, Integer> count, List<PSCategoryTree> children) {
        this.category = category;
        this.count = count;
        this.children = children;
    }

    /**
     * Gets the category name.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category name.
     *
     * @param category the category to set
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the count pair.
     *
     * @return the count
     */
    public PSPair<Integer, Integer> getCount() {
        return count;
    }

    /**
     * Sets the count pair.
     *
     * @param count the count to set
     */
    public void setCount(PSPair<Integer, Integer> count) {
        this.count = count;
    }

    /**
     * Gets the child category nodes.
     *
     * @return the children
     */
    public List<PSCategoryTree> getChildren() {
        return children;
    }

    /**
     * Sets the child category nodes.
     *
     * @param children the children to set
     */
    public void setChildren(List<PSCategoryTree> children) {
        this.children = children;
    }
}
