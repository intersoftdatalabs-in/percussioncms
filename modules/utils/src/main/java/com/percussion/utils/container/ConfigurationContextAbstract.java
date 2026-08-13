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

import com.percussion.utils.container.config.ContainerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.beanutils.PropertyUtils;

/**
 * Abstract base class for configuration contexts.
 *
 * @param <T> the configuration context type
 * @param <U> the container configuration type
 */
public abstract class ConfigurationContextAbstract<
        T extends ConfigurationCtx, U extends ContainerConfig>
    implements ConfigurationAdaptorComposite<T, U> {

  private final Supplier<U> ctor;

  private U config;

  private List<IPSConfigurationAdapter<T>> adapters = new ArrayList<>();

  /**
   * Constructs a new configuration context with the given constructor.
   *
   * @param ctor the constructor for the configuration object
   */
  public ConfigurationContextAbstract(Supplier<U> ctor) {
    this.ctor = Objects.requireNonNull(ctor);
    config = ctor.get();
  }

  @Override
  public void addConfigurationAdapter(IPSConfigurationAdapter<T> adapter) {
    adapters.add(adapter);
  }

  @Override
  public void load(T ctx) {
    adapters.stream().forEach(c -> c.load(ctx));
  }

  @Override
  public void save(T ctx) {
    adapters.stream().forEach(c -> c.save(ctx));
  }

  @Override
  public U getConfig() {
    return config;
  }

  @Override
  public void load() {
    load(self());
  }

  @Override
  public void save() {
    save(self());
  }

  /**
   * Copies configuration from another context.
   *
   * @param from the source context to copy from
   */
  public void copyFrom(ConfigurationContextAbstract<T, U> from) {
    try {
      this.config = cloneConfig(from.getConfig());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * CRTP self-view. Concrete contexts implement this as {@code return this} so {@link #load()} /
   * {@link #save()} stay typed without an unchecked {@code (T) this} cast.
   *
   * @return {@code this} as {@code T}
   */
  protected abstract T self();

  /**
   * Clone a config bean. Instantiates via this context's {@link #ctor} (same supplier used at
   * construction) and copies properties with {@link PropertyUtils#copyProperties(Object, Object)},
   * matching {@code BeanUtils.cloneBean} without an unchecked {@code (U)} cast.
   *
   * @param source config to clone, never {@code null}
   * @return cloned config as {@code U}
   * @throws Exception if property copy fails
   */
  private U cloneConfig(U source) throws Exception {
    U copy = ctor.get();
    PropertyUtils.copyProperties(copy, source);
    return copy;
  }
}
