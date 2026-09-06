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
// REFACTORED: CP-JAVA11

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigChangeListener;
import com.percussion.rx.config.IPSConfigHandler;
import com.percussion.rx.config.IPSConfigRegistrationMgr;
import com.percussion.rx.config.IPSConfigService;
import com.percussion.rx.config.IPSConfigStatusMgr;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.config.PSConfigValidation;
import com.percussion.rx.config.data.PSConfigStatus;
import com.percussion.rx.config.data.PSConfigStatus.ConfigStatus;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.io.PSPathInjectionGuard;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.util.IOTools;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.types.PSPair;
import jakarta.xml.bind.JAXBException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default implementation of {@link IPSConfigService}. Provides methods to apply local
 * configuration, register installed packages, and listen for package installation notifications.
 */
public class PSConfigService implements IPSConfigService {

  /** Default constructor for use by Spring. */
  public PSConfigService() {}

  /*
   * (non-Javadoc)
   * @see com.percussion.rx.config.IPSConfigService#notifyPackageInstalled(java.lang.String)
   */
  @Override
  public List<PSPair<String, Exception>> applyConfiguration(
      String[] configNames, boolean deltasOnly) {
    Objects.requireNonNull(configNames, "configNames must not be null");
    var results = new ArrayList<PSPair<String, Exception>>();
    for (var cfg : configNames) {
      if (!isValidConfiguartion(cfg)) {
        var msg =
            "Missing one or more configuration files for configuration {0}. Skipping"
                + " configuration.";
        var error =
            new PSPair<String, Exception>(
                cfg, new PSConfigException(MessageFormat.format(msg, cfg)));
        results.add(error);
        continue;
      }
      var lcFile = getConfigFile(ConfigTypes.LOCAL_CONFIG, cfg);
      try {
        applyLocalConfiguration(lcFile, deltasOnly);
      } catch (Exception e) {
        results.add(new PSPair<>(cfg, e));
      }
    }
    return results;
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.rx.config.IPSConfigService#uninstallConfiguartion(java.lang.String[])
   */
  @Override
  public Map<File, Exception> uninstallConfiguration(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be null or empty.");

    var undelMap = new HashMap<File, Exception>();

    m_configRegMgr.unregister(configName);
    deleteConfigFile(configName, ConfigTypes.CONFIG_DEF, undelMap);
    deleteConfigFile(configName, ConfigTypes.DEFAULT_CONFIG, undelMap);
    deleteConfigFile(configName, ConfigTypes.LOCAL_CONFIG, undelMap);
    deleteConfigFile(configName, ConfigTypes.VISIBILITY, undelMap);

    return undelMap;
  }

  private PSPurgableTempFile getTempConfigDefFile(String cfgName, String configDef)
      throws IOException {
    var cfgFile = new PSPurgableTempFile(cfgName, "xml", null);
    FileUtils.writeStringToFile(cfgFile, configDef, StandardCharsets.UTF_8);
    return cfgFile;
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.rx.config.IPSConfigService#validateConfiguartion(java.lang.String)
   */
  @Override
  public List<PSConfigValidation> validateConfiguartion(String configName) {
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be empty");
    List<PSConfigValidation> validationErrors = new ArrayList<>();
    try {
      validationErrors = validateConfig(configName);
    } catch (Exception e) {
      validationErrors.add(new PSConfigValidation(configName, e));
    }
    return validationErrors;
  }

  private void deleteConfigFile(String cfg, ConfigTypes type, Map<File, Exception> undelMap) {
    File file = null;
    try {
      file = getConfigFile(type, cfg);
      if (file.exists()) file.delete();
    } catch (Exception e) {
      undelMap.put(file, e);
    }
  }

  /*
   * (non-Javadoc)
   * @see com.percussion.rx.config.IPSConfigService#getConfigName(java.io.File)
   */
  @Override
  public String getConfigName(File configFile) {
    Objects.requireNonNull(configFile, "configFile must not be null");
    var fileName = configFile.getName();
    if (fileName.endsWith(LOCAL_CONFIG_FILE_SUFFIX + ".xml")) {
      return fileName.substring(0, fileName.indexOf(LOCAL_CONFIG_FILE_SUFFIX));
    } else if (fileName.endsWith(DEFAULT_CONFIG_FILE_SUFFIX + ".xml")) {
      return fileName.substring(0, fileName.indexOf(DEFAULT_CONFIG_FILE_SUFFIX));
    } else if (fileName.endsWith(CONFIG_DEF_FILE_SUFFIX + ".xml")) {
      return fileName.substring(0, fileName.indexOf(CONFIG_DEF_FILE_SUFFIX));
    } else if (fileName.endsWith(VISIBILITY_FILE_SUFFIX + ".xml")) {
      return fileName.substring(0, fileName.indexOf(VISIBILITY_FILE_SUFFIX));
    }
    return null;
  }

  private boolean isValidConfiguartion(String cfgName) {
    var lcFile = getConfigFile(ConfigTypes.LOCAL_CONFIG, cfgName);
    var dcFile = getConfigFile(ConfigTypes.DEFAULT_CONFIG, cfgName);
    var cdFile = getConfigFile(ConfigTypes.CONFIG_DEF, cfgName);
    return lcFile.exists() // codeql[java/path-injection]
        && dcFile.exists() // codeql[java/path-injection]
        && cdFile.exists(); // codeql[java/path-injection]
  }

  /**
   * Applies the local configuration from the supplied file to the design objects.
   *
   * @param localConfigFile the local configuration file, never <code>null</code>.
   * @param changesOnly if <code>true</code> only changes from the last successful configuration are
   *     applied; otherwise the full configuration is applied.
   */
  public void applyLocalConfiguration(File localConfigFile, boolean changesOnly) {
    Objects.requireNonNull(localConfigFile, "file must not be null");
    var safeLocal = requireConfigFileUnderRxDir(localConfigFile);

    var fileName = safeLocal.getName();
    var configName = fileName.substring(0, fileName.indexOf(LOCAL_CONFIG_FILE_SUFFIX));
    var prevCfg = getLastSuccessConfig(configName);
    var prevProps = prevCfg != null ? prevCfg.getSecond() : new HashMap<String, Object>();

    applyLocalConfiguration(safeLocal, prevProps, changesOnly);
  }

  /**
   * Applies the local configuration from the supplied file against the supplied previous properties
   * to the design objects.
   *
   * @param localConfigFile the local configuration file, never <code>null</code>.
   * @param prevProps the previous configuration properties, never <code>null</code>.
   * @param changesOnly if <code>true</code> only changes from the previous properties are applied;
   *     otherwise the full configuration is applied.
   */
  public void applyLocalConfiguration(
      File localConfigFile, Map<String, Object> prevProps, boolean changesOnly) {
    Objects.requireNonNull(localConfigFile, "file must not be null");
    Objects.requireNonNull(prevProps, "Previous properties must not be null");
    var safeLocal = requireConfigFileUnderRxDir(localConfigFile);

    var fileName = safeLocal.getName();
    var configName = fileName.substring(0, fileName.indexOf(LOCAL_CONFIG_FILE_SUFFIX));
    var status = ConfigStatus.FAILURE;

    try (var defConfIs =
        new FileInputStream(getConfigFile(ConfigTypes.DEFAULT_CONFIG, configName))) { // codeql[java/path-injection]
      var normalizer = new PSConfigNormalizer();
      var defaultProps = normalizer.getNormalizedMap(defConfIs);
      var newProps = getNewProps(safeLocal, defaultProps);
      Map<String, Object> propsToProcess;
      if (changesOnly) {
        var df = new PSConfigDeltaFinder();
        propsToProcess = df.getConfigDelta(newProps, prevProps);
        if (propsToProcess.isEmpty()) {
          ms_logger.info(
              "Skipped applying configuration for package ({}) as no changes found from the last"
                  + " successful configuration",
              configName);
          return;
        }
      } else {
        propsToProcess = newProps;
      }
      ms_logger.info("Applying configuration for package {}...", configName);

      notifyPreConfig(configName);

      var prevProps2 = prevProps.isEmpty() ? prevProps : applyDefaultProps(prevProps, defaultProps);

      var mapper = new PSConfigMapper();
      var cfgDefFile = getConfigFile(ConfigTypes.CONFIG_DEF, configName);
      var cfgHandlers =
          mapper.getResolvedHandlers(
              cfgDefFile.getAbsolutePath(), propsToProcess, newProps, prevProps2);

      validateConfig(configName, cfgHandlers);

      var merger = new PSConfigMerger();
      var mergeResults = merger.merge(cfgHandlers, !prevProps.isEmpty(), true);

      if (mergeResults.getSecond() != null) {
        status = ConfigStatus.FAILURE;
        notifyConfigChanges(mergeResults.getFirst(), status);
        throw mergeResults.getSecond();
      } else {
        status = ConfigStatus.SUCCESS;
        notifyConfigChanges(mergeResults.getFirst(), status);
        saveConfigStatus(configName, status);
      }

      ms_logger.info("Finished applying configuration for package {}", configName);
    } catch (Exception e) {
      ms_logger.error(
          "Failed to apply config for package {} Error: {}",
          configName,
          PSExceptionUtils.getMessageForLog(e));
      saveConfigStatus(configName, status);
      throw new PSConfigException(e);
    }
  }

  /**
   * Validates the supplied configuration name.
   *
   * @param cfgName the configuration name to validate, never <code>null</code> or empty.
   * @return the list of validation results, never <code>null</code>, may be empty.
   * @throws JAXBException if the configuration file cannot be parsed.
   */
  public List<PSConfigValidation> validateConfig(String cfgName) throws JAXBException {
    if (StringUtils.isBlank(cfgName))
      throw new IllegalArgumentException("Configure name must not be blank.");

    var defaultFile = getConfigFile(ConfigTypes.DEFAULT_CONFIG, cfgName);
    var localFile = getConfigFile(ConfigTypes.LOCAL_CONFIG, cfgName);
    var cfgDefFile = getConfigFile(ConfigTypes.CONFIG_DEF, cfgName);

    try (var defIS = new FileInputStream(defaultFile)) { // codeql[java/path-injection]
      try (var localIS = new FileInputStream(localFile)) { // codeql[java/path-injection]
        if (!(defaultFile.exists() // codeql[java/path-injection]
            && localFile.exists() // codeql[java/path-injection]
            && cfgDefFile.exists())) // codeql[java/path-injection]
          return Collections.emptyList();

        var normalizer = new PSConfigNormalizer();
        var defaultProps = normalizer.getNormalizedMap(defIS);
        var localProps = normalizer.getNormalizedMap(localIS);
        var curProps = applyDefaultProps(localProps, defaultProps);
        var emptyProps = Collections.<String, Object>emptyMap();

        var mapper = new PSConfigMapper();
        var cfgHandlers =
            mapper.getResolvedHandlers(
                cfgDefFile.getAbsolutePath(), curProps, curProps, emptyProps);

        return validateHandlers(cfgName, cfgHandlers);
      }
    } catch (IOException e) {
      ms_logger.error(PSExceptionUtils.getMessageForLog(e));
    }
    return Collections.emptyList();
  }

  private void validateConfig(String pkgName, List<IPSConfigHandler> hdls) {
    var hasError = false;
    for (var validate : validateHandlers(pkgName, hdls)) {
      if (validate.isError()) {
        hasError = true;
        ms_logger.error(validate.getValidationMsg());
      } else {
        ms_logger.warn(validate.getValidationMsg());
      }
    }
    if (hasError) throw new PSConfigException("Package \"" + pkgName + "\" validation failed.");
  }

  private List<PSConfigValidation> validateHandlers(String pkgName, List<IPSConfigHandler> hdls) {
    var result = new ArrayList<PSConfigValidation>();
    var mgr = getConfigStatusManager();
    for (var status : mgr.findLatestConfigStatus("%")) {
      if (pkgName.equalsIgnoreCase(status.getConfigName())) continue;

      var sucStatus = status;
      if (!status.getStatus().equals(ConfigStatus.SUCCESS))
        sucStatus = mgr.findLastSuccessfulConfigStatus(status.getConfigName());
      if (sucStatus == null) continue;

      var tgtHandlers = getConfigHandlers(status.getConfigName());
      if (tgtHandlers.isEmpty()) continue;

      for (var srcH : hdls) {
        for (var tgtH : tgtHandlers) {
          var validates = srcH.validate(tgtH);
          updateValidateResult(pkgName, status.getConfigName(), validates);
          result.addAll(validates);
        }
      }
    }
    return result;
  }

  private void updateValidateResult(
      String myPkgName, String tgtPkgName, List<PSConfigValidation> validates) {
    for (var validate : validates) {
      validate.setPkgName(myPkgName);
      validate.setOtherPkgName(tgtPkgName);
    }
  }

  private List<IPSConfigHandler> getConfigHandlers(String configName) {
    var mapper = new PSConfigMapper();
    var cfg = getLastSuccessConfig(configName);
    if (cfg == null) return Collections.emptyList();
    var cfgDef = cfg.getFirst();
    var props = cfg.getSecond();
    if (cfgDef == null || props.isEmpty()) return Collections.emptyList();

    try (var cfgDefFile = getTempConfigDefFile(configName, cfgDef)) {
      var emptyProps = Collections.<String, Object>emptyMap();
      return mapper.getResolvedHandlers(cfgDefFile.getAbsolutePath(), props, props, emptyProps);
    } catch (Exception e) {
      ms_logger.error(
          "Failed to get handlers from last success configuration of package {} Error: {}",
          configName,
          PSExceptionUtils.getMessageForLog(e));
    }
    return Collections.emptyList();
  }

  /**
   * De-applies the supplied configuration.
   *
   * @param cfgName the name of the configuration to de-apply, never <code>null</code> or empty.
   */
  @Override
  public void deApplyConfiguration(String cfgName) {
    if (StringUtils.isBlank(cfgName))
      throw new IllegalArgumentException("cfgName must not be null");

    ms_logger.info("Reverting configuration file: {}", cfgName);

    var mgr = getConfigStatusManager();
    var cfgStatus = mgr.findLastSuccessfulConfigStatus(cfgName);
    Optional<String> configDef = cfgStatus == null ? Optional.empty() : cfgStatus.getConfigDef();
    if (configDef.isEmpty() || StringUtils.isBlank(configDef.get())) return;

    Optional<String> defaultCfgOpt =
        cfgStatus == null ? Optional.empty() : cfgStatus.getDefaultConfig();
    Optional<String> localCfgOpt =
        cfgStatus == null ? Optional.empty() : cfgStatus.getLocalConfig();
    if (defaultCfgOpt.isEmpty() || localCfgOpt.isEmpty()) return;

    try (var cfgFile = getTempConfigDefFile(cfgName, configDef.get())) {
      deApplyConfiguration(
          cfgName,
          cfgFile.getAbsolutePath(),
          new ByteArrayInputStream(defaultCfgOpt.get().getBytes(StandardCharsets.UTF_8)),
          new ByteArrayInputStream(localCfgOpt.get().getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      ms_logger.error(
          "Failed to revert configuration in file: {} Error: {}",
          cfgName,
          PSExceptionUtils.getMessageForLog(e));
    }
  }

  /**
   * De-applies the supplied configuration using the supplied config definition, default, and local
   * config input streams.
   *
   * @param configName the name of the configuration to de-apply, never <code>null</code> or empty.
   * @param configDefPath the path of the configuration definition, may not be <code>null</code> or
   *     empty.
   * @param defaultCfg the default config input stream, may not be <code>null</code>.
   * @param localCfg the local config input stream, may not be <code>null</code>.
   */
  public void deApplyConfiguration(
      String configName, String configDefPath, InputStream defaultCfg, InputStream localCfg)
      throws PSNotFoundException {
    Objects.requireNonNull(defaultCfg, "defaultCfg must not be null");
    Objects.requireNonNull(localCfg, "localCfg must not be null");
    if (StringUtils.isBlank(configName))
      throw new IllegalArgumentException("configName must not be null or empty");
    if (StringUtils.isBlank(configDefPath))
      throw new IllegalArgumentException("configDefPath must not be null or empty");

    notifyPreConfig(configName);

    var status = ConfigStatus.FAILURE;
    try {
      ms_logger.info("De-applying config for package \"{}\"...\n", configName);

      var normalizer = new PSConfigNormalizer();
      var defaultProps = normalizer.getNormalizedMap(defaultCfg);
      var localProps = normalizer.getNormalizedMap(localCfg);

      var curProps = applyDefaultProps(localProps, defaultProps);
      var prevProps = Collections.<String, Object>emptyMap();

      var mapper = new PSConfigMapper();
      var cfgHandlers = mapper.getResolvedHandlers(configDefPath, curProps, curProps, prevProps);

      var merger = new PSConfigMerger();
      var mergeResults = merger.merge(cfgHandlers, !prevProps.isEmpty(), false);

      if (mergeResults.getSecond() != null) {
        status = ConfigStatus.FAILURE;
        notifyConfigChanges(mergeResults.getFirst(), status);
        throw mergeResults.getSecond();
      } else {
        status = ConfigStatus.SUCCESS;
        notifyConfigChanges(mergeResults.getFirst(), status);
        saveConfigStatus(configName, status);
      }

      ms_logger.info("Finished reverting config for package {}", configName);

    } catch (Exception e) {
      ms_logger.error(
          "Failed to revert config for package {}. Error: {}",
          configName,
          PSExceptionUtils.getMessageForLog(e));
      saveConfigStatus(configName, status);
      throw new PSConfigException(e);
    }
  }

  private void notifyConfigChanges(Collection<IPSGuid> ids, ConfigStatus status)
      throws PSNotFoundException {
    for (var ls : m_configChangeListeners) {
      ls.configChanged(ids, status);
    }
  }

  private void notifyPreConfig(String name) throws PSNotFoundException {
    for (var ls : m_configChangeListeners) {
      ls.preConfiguration(name);
    }
  }

  private Map<String, Object> applyDefaultProps(
      Map<String, Object> localConfig, Map<String, Object> defaultConfig) {
    var nm = new HashMap<String, Object>();
    nm.putAll(defaultConfig);
    nm.putAll(localConfig);
    return nm;
  }

  private Map<String, Object> getNewProps(File localConfigFile, Map<String, Object> defaultProps) {
    var safeLocal = requireConfigFileUnderRxDir(localConfigFile);
    try (var locConfigIs = new FileInputStream(safeLocal)) { // codeql[java/path-injection]
      var normalizer = new PSConfigNormalizer();
      var localProps = normalizer.getNormalizedMap(locConfigIs);
      return applyDefaultProps(localProps, defaultProps);
    } catch (IOException | JAXBException e) {
      throw new PSConfigException(e);
    }
  }

  private PSPair<String, Map<String, Object>> getLastSuccessConfig(String configName) {
    try {
      var mgr = getConfigStatusManager();
      var sucCfg = mgr.findLastSuccessfulConfigStatus(configName);
      if (sucCfg != null) {
        Optional<String> defOpt = sucCfg.getConfigDef();
        if (defOpt.isPresent() && StringUtils.isNotBlank(defOpt.get())) {
          String def = defOpt.get();
          var results =
              applyDefaultProps(
                  normalizeConfig(sucCfg.getLocalConfig().orElse("")),
                  normalizeConfig(sucCfg.getDefaultConfig().orElse("")));
          return new PSPair<String, Map<String, Object>>(def, results);
        }
      }
    } catch (Exception e) {
      var msg =
          "Failed to get last successfully applied configuration for configuration({0}), full"
              + " configuration will be applied.";
      ms_logger.warn(MessageFormat.format(msg, configName), e);
    }
    return null;
  }

  private Map<String, Object> normalizeConfig(String config) throws JAXBException {
    if (StringUtils.isBlank(config)) return new HashMap<>();
    var normalizer = new PSConfigNormalizer();
    return normalizer.getNormalizedMap(
        new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)));
  }

  private void saveConfigStatus(String pkgName, ConfigStatus status) {
    var mgr = getConfigStatusManager();
    var cfgStatus = mgr.createConfigStatus(pkgName);
    var localConfig = getConfigContent(ConfigTypes.LOCAL_CONFIG, pkgName);
    var defaultConfig = getConfigContent(ConfigTypes.DEFAULT_CONFIG, pkgName);
    var configDef = getConfigContent(ConfigTypes.CONFIG_DEF, pkgName);
    cfgStatus.setLocalConfig(localConfig);
    cfgStatus.setDefaultConfig(defaultConfig);
    cfgStatus.setConfigDef(configDef);
    cfgStatus.setDateApplied(new Date());
    cfgStatus.setStatus(status);

    mgr.saveConfigStatus(cfgStatus);
  }

  private String getConfigContent(ConfigTypes type, String pkgName) {
    var file = getConfigFile(type, pkgName);
    var configuration = "";
    if (file.exists()) { // codeql[java/path-injection]
      try {
        configuration = IOTools.getFileContent(file);
      } catch (Exception e) {
        var msg =
            "Failed to read the configuration file \""
                + file.getAbsolutePath()
                + "\" for package ({0})";
        ms_logger.warn(MessageFormat.format(msg, pkgName), e);
      }
    }
    return configuration;
  }

  /**
   * Returns the configuration file for the supplied type and package.
   *
   * @param type the configuration type, never <code>null</code>.
   * @param packageName the package name, never <code>null</code> or empty.
   * @return the configuration file, never <code>null</code>.
   */
  @Override
  public File getConfigFile(ConfigTypes type, String packageName) {
    Objects.requireNonNull(type, "type cannot be null.");
    if (StringUtils.isBlank(packageName))
      throw new IllegalArgumentException("packageName cannot be null or empty.");
    PSPathInjectionGuard.requireSafeFileName(packageName);

    var sb = new StringBuilder();
    var postfix = "";
    sb.append(CONFIG_FILE_BASE);
    if (type == ConfigTypes.LOCAL_CONFIG) {
      sb.append("LocalConfigs/");
      postfix = LOCAL_CONFIG_FILE_SUFFIX;
    } else if (type == ConfigTypes.DEFAULT_CONFIG) {
      sb.append("DefaultConfigs/");
      postfix = DEFAULT_CONFIG_FILE_SUFFIX;
    } else if (type == ConfigTypes.CONFIG_DEF) {
      sb.append("ConfigDefs/");
      postfix = CONFIG_DEF_FILE_SUFFIX;
    } else if (type == ConfigTypes.VISIBILITY) {
      sb.append("Visibility/");
      postfix = VISIBILITY_FILE_SUFFIX;
    }
    sb.append(packageName);
    sb.append(postfix);
    sb.append(".xml");

    return requireConfigFileUnderRxDir(new File(PSServer.getRxDir(), sb.toString()));
  }

  /**
   * Contains {@code file} under the server Rx directory (CodeQL {@code java/path-injection}
   * #2039–#2044).
   */
  static File requireConfigFileUnderRxDir(File file) {
    Objects.requireNonNull(file, "file must not be null");
    return PSPathInjectionGuard.requireUnderBase(PSServer.getRxDir(), file.getPath());
  }

  /**
   * Returns the configuration registration manager.
   *
   * @return the configuration registration manager, may be <code>null</code> before Spring
   *     dependency injection has completed.
   */
  @Override
  public IPSConfigRegistrationMgr getConfigRegistrationMgr() {
    return m_configRegMgr;
  }

  /**
   * Sets the configuration registration manager.
   *
   * @param mgr the configuration registration manager, may not be <code>null</code>.
   */
  public void setConfigRegistrationService(IPSConfigRegistrationMgr mgr) {
    Objects.requireNonNull(mgr, "mgr must not be null");
    m_configRegMgr = mgr;
  }

  /**
   * Returns the configuration status manager.
   *
   * @return the configuration status manager, may be <code>null</code>.
   */
  public IPSConfigStatusMgr getConfigStatusManager() {
    return m_configStatusMgr;
  }

  /**
   * Sets the configuration status manager.
   *
   * @param mgr the configuration status manager, may be <code>null</code>.
   */
  public void setConfigStatusManager(IPSConfigStatusMgr mgr) {
    m_configStatusMgr = mgr;
  }

  @Override
  public List<PSConfigStatus> getConfigStatus(String configName) {
    var mgr = getConfigStatusManager();
    return mgr.findLatestConfigStatus(configName);
  }

  @Override
  public void addConfigChangeListener(IPSConfigChangeListener listener) {
    Objects.requireNonNull(listener, "listener may not be null");
    m_configChangeListeners.add(listener);
  }

  /**
   * Initializes the community visibility file for the supplied package.
   *
   * @param pkgName the package name, never <code>null</code> or empty.
   */
  @Override
  public void initVisibility(String pkgName) {
    var f = getConfigFile(ConfigTypes.VISIBILITY, pkgName);
    if (!f.exists()) PSConfigUtils.saveObjectToFile(new HashSet<String>(), f);
  }

  /**
   * Loads the community visibility list for the supplied package.
   *
   * @param pkgName the package name, never <code>null</code> or empty.
   * @return the list of community names, never <code>null</code>, may be empty.
   */
  @Override
  public Collection<String> loadCommunityVisibility(String pkgName) {
    var f = getConfigFile(ConfigTypes.VISIBILITY, pkgName);
    if (!f.exists()) {
      return Collections.emptySet();
    }
    var loaded = (Collection<String>) PSConfigUtils.loadObjectFromFile(f);
    return loaded == null ? Collections.emptySet() : loaded;
  }

  /**
   * Saves the community visibility list for the supplied package.
   *
   * @param communities the list of community names, never <code>null</code>.
   * @param pkgName the package name, never <code>null</code> or empty.
   * @param isReplace if <code>true</code> replaces the existing list, otherwise merges with it.
   */
  @Override
  public void saveCommunityVisibility(
      Collection<String> communities, String pkgName, boolean isReplace) {
    var f = getConfigFile(ConfigTypes.VISIBILITY, pkgName);
    var commSet = new HashSet<>(communities);
    if (!isReplace) {
      commSet.addAll(loadCommunityVisibility(pkgName));
    }
    PSConfigUtils.saveObjectToFile(commSet, f);
  }

  /** Suffix used for the config definition file. */
  private static final String CONFIG_DEF_FILE_SUFFIX = "_configDef";

  /** Suffix used for the local config file. */
  private static final String LOCAL_CONFIG_FILE_SUFFIX = "_localConfig";

  /** Suffix used for the default config file. */
  private static final String DEFAULT_CONFIG_FILE_SUFFIX = "_defaultConfig";

  /** Suffix used for the visibility file. */
  private static final String VISIBILITY_FILE_SUFFIX = "_visibility";

  /** Directory name used to back up local config files. */
  public static final String LOCAL_CONFIG_BACKUP_DIR = "Backup";

  /** Base path for config files relative to the Rhythmyx root. */
  public static final String CONFIG_FILE_BASE = "rxconfig/Packages/";

  private static final Logger ms_logger = LogManager.getLogger("PSConfigService");

  private IPSConfigRegistrationMgr m_configRegMgr = null;
  private IPSConfigStatusMgr m_configStatusMgr = null;
  private final List<IPSConfigChangeListener> m_configChangeListeners = new ArrayList<>();
}
