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

import com.percussion.utils.container.config.model.impl.BaseContainerUtils;
import java.nio.file.Path;
import java.util.function.Supplier;

/** Default implementation of configuration context. */
public class DefaultConfigurationContextImpl
    extends ConfigurationContextAbstract<DefaultConfigurationContextImpl, BaseContainerUtils>
    implements ConfigurationCtx {

  private Path rootDir;
  private String encKey;

  /**
   * Create a context with the given root directory and encryption key.
   *
   * @param path the root directory, may not be <code>null</code>.
   * @param key the encryption key, may be <code>null</code>.
   */
  public DefaultConfigurationContextImpl(Path path, String key) {
    this(path, key, BaseContainerUtils::new);
  }

  /**
   * Create a context with the given root directory, encryption key and constructor.
   *
   * @param path the root directory, may not be <code>null</code>.
   * @param key the encryption key, may be <code>null</code>.
   * @param ctor the constructor supplier, may not be <code>null</code>.
   */
  public DefaultConfigurationContextImpl(Path path, String key, Supplier<BaseContainerUtils> ctor) {
    super(ctor);
    this.rootDir = path;
    this.encKey = key;
  }

  @Override
  protected DefaultConfigurationContextImpl self() {
    return this;
  }

  /**
   * Get the root directory.
   *
   * @return the root directory, may be <code>null</code>.
   */
  public Path getRootDir() {
    return rootDir;
  }

  /**
   * Get the encryption key.
   *
   * @return the encryption key, may be <code>null</code>.
   */
  public String getEncKey() {
    return encKey;
  }
}
