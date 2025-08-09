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

package com.percussion.utils.container;

import com.percussion.utils.container.adapters.JettyDatasourceConfigurationAdapter;
import com.percussion.utils.container.config.model.impl.BaseContainerUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ConfigurationContextTest {

  /**
   * Test loading a Jetty datasource configuration.
   *
   * @throws IOException if an error occurs while reading the configuration files.
   */
  @Test
  public void load1() throws IOException {

    String resourcePath = "/com/percussion/utils/container/jetty/base/etc/perc-ds-derby.properties";
    String dsxml = "/com/percussion/utils/container/jetty/base/etc/perc-ds.xml";

    InputStream is = getClass().getResourceAsStream(resourcePath);
    InputStream dsxmlIs = getClass().getResourceAsStream(dsxml);

    Path root = Files.createTempDirectory("test");
    root.toFile().deleteOnExit();

    Files.createDirectories(root.resolve("jetty/base/etc"));

    Files.copy(is, root.resolve("jetty/base/etc/perc-ds.properties"));
    Files.copy(dsxmlIs, root.resolve("jetty/base/etc/perc-ds.xml"));

    DefaultConfigurationContextImpl ctx = new DefaultConfigurationContextImpl(root, "encKey");

    ctx.addConfigurationAdapter(new JettyDatasourceConfigurationAdapter());

    ctx.load();

    BaseContainerUtils containerUtils = ctx.getConfig();

    Assertions.assertEquals("jdbc/RhythmyxData", containerUtils.getDatasources().get(0).getName());
  }

}
