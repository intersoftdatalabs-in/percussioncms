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

package com.percussion.extension;

import java.util.Iterator;

/**
 * An IPSExtensionDef defines an extension and its deployment settings, including the URLs of all
 * its resources. The actual contents of the resources are specified elsewhere.
 *
 * <p>This interface may be extended to support particular kinds of extension definitions.
 */
public interface IPSExtensionDef {
  /** The full classname for the extension. */
  String INIT_PARAM_CLASSNAME = "className";

  /** The javascript version. */
  String INIT_PARAM_JAVASCRIPT_VERSION = "version";

  /** The extension version. */
  String INIT_PARAM_VERSION = "com.percussion.extension.version";

  /**
   * The full classname of the factory that should be used to serialize and deserialize this def.
   * This is for future extensibility.
   */
  String INIT_PARAM_FACTORY = "com.percussion.extension.factoryClassName";

  /**
   * "yes" or "no" depending on whether this extension is reentrant. This is used by the server. The
   * default value is "no". The initial version only supports re-entrant extensions.
   */
  String INIT_PARAM_REENTRANT = "com.percussion.extension.reentrant";

  /**
   * This is provided so all users will use this property consistently. "yes" or "no" depending on
   * whether this extension is scriptable. The server does not use this property.
   */
  String INIT_PARAM_SCRIPTABLE = "com.percussion.user.scriptable";

  /**
   * This is provided so all users will use this property consistently. The human-readable, freeform
   * description of this extension. The server does not use this property.
   */
  String INIT_PARAM_DESCRIPTION = "com.percussion.user.description";

  /**
   * Usually file name extension of template source for given assembler. If specified then template
   * editor adds this suffix when generating file name it uses to discover editor for this source.
   * If not specified no editor will be displayed. Sample values: ".txt", ".xml".
   */
  String INIT_PARAM_ASSEMBLY_FILE_SUFFIX = "com.percussion.extension.assembly.fileSuffix";

  /**
   * Controls the loading of velocity macros. yes means that the velocity engine is told to reload
   * on change. sys_reinit=true may need to be used anyway.
   */
  String INIT_PARAM_ASSEMBLY_AUTO_RELOAD = "com.percussion.extension.assembly.autoReload";

  /**
   * A comma delimited list of &quot;library&quot; velocity macro files to load from the library
   * locations.
   */
  String INIT_PARAM_ASSEMBLY_LIBRARIES = "com.percussion.extension.assembly.libraries";

  /** The the javascript script body. */
  String INIT_PARAM_SCRIPT_BODY = "scriptBody";

  /**
   * Returns the extension reference containing the fully qualified name of this extension.
   *
   * @return The extension reference. Never {@code null}.
   */
  PSExtensionRef getRef();

  /**
   * Returns the fully qualified names of the known extension interfaces implemented by this
   * extension. These interfaces help to define where instances of this extension may be used (for
   * example, is this a JDBC driver, a password filter, a result document processor, or some
   * combination of the three?).
   *
   * @return An Iterator over one or more non-{@code null} Strings. Never {@code null}.
   */
  Iterator<String> getInterfaces();

  /**
   * Compares the supplied interface name against the interfaces supported by this extension and
   * returns the result.
   *
   * @param iface The fully qualified name of the interface to check.
   * @return {@code true} if the supplied interface is supported by this extension, {@code false}
   *     otherwise.
   */
  boolean implementsInterface(String iface);

  /**
   * Returns the names of the initialization parameters. Initialization parameters will be passed to
   * the extension (via an extension-specific mechanism) when the extension is first initialized.
   * All parameters beginning with "com.percussion.extension" are used by the extension subsystem.
   * No user defined params should begin with this suffix. The order of parameters is unspecified
   * and should not be taken to mean anything.
   *
   * @return An Iterator over zero or more non-{@code null} Strings. Never {@code null}.
   */
  Iterator<String> getInitParameterNames();

  /**
   * Returns a String containing the value of the named initialization parameter of the extension,
   * or {@code null} if the parameter does not exist. All parameters beginning with
   * "com.percussion.extension" are used by the extension subsystem. No user defined params should
   * begin with this suffix.
   *
   * @param name The parameter name. Must not be {@code null}.
   * @return The value of the named parameter, or {@code null} if it does not exist. This method
   *     will never return {@code null} when called with a String value from the parameter name
   *     iteration.
   * @see #getInitParameterNames
   */
  String getInitParameter(String name);

