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

package com.percussion.process;

import java.util.Map;

/**
 * Interface to be implemented by process parameter resolvers. The framework uses classes
 * implementing this interface in the following way:
 *
 * <p>&lt;p&gt;While parsing the xml:
 *
 * <p>&lt;ol&gt; &lt;li&gt;Instantiate the class defined as the resolver.&lt;/li&gt; &lt;li&gt;Set
 * the name with the name supplied in the def.&lt;/li&gt; &lt;li&gt;Set the value with the value
 * supplied in the def. If no value is present, "" is set.&lt;/li&gt; &lt;/ol&gt;
 *
 * <p>During process instantiation, the {@link #getValue(String, Map)} method is called and its
 * result is passed to the process or process container.
 */
public interface IPSVariableResolver {
  /**
   * Returns the resolved value using the supplied context.
   *
   * @param value the string to resolve, may be <code>null</code> or empty
   * @param ctx a {@link Map} that contains data for executing the process, may not be <code>null
   *     </code>. Each entry has a <code>String</code> key and a <code>String</code> value. The
   *     supplied parameters are dependent upon the context in which the process is executed.
   * @return the resolved string, may be empty, never <code>null</code>
   * @throws PSResolveException if any error occurs resolving the specified string
   */
  public String getValue(String value, Map<String, ?> ctx) throws PSResolveException;
}
