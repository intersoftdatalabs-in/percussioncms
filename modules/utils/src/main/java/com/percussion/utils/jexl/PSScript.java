/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */

package com.percussion.utils.jexl;

import com.percussion.install.RxFileManager;
import com.percussion.utils.container.DefaultConfigurationContextImpl;
import com.percussion.utils.container.PSContainerUtilsFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlFeatures;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PSScript implements IPSScript {
  /** The logger. NOTE: jexl requires commons logging */
  private static final Log LOG = LogFactory.getLog(PSScript.class);

  /** The shared expression cache size. */
  public static int CACHE_SIZE = 512;

  private String scriptText;

  // Control JEXL strict mode
  private static boolean jexlUseStrict = false;

  // Control JEXL silent
  private static boolean jexlUseSilent = false;

  // Control JEXL debug
  private static boolean jexlUseDebug = false;

<<<<<<< HEAD
  private boolean compilable = false;

  private JexlScript compiledScript = null;

  public PSScript(String scriptText) {
    this.scriptText = scriptText;
  }

=======
  // Explicit feature toggles for backward-compatibility control
  private static boolean jexlLexical = false;
  private static boolean jexlLexicalShade = false;
  private static boolean jexlConstCapture = false;

  private boolean compilable = false;

  private volatile JexlScript compiledScript = null;
  private volatile String fixedScriptText = null;

  public PSScript(String scriptText) {
    this.scriptText = scriptText;
  }

>>>>>>> development-8.1.x
  private String ownerType = "";

  private String ownerName = "";

<<<<<<< HEAD
  /***
   * An optional string indicating the type of system object that owns this script. Never null
   * @return a user friendly string that indicates the owner type: Template, Widget, Snippet, Location Scheme etc. Never null.  May be empty.
=======
  /**
   * * An optional string indicating the type of system object that owns this script. Never null
   *
   * @return a user friendly string that indicates the owner type: Template, Widget, Snippet,
   *     Location Scheme etc. Never null. May be empty.
>>>>>>> development-8.1.x
   */
  @Override
  public String getOwnerType() {
    return ownerType;
  }

<<<<<<< HEAD
  /***
   * Sets the type of system object that owns this script.  Should be user friendly and i18N.
=======
  /**
   * * Sets the type of system object that owns this script. Should be user friendly and i18N.
   *
>>>>>>> development-8.1.x
   * @param ownerType
   */
  @Override
  public void setOwnerType(String ownerType) {
    if (ownerType == null) ownerType = "";
    this.ownerType = ownerType.trim();
  }

<<<<<<< HEAD
  /***
   * An optional property that indicates the system object that owns this script.
=======
  /**
   * * An optional property that indicates the system object that owns this script.
   *
>>>>>>> development-8.1.x
   * @return name of the system object, never null, may be empty
   */
  @Override
  public String getOwnerName() {
    return ownerName;
  }

<<<<<<< HEAD
  /***
   * Sets the user friendly name of the system object that owns this script. Should be user friendly.
   * @param ownerName  Never null.
=======
  /**
   * * Sets the user friendly name of the system object that owns this script. Should be user
   * friendly.
   *
   * @param ownerName Never null.
>>>>>>> development-8.1.x
   */
  @Override
  public void setOwnerName(String ownerName) {
    if (ownerName == null) {
      ownerName = "";
<<<<<<< HEAD
    }
    this.ownerName = ownerName.trim();
  }

  @Override
  public boolean isCompilable() {
    return this.compilable;
  }

  @Override
  public String getScriptText() {
    return scriptText;
  }

  @Override
  public String getParsedText() {
    return scriptText;
  }

  @Override
  public Object eval(Map<String, Object> bindingsMap) throws JexlException {
    JexlContext context = new MapContext(bindingsMap);

    if (compiledScript == null) {
      String fixedScriptText = JexlScriptFixes.fixScript(scriptText, ownerType, ownerName);

      compiledScript = EngineSingletonHolder.DEFAULT_ENGINE.createScript(fixedScriptText);
=======
>>>>>>> development-8.1.x
    }
    this.ownerName = ownerName.trim();
  }

<<<<<<< HEAD
    return compiledScript.execute(context);
  }

  @Override
  public String getSourceText() {
    return getScriptText();
  }

  @Override
  public boolean getUseStrictMode() {
    return jexlUseStrict;
  }

  @Override
  public void setUseStrictMode(boolean useStrictMode) {
    jexlUseStrict = useStrictMode;
  }

  @Override
  public boolean getUseDebugMode() {
    return jexlUseDebug;
  }

  @Override
  public void setUseDebugMode(boolean useDebugMode) {
    jexlUseDebug = useDebugMode;
  }

  @Override
  public boolean getSilentMode() {
    return jexlUseSilent;
  }

  @Override
  public void setUseSilentMode(boolean useSilentMode) {
    jexlUseSilent = useSilentMode;
  }

  /***
   * Reinitialize
   * @param reloadOptionsFromConfig
   */
  @Override
  public void reinit(boolean reloadOptionsFromConfig) {
    EngineSingletonHolder.reinit(reloadOptionsFromConfig);
  }

  private static final class EngineSingletonHolder {
    /** non instantiable. */
    private EngineSingletonHolder() {
      initConfig();
    }

=======
  @Override
  public boolean isCompilable() {
    return this.compilable;
  }

  @Override
  public String getScriptText() {
    return scriptText;
  }

  @Override
  public String getParsedText() {
    return scriptText;
  }

  @Override
  public Object eval(Map<String, Object> bindingsMap) throws JexlException {
    JexlContext context = new MapContext(bindingsMap);
    try {
      // Double-checked locking to avoid duplicate compilation under concurrency
      JexlScript local = compiledScript;
      if (local == null) {
        synchronized (this) {
          local = compiledScript;
          if (local == null) {
            if (fixedScriptText == null) {
              fixedScriptText = JexlScriptFixes.fixScript(scriptText, ownerType, ownerName);
            }
            local = EngineSingletonHolder.DEFAULT_ENGINE.createScript(fixedScriptText);
            compiledScript = local;
          }
        }
      }
      return local.execute(context);
    } catch (JexlException x) {
      // Add owner context for faster diagnosis while preserving original exception type
      if (LOG.isDebugEnabled()) {
        LOG.debug(
            "JEXL error in script. Type: "
                + ownerType
                + " Name: "
                + ownerName
                + " Script: "
                + scriptText);
      }
      throw x;
    }
  }

  @Override
  public String getSourceText() {
    return getScriptText();
  }

  @Override
  public boolean getUseStrictMode() {
    return jexlUseStrict;
  }

  @Override
  public void setUseStrictMode(boolean useStrictMode) {
    jexlUseStrict = useStrictMode;
  }

  @Override
  public boolean getUseDebugMode() {
    return jexlUseDebug;
  }

  @Override
  public void setUseDebugMode(boolean useDebugMode) {
    jexlUseDebug = useDebugMode;
  }

  @Override
  public boolean getSilentMode() {
    return jexlUseSilent;
  }

  @Override
  public void setUseSilentMode(boolean useSilentMode) {
    jexlUseSilent = useSilentMode;
  }

  /**
   * * Reinitialize
   *
   * @param reloadOptionsFromConfig
   */
  @Override
  public void reinit(boolean reloadOptionsFromConfig) {
    EngineSingletonHolder.reinit(reloadOptionsFromConfig);
  }

  private static final class EngineSingletonHolder {
    /** non instantiable. */
    private EngineSingletonHolder() {
      initConfig();
    }

>>>>>>> development-8.1.x
    /** The JEXL engine singleton instance. */
    private static JexlEngine DEFAULT_ENGINE =
        new JexlBuilder()
            .strict(jexlUseStrict)
            .silent(jexlUseSilent)
            .debug(jexlUseDebug)
            .logger(LOG)
            .cache(CACHE_SIZE)
<<<<<<< HEAD
=======
            // Explicit feature toggles (default false) to preserve legacy behavior
            .features(
                new JexlFeatures()
                    .lexical(jexlLexical)
                    .lexicalShade(jexlLexicalShade)
                    .constCapture(jexlConstCapture))
>>>>>>> development-8.1.x
            .create();

    private static void initConfig() {
      DefaultConfigurationContextImpl config =
          PSContainerUtilsFactory.getConfigurationContextInstance();

      Path root = config.getRootDir();

      RxFileManager mgr = new RxFileManager(root.toString());

      String serverProps = mgr.getServerPropertiesFile();

      Properties props = null;

      try {
        props = RxFileManager.loadProperties(serverProps);
      } catch (IOException e) {
        LOG.warn(
            "JEXL engine unable to load server.properties, default configuration will be used.");
      }

      if (props != null) {

        // Default to non strict for backward compatibility.
        jexlUseStrict = Boolean.parseBoolean(props.getProperty("jexlUseStrict", "false"));

        jexlUseSilent = Boolean.parseBoolean(props.getProperty("jexlUseSilent", "false"));

        jexlUseDebug = Boolean.parseBoolean(props.getProperty("jexlUseDebug", "false"));

        CACHE_SIZE = Integer.parseInt(props.getProperty("jexlCacheSize", "512"));
<<<<<<< HEAD
=======

        // Feature flags default to false for legacy semantics; can be enabled via config
        jexlLexical = Boolean.parseBoolean(props.getProperty("jexlLexical", "false"));
        jexlLexicalShade = Boolean.parseBoolean(props.getProperty("jexlLexicalShade", "false"));
        jexlConstCapture = Boolean.parseBoolean(props.getProperty("jexlConstCapture", "false"));
>>>>>>> development-8.1.x
      }
    }

    public static synchronized void reinit(boolean reloadOptionsFromConfig) {
      // Reload from property files if set.
      if (reloadOptionsFromConfig) initConfig();

      if (DEFAULT_ENGINE != null) DEFAULT_ENGINE.clearCache();

      DEFAULT_ENGINE =
          new JexlBuilder()
              .strict(jexlUseStrict)
              .silent(jexlUseSilent)
              .debug(jexlUseDebug)
              .logger(LOG)
              .cache(CACHE_SIZE)
<<<<<<< HEAD
=======
              // Preserve backward-compatible semantics unless explicitly overridden by config
              .features(
                  new JexlFeatures()
                      .lexical(jexlLexical)
                      .lexicalShade(jexlLexicalShade)
                      .constCapture(jexlConstCapture))
>>>>>>> development-8.1.x
              .create();
    }
  }

  @Override
  public String toString() {
    return "PSScript{"
        + "scriptText='"
        + scriptText
        + '\''
        + ", compilable="
        + compilable
        + ", compiledScript="
        + compiledScript
        + ", ownerType='"
        + ownerType
        + '\''
        + ", ownerName='"
        + ownerName
        + '\''
        + '}';
  }
}
