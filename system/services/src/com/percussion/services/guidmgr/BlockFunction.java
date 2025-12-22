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

package com.percussion.services.guidmgr;

import java.util.function.BiFunction;

/**
 * A functional interface for operations that take two parameters and return a result.
 * This interface is equivalent to {@link BiFunction} and is maintained for backward compatibility.
 *
 * <p>New code should prefer using {@link BiFunction} directly for better integration
 * with the standard Java functional programming APIs.
 *
 * @param <T1> the type of the first argument to the function
 * @param <T2> the type of the second argument to the function
 * @param <T3> the type of the result of the function
 *
 * @author dougrand
 * @since Java 11 Modernization
 * @deprecated Use {@link BiFunction} instead for new implementations
 */
@Deprecated(since = "Java 11 Migration")
@FunctionalInterface
public interface BlockFunction<T1, T2, T3> extends BiFunction<T1, T2, T3> {

   /**
    * Applies this function to the given arguments.
    *
    * @param t1 the first function argument
    * @param t2 the second function argument
    * @return the function result
    */
   @Override
   T3 apply(T1 t1, T2 t2);

   /**
    * Creates a new BlockFunction from a BiFunction for backward compatibility.
    *
    * @param <T1> the type of the first argument
    * @param <T2> the type of the second argument
    * @param <T3> the type of the result
    * @param biFunction the BiFunction to wrap
    * @return a BlockFunction equivalent
    */
   static <T1, T2, T3> BlockFunction<T1, T2, T3> of(BiFunction<T1, T2, T3> biFunction) {
      return biFunction::apply;
   }
}
