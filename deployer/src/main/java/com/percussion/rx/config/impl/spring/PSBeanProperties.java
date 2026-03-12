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
package com.percussion.rx.config.impl.spring;

import com.percussion.rx.config.impl.PSConfigService;
import com.percussion.rx.config.impl.PSConfigUtils;
import com.percussion.server.PSServer;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class manages properties that may defined in default and local configure files.
 *
 * @author YuBingChen
 */
public class PSBeanProperties implements IPSBeanPropertiesInternal {
  /** The default constructor. It loads properties from repository into memory if there are any. */
  public PSBeanProperties() {
    loadProperties();
  }

  /** Loads the properties from file into memory. Does nothing if there is no properties file. */
  private void loadProperties() {
    var f = getPropertiesFile();
    if (!f.exists()) {
      return;
    }
    m_props = (Map<String, Object>) PSConfigUtils.loadObjectFromFile(f);
  }

  /** Saves current properties into the properties file. */
  private void saveProperties() {
    PSConfigUtils.saveObjectToFile(m_props, getPropertiesFile());
  }

  /*
   * //see base class method for details
   */
  @Override
  public List<Object> getList(String name) {
    var v = getProperty(name);
    return (v == null) ? null : (List<Object>) v;
  }

  /*
   * //see base class method for details
   */
  @Override
  public Map<String, Object> getMap(String name) {
    var v = getProperty(name);
    return (v == null) ? null : (Map<String, Object>) v;
  }

  /*
   * //see base class method for details
   */
  @Override
  public Object getProperty(String name) {
    return m_props.get(name);
  }

  /*
   * //see base class method for details
   */
  @Override
  public String getString(String name) {
    var v = getProperty(name);
    return (v == null) ? null : (String) v;
  }

  /*
   * //see base class method for details
   */
  @Override
  public void save(Map<String, Object> props) {
    if (props == null) {
      throw new IllegalArgumentException("props may not be null.");
    }
    m_props.putAll(props);
    saveProperties();
  }

  /**
   * Gets the properties. This is not exposed through the interface, but can be called directly,
   * e.g., from unit tests.
   *
   * @return the properties, never {@code null}, may be empty.
   */
  public Map<String, Object> getProperties() {
    return m_props;
  }

  /**
   * Gets the properties file. This is not exposed through the interface, but can be called
   * directly, e.g., from unit tests.
   *
   * @return the properties file, never {@code null}.
   */
  public File getPropertiesFile() {
    if (m_file != null) {
      return m_file;
    }
    var path = PSServer.getRxDir() + "/" + PSConfigService.CONFIG_FILE_BASE + "/BeanProperties.xml";
    m_file = new File(path);
    return m_file;
  }

  /** All bean properties. Defaults to empty. Never {@code null}. */
  Map<String, Object> m_props = new HashMap<>();

  /** The properties file, default to {@code null}. */
  File m_file = null;

  // Logger for this class (currently unused)
  // private static final Logger ms_log = LogManager.getLogger("PSBeanProperties");
}
