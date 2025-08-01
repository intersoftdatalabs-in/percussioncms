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

package com.percussion.category.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a node in a transformable category tree.
 */
@XmlRootElement(name = "CategoryNode")
public class PSTransformCategoryNode {

    private String id;
    private String label;
    private String selectable;
    private List<PSTransformCategoryNode> childNodes = new ArrayList<>();

    @XmlAttribute(name = "id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @XmlAttribute(name = "selectable")
    public String getSelectable() {
        return selectable;
    }

    public void setSelectable(String selectable) {
        this.selectable = selectable;
    }

    @XmlElement(name = "Node")
    public List<PSTransformCategoryNode> getChildNodes() {
        return childNodes;
    }

    public void setChildNodes(List<PSTransformCategoryNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public String toString() {
        return "PSTransformCategoryNode [id=" + id + ", label=" + label + ", selectable=" + selectable
                + ", childNodes=" + childNodes + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(childNodes, id, label, selectable);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSTransformCategoryNode)) return false;
        var other = (PSTransformCategoryNode) obj;
        return Objects.equals(childNodes, other.childNodes)
                && Objects.equals(id, other.id)
                && Objects.equals(label, other.label)
                && Objects.equals(selectable, other.selectable);
    }
}
