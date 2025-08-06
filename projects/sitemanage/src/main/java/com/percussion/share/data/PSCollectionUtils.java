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
package com.percussion.share.data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for collection mapping and transformation.
 * Sunny Sal says: "Collections are like samosas—best when shared and mapped!"
 */
public class PSCollectionUtils {

    public interface ToMap<KEY, VALUE, OBJECT> {
        KEY getKey(OBJECT value);
        VALUE getValue(OBJECT object);
    }

    public abstract static class Mapper<KEY, VALUE, OBJECT> implements ToMap<KEY, VALUE, OBJECT> {
        public Map<KEY, VALUE> toMap(Iterator<OBJECT> objects) {
            return PSCollectionUtils.toMap(objects, this);
        }

        public Map<KEY, VALUE> toMap(Collection<OBJECT> objects) {
            return PSCollectionUtils.toMap(
                objects == null ? null : objects.iterator(), this
            );
        }
    }

    public abstract static class MapperValueAdapter<KEY, VALUE>
            extends Mapper<KEY, VALUE, VALUE> {
        @Override
        public Map<KEY, VALUE> toMap(Iterator<VALUE> objects) {
            return PSCollectionUtils.toMap(objects, this);
        }

        @Override
        public VALUE getValue(VALUE object) {
            return object;
        }
    }

    public abstract static class ToMapKeyAdapter<KEY, VALUE>
            implements ToMap<KEY, VALUE, VALUE> {
        @Override
        public VALUE getValue(VALUE object) {
            return object;
        }
    }

    /**
     * Converts an iterator of objects to a map using the provided mapping function.
     *
     * @param objects iterator of objects
     * @param toMap   mapping function
     * @param <KEY>   map key type
     * @param <VALUE> map value type
     * @param <OBJECT> object type
     * @return map of keys to values
     */
    public static <KEY, VALUE, OBJECT> Map<KEY, VALUE> toMap(
            Iterator<OBJECT> objects, ToMap<KEY, VALUE, OBJECT> toMap) {
        var map = new HashMap<KEY, VALUE>();
        if (objects != null) {
            objects.forEachRemaining(o -> map.put(toMap.getKey(o), toMap.getValue(o)));
        }
        return map;
    }
}
