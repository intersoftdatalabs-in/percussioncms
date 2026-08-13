/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.security;

import com.percussion.design.objectstore.IPSGroupProviderInstance;
import com.percussion.design.objectstore.PSAttribute;
import com.percussion.design.objectstore.PSAttributeList;
import com.percussion.design.objectstore.PSDirectorySet;
import com.percussion.design.objectstore.PSLiteral;
import com.percussion.design.objectstore.PSLiteralSet;
import com.percussion.design.objectstore.PSReference;
import com.percussion.design.objectstore.PSSubject;
import com.percussion.util.PSCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Typed adapters over raw design.objectstore collections used by security catalogers.
 *
 * <p>Slice-5 (#2299 / #3289) keeps {@code com.percussion.design.objectstore} raw (owned by #2022
 * slices 1–2). Cataloger code uses these adapters instead of {@code @SuppressWarnings("unchecked")}.
 */
final class PSCatalogerTypes {

  private PSCatalogerTypes() {}

  /**
   * Subject identifier comparator used to order and de-duplicate cataloger results. Wraps the raw
   * objectstore comparator without an unchecked cast at each call site.
   */
  static Comparator<PSSubject> subjectIdentifierComparator() {
    Comparator<?> raw = wildcardComparator(PSSubject.getSubjectIdentifierComparator());
    return (left, right) -> compareSubjects(raw, left, right);
  }

  static Iterator<PSAttribute> attributes(PSAttributeList attributes) {
    Objects.requireNonNull(attributes, "attributes");
    return typed(attributes.iterator(), PSAttribute.class);
  }

  static List<PSAttribute> attributeList(PSAttributeList attributes) {
    return collect(attributes(attributes));
  }

  static Iterator<PSReference> directoryRefs(PSDirectorySet directorySet) {
    Objects.requireNonNull(directorySet, "directorySet");
    return typed(directorySet.iterator(), PSReference.class);
  }

  static Iterator<IPSGroupProviderInstance> groupProviderInstances(PSCollection raw) {
    Objects.requireNonNull(raw, "raw");
    return typed(raw.iterator(), IPSGroupProviderInstance.class);
  }

  static Iterator<PSLiteral> literals(PSLiteralSet literals) {
    Objects.requireNonNull(literals, "literals");
    return typed(literals.iterator(), PSLiteral.class);
  }

  static Iterator<String> strings(Iterator<?> raw) {
    Objects.requireNonNull(raw, "raw");
    return typed(raw, String.class);
  }

  static <T> Iterator<T> typed(Iterator<?> raw, Class<T> type) {
    Objects.requireNonNull(raw, "raw");
    Objects.requireNonNull(type, "type");
    return new Iterator<>() {
      private T next;
      private boolean ready;

      @Override
      public boolean hasNext() {
        advance();
        return ready;
      }

      @Override
      public T next() {
        advance();
        if (!ready) {
          throw new NoSuchElementException();
        }
        T value = next;
        next = null;
        ready = false;
        return value;
      }

      private void advance() {
        if (ready) {
          return;
        }
        if (!raw.hasNext()) {
          return;
        }
        Object item = raw.next();
        if (item != null && !type.isInstance(item)) {
          throw new ClassCastException(
              item.getClass().getName() + " cannot be cast to " + type.getName());
        }
        next = type.cast(item);
        ready = true;
      }
    };
  }

  static <T> List<T> collect(Iterator<T> iterator) {
    List<T> values = new ArrayList<>();
    while (iterator.hasNext()) {
      values.add(iterator.next());
    }
    return values;
  }

  /**
   * Assigns the raw objectstore {@link Comparator} through a wildcard so callers do not need
   * {@code @SuppressWarnings}.
   */
  @SuppressWarnings("rawtypes")
  private static Comparator<?> wildcardComparator(Comparator raw) {
    return raw;
  }

  /**
   * Applies the objectstore subject comparator. The unchecked conversion is inherent: {@link
   * PSSubject#getSubjectIdentifierComparator()} still returns a raw {@link Comparator}.
   */
  @SuppressWarnings("unchecked")
  private static int compareSubjects(Comparator<?> comparator, PSSubject left, PSSubject right) {
    return ((Comparator<PSSubject>) comparator).compare(left, right);
  }
}
