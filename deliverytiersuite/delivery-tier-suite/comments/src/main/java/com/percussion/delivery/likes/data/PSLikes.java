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

import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Container for a list of likes, used for JSON/XML serialization.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"likes"})
@XmlRootElement(name = "likes")
public class PSLikes {

    private List<IPSLikes> likes;

    /**
     * Default constructor for JAXB.
     * Initializes with an empty list.
     */
    public PSLikes() {
        this.likes = Collections.emptyList();
    }

    /**
     * Constructs a PSLikes object.
     * @param likes the list of likes, never null
     */
    public PSLikes(List<IPSLikes> likes) {
        this.likes = likes == null ? Collections.emptyList() : List.copyOf(likes);
    }

    /**
     * @return unmodifiable list of likes, never null
     */
    public List<IPSLikes> getLikes() {
        if (likes == null) {
            likes = Collections.emptyList();
        }
        return likes;
    }
}
