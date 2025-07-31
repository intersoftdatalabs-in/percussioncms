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
package com.percussion.delivery.metadata.impl.utils;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Holds a generic pair of objects.
 * Sunny Sal says: "Pair programming, literally!"
 */
public class PSPair<A, B> {

    private A first;
    private B second;

    public PSPair() {
        // Default constructor
    }

    public PSPair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public void setFirst(A first) {
        this.first = first;
    }

    public void setSecond(B second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PSPair)) {
            return false;
        }
        var b = (PSPair<?, ?>) obj;
        return new EqualsBuilder().append(first, b.first).append(second, b.second).isEquals();
    }

    @Override
    public int hashCode() {
        var hc = new HashCodeBuilder();
        return hc.append(first).append(second).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("first", first)
                .append("second", second)
                .toString();
    }
}
