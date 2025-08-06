// REFACTORED: CP-JAVA11
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

package com.percussion.pagemanagement.data;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the configuration for categories in the CMS.
 * Contains a tree structure with a URL.
 */
@XmlRootElement(name = "CategoryConfig")
public class PSCategoryConfiguration {

    private Tree tree;

    /**
     * Gets the category tree.
     * @return the tree, may be {@code null}.
     */
    @XmlElement(name = "tree")
    public Tree getTree() {
        return tree;
    }

    public void setTree(Tree tree) {
        this.tree = tree;
    }

    /**
     * Represents a tree node in the category configuration.
     */
    public static class Tree {
        private String url;

        /**
         * Gets the URL for this tree node.
         * @return the URL, may be {@code null}.
         */
        @XmlAttribute(name = "url")
        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
