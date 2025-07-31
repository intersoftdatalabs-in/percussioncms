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
package com.percussion.delivery.comments.data;

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Container for a list of comments, used for JSON/XML serialization.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"comments"})
@XmlRootElement(name = "comments")
public class PSComments {
    private final List<IPSComment> comments;

    /**
     * Default constructor for JAXB.
     * Initializes with an empty list.
     */
    public PSComments() {
        this.comments = Collections.emptyList();
    }

    /**
     * Constructs a PSComments object.
     * @param comments the list of comments, never null
     */
    public PSComments(List<IPSComment> comments) {
        this.comments = comments == null ? Collections.emptyList() : List.copyOf(comments);
    }

    /**
     * @return unmodifiable list of comments, never null
     */
    public List<IPSComment> getComments() {
        return comments;
    }
}