  /**
   * Gets the names of all runtime parameters required by this extension. These runtime parameters
   * must be bound (in the returned order) to the extension instance at runtime (usually the caller
   * of the extension is responsible for doing the binding).
   *
   * @return An Iterator over zero or more non-{@code null} String param names. The order of the
   *     parameter names is important and should be preserved.
   */
  Iterator<String> getRuntimeParameterNames();

  /**
   * Gets the parameter definition for the named runtime parameter, or {@code null} if no parameter
   * by that name is used.
   *
   * @param name The param name. Must not be {@code null}.
   * @return The parameter definition, or {@code null}.
   */
  IPSExtensionParamDef getRuntimeParameter(String name);

  /**
   * Returns the locations of all resources required by the defined extension. This includes any
   * files which make up the extension itself (such as Java .class or .jar files, native executables
   * or libraries, etc.) as well as any other resources used by the extension.
   *
   * @return An Iterator over zero or more non-{@code null} java.net.URL objects. Never {@code
   *     null}.
   */
  Iterator<java.net.URL> getResourceLocations();

  /**
   * Returns the supplied resources for this extension.
   *
   * @return An Iterator over zero or more URL objects. May be {@code null} if no resources have
   *     been supplied.
   */
  Iterator<java.net.URL> getSuppliedResources();

  /**
   * Sets the supplied resources for this extension.
   *
   * @param resources A non-{@code null} but possibly empty Iterator of URL objects.
   */
  void setSuppliedResources(Iterator<java.net.URL> resources);

  /**
   * Sets the list of extension applications that are required by this extension. Every time this
   * extension is loaded, all of the required applications will be loaded as well.
   *
   * <p>This method may be called multiple times. Each call replaces the previous data.
   *
   * <p>The applications are loaded in the order they are supplied. If an application can't be
   * loaded, a warning is logged and the process continues. If an application is already loaded, it
   * is skipped.
   *
   * <p>Circular dependencies are not detected. If they exist, the behavior is undefined.
   *
   * <p>Applications are not automatically unloaded when this extension is unloaded.
   *
   * @param apps An iterator over 0 or more PSExtensionRef objects. If {@code null}, any existing
   *     settings are cleared. May be an empty iterator.
   */
  void setRequiredApplications(Iterator<PSExtensionRef> apps);

  /**
   * Returns the list of applications that are required by this extension. See {@link
   * #setRequiredApplications(Iterator)} for more info.
   *
   * @return An iterator over 0 or more PSExtensionRef objects. Never {@code null}, but may be
   *     empty.
   */
  Iterator<PSExtensionRef> getRequiredApplications();

  /**
   * Returns extension methods implemented by this extension. This is used in jexl method
   * extensions.
   *
   * @return An iterator over 0 or more PSExtensionMethod objects. Never {@code null}, but may be
   *     empty.
   */
  Iterator<PSExtensionMethod> getMethods();

  /**
   * Returns the stored version as an integer. Default implementation reads the {@link
   * #INIT_PARAM_VERSION} initialization parameter and parses it as an integer.
   *
   * @return the extension version, or {@code 0} when not set or not parseable.
   */
  default int getVersion() {
    String v = getInitParameter(INIT_PARAM_VERSION);
    if (v == null) return 0;
    try {
      return Integer.parseInt(v);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Indicates whether this extension implements jexl extension methods. Default implementation
   * returns true when {@link #getMethods()} is non-empty.
   *
   * @return {@code true} if this extension exposes jexl methods, {@code false} otherwise
   */
  default boolean isJexlExtension() {
    Iterator<PSExtensionMethod> it = getMethods();
    return it != null && it.hasNext();
  }

  /**
   * Adds an extension method to this definition. Default implementation is a no-op to preserve
   * backward compatibility for implementations that do not support mutation.
   *
   * @param method the extension method to add
   */
  default void addExtensionMethod(PSExtensionMethod method) {
    // no-op for backward compatibility
  }

  /**
   * Indicates whether this extension is deprecated. Default returns {@code false} to preserve
   * backward compatibility when implementations do not provide this information.
   *
   * @return {@code true} if deprecated, {@code false} otherwise
   */
  default boolean isDeprecated() {
    return false;
  }

  /**
   * Indicates whether request parameters should be restored when an extension throws an error.
   * Default returns {@code false} to preserve backward compatibility.
   *
   * @return {@code true} to restore request parameters on error, {@code false} otherwise
   */
  default boolean isRestoreRequestParamsOnError() {
    return false;
  }
}
