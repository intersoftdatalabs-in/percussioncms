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

package com.percussion.util;

import java.util.function.Function;

/**
 * Function that may throw a checked exception.
 *
 * @param <T> the input type
 * @param <R> the result type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
  /**
   * Applies this function.
   *
   * @param t the input, may be <code>null</code>
   * @return the result, may be <code>null</code>
   * @throws E on function errors
   */
  R apply(T t) throws E;

  /**
   * Wraps this function in an unchecked function.
   *
   * @param <T> the input type
   * @param <R> the result type
   * @param <E> the exception type
   * @param f the function to wrap, never <code>null</code>
   * @return the wrapping function, never <code>null</code>
   */
  static <T, R, E extends Throwable> Function<T, R> unchecked(ThrowingFunction<T, R, E> f) {
    return t -> {
      try {
        return f.apply(t);
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }
}
