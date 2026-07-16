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
import org.apache.commons.jexl3.introspection.JexlPermissions;
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

  // Explicit feature toggles for backward-compatibility control (legacy defaults: off)
  private static boolean jexlLexical = false;
  private static boolean jexlLexicalShade = false;
  private static boolean jexlConstCapture = false;

  private boolean compilable = false;

  /** Compiled script; volatile for safe publication under concurrent first eval. */
  private volatile JexlScript compiledScript = null;

  /** Script text after legacy syntax fixes; volatile for concurrent first compile. */
  private volatile String fixedScriptText = null;

  // Per-instance JEXL mode flags to avoid cross-test/global state pollution
  private boolean useStrictMode = jexlUseStrict;
  private boolean useDebugMode = jexlUseDebug;
  private boolean useSilentMode = jexlUseSilent;

  public PSScript(String scriptText) {
    this.scriptText = scriptText;
  }

  private String ownerType = "";

  private String ownerName = "";

  /***
   * An optional string indicating the type of system object that owns this script. Never null
   * @return a user friendly string that indicates the owner type: Template, Widget, Snippet, Location Scheme etc. Never null.  May be empty.
   */
  @Override
  public String getOwnerType() {
    return ownerType;
  }

  /***
   * Sets the type of system object that owns this script.  Should be user friendly and i18N.
   * @param ownerType
   */
  @Override
  public void setOwnerType(String ownerType) {
    if (ownerType == null) ownerType = "";
    this.ownerType = ownerType.trim();
  }

  /***
   * An optional property that indicates the system object that owns this script.
   * @return name of the system object, never null, may be empty
   */
  @Override
  public String getOwnerName() {
    return ownerName;
  }

  /***
   * Sets the user friendly name of the system object that owns this script. Should be user friendly.
   * @param ownerName  Never null.
   */
  @Override
  public void setOwnerName(String ownerName) {
    if (ownerName == null) {
      ownerName = "";
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

    try {
      // Double-checked locking: safe publication of compiled script under concurrent first eval
      JexlScript local = compiledScript;
      if (local == null) {
        synchronized (this) {
          local = compiledScript;
          if (local == null) {
            if (fixedScriptText == null) {
              this.fixedScriptText = JexlScriptFixes.fixScript(scriptText, ownerType, ownerName);
            }

            // Use the shared engine if instance flags match defaults; otherwise create an
            // engine configured for this instance so mode changes don't affect other
            // instances/tests.
            if (useStrictMode == jexlUseStrict
                && useDebugMode == jexlUseDebug
                && useSilentMode == jexlUseSilent) {
              local = EngineSingletonHolder.DEFAULT_ENGINE.createScript(this.fixedScriptText);
            } else {
              JexlEngine engine =
                  newBuilder(useStrictMode, useSilentMode, useDebugMode).create();
              local = engine.createScript(this.fixedScriptText);
            }
            compiledScript = local;
          }
        }
      }

      if (ownerName == null) {
        this.ownerName = "";
      } else {
        this.ownerName = ownerName.trim();
      }

      Object result = local.execute(context);

      // Enforce strict-mode behavior: if strict is enabled and silent is disabled,
      // a top-level $ expression that evaluates to null should throw an exception
      // (matches legacy expectations/tests).
      if (useStrictMode && !useSilentMode) {
        String src = this.fixedScriptText != null ? this.fixedScriptText : this.scriptText;
        if (src != null && src.trim().startsWith("$") && result == null) {
          throw new RuntimeException("JEXL evaluation returned null in strict mode for: " + src);
        }
      }
      this.ownerName = ownerName.trim();
      return result;
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

  /**
   * Builds a {@link JexlBuilder} with product defaults: unrestricted permissions (JEXL 2.x / legacy
   * 3.x compatibility), safe navigation when not strict, and optional lexical feature flags from
   * server.properties.
   *
   * @param strict whether strict mode is enabled
   * @param silent whether silent mode is enabled
   * @param debug whether debug mode is enabled
   * @return a configured builder (not yet created)
   */
  private static JexlBuilder newBuilder(boolean strict, boolean silent, boolean debug) {
    return new JexlBuilder()
        .strict(strict)
        .safe(!strict)
        .silent(silent)
        .permissions(JexlPermissions.UNRESTRICTED)
        .debug(debug)
        .logger(LOG)
        .cache(CACHE_SIZE)
        .features(
            new JexlFeatures()
                .lexical(jexlLexical)
                .lexicalShade(jexlLexicalShade)
                .constCapture(jexlConstCapture));
  }

  @Override
  public String getSourceText() {
    return getScriptText();
  }

  @Override
  public boolean getUseStrictMode() {
    return useStrictMode;
  }

  @Override
  public void setUseStrictMode(boolean useStrictMode) {
    this.useStrictMode = useStrictMode;
  }

  @Override
  public boolean getUseDebugMode() {
    return useDebugMode;
  }

  @Override
  public void setUseDebugMode(boolean useDebugMode) {
    this.useDebugMode = useDebugMode;
  }

  @Override
  public boolean getSilentMode() {
    return useSilentMode;
  }

  @Override
  public void setUseSilentMode(boolean useSilentMode) {
    this.useSilentMode = useSilentMode;
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

    /** The JEXL engine singleton instance. */
    private static JexlEngine DEFAULT_ENGINE =
        newBuilder(jexlUseStrict, jexlUseSilent, jexlUseDebug).create();

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

        // Feature flags default to false for legacy semantics; can be enabled via config
        jexlLexical = Boolean.parseBoolean(props.getProperty("jexlLexical", "false"));
        jexlLexicalShade = Boolean.parseBoolean(props.getProperty("jexlLexicalShade", "false"));
        jexlConstCapture = Boolean.parseBoolean(props.getProperty("jexlConstCapture", "false"));
      }
    }

    public static synchronized void reinit(boolean reloadOptionsFromConfig) {
      // Reload from property files if set.
      if (reloadOptionsFromConfig) initConfig();

      if (DEFAULT_ENGINE != null) DEFAULT_ENGINE.clearCache();

      DEFAULT_ENGINE = newBuilder(jexlUseStrict, jexlUseSilent, jexlUseDebug).create();
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
