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

package com.percussion.utils.jexl;

import java.util.Map;
import org.apache.commons.jexl3.JexlException;

/**
 * Script interface.
 */
public interface IPSScript {

  /**
   * An optional string indicating the type of system object that owns this script. Never null
   *
   * @return a user friendly string that indicates the owner type: Template, Widget, Snippet, Location Scheme etc. Never null. May be empty.
   */
  public String getOwnerType();

  /**
   * Sets the type of system object that owns this script. Should be user friendly and i18N.
   *
   * @param ownerType the owner type, may be <code>null</code>.
   */
  public void setOwnerType(String ownerType);

  /**
   * An optional property that indicates the system object that owns this script.
   *
   * @return name of the system object, never null, may be empty
   */
  public String getOwnerName();

  /**
   * Sets the user friendly name of the system object that owns this script. Should be user friendly.
   *
   * @param ownerName the owner name, never <code>null</code>.
   */
  public void setOwnerName(String ownerName);

  /**
   * Whether the script is compilable.
   *
   * @return <code>true</code> if compilable, <code>false</code> otherwise.
   */
  public boolean isCompilable();

  /**
   * Get the script text.
   *
   * @return the script text, may be <code>null</code>.
   */
  public String getScriptText();

  /**
   * Get the parsed text.
   *
   * @return the parsed text, may be <code>null</code>.
   */
  public String getParsedText();

  /**
   * Evaluate the script.
   *
   * @param bindings the bindings, may be <code>null</code>.
   * @return the result, may be <code>null</code>.
   * @throws JexlException if evaluation fails.
   */
  public Object eval(Map<String, Object> bindings) throws JexlException;

  /**
   * Get the source text.
   *
   * @return the source text, may be <code>null</code>.
   */
  public String getSourceText();

  /**
   * Get the use strict mode flag.
   *
   * @return <code>true</code> if strict mode, <code>false</code> otherwise.
   */
  public boolean getUseStrictMode();

  /**
   * Set the use strict mode flag.
   *
   * @param useStrictMode the flag.
   */
  public void setUseStrictMode(boolean useStrictMode);

  /**
   * Get the use debug mode flag.
   *
   * @return <code>true</code> if debug mode, <code>false</code> otherwise.
   */
  public boolean getUseDebugMode();

  /**
   * Set the use debug mode flag.
   *
   * @param useDebugMode the flag.
   */
  public void setUseDebugMode(boolean useDebugMode);

  /**
   * Get the silent mode flag.
   *
   * @return <code>true</code> if silent mode, <code>false</code> otherwise.
   */
  public boolean getSilentMode();

  /**
   * Set the use silent mode flag.
   *
   * @param useSilentMode the flag.
   */
  public void setUseSilentMode(boolean useSilentMode);

  /**
   * Reinitialize.
   *
   * @param reloadOptionsFromConfig whether to reload options from config.
   */
  public void reinit(boolean reloadOptionsFromConfig);
}