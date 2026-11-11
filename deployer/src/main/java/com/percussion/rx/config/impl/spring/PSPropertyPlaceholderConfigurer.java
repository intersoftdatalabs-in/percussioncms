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

import com.percussion.rx.config.IPSBeanProperties;
import com.percussion.rx.config.PSBeanPropertiesLocator;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * Resolves {@code ${...}} placeholders against local properties first, then the
 * {@link IPSBeanProperties} configuration store, and finally system properties and
 * environment variables.
 *
 * <p>This is the Spring 7 migration of the former {@code PropertyPlaceholderConfigurer}
 * base class (deprecated since Spring 5.2 {@code forRemoval=true}, slated for removal
 * in Spring 8). The {@link IPSBeanProperties} fallback is contributed as a
 * {@link PropertySource} slotted directly after the local-properties source, which
 * preserves the historical resolution order: local properties win over
 * {@code IPSBeanProperties}, which wins over system properties and environment
 * variables. That matches the old {@code SYSTEM_PROPERTIES_MODE_FALLBACK} default,
 * expressed here via {@code localOverride=true} (local properties take precedence
 * over the environment).
 *
 * <p>One intentional observability change: the old implementation logged a warning
 * for every placeholder missed by both local properties and {@code IPSBeanProperties},
 * even when a system property subsequently resolved it. Misses are now simply passed
 * down the source chain; a placeholder missed by every source still fails fast with
 * an exception naming the placeholder (unless {@code ignoreUnresolvablePlaceholders}
 * is set), as before.
 *
 * @author YuBingChen
 */
public class PSPropertyPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer {

  /**
   * Default constructor for use by Spring.
   *
   * @implNote {@code setLocalOverride} only assigns a boolean flag on the fully
   *     constructed support object and this class has no subclasses in-repo, so
   *     the constructor call cannot observe uninitialized subclass state.
   */
  @SuppressWarnings("this-escape")
  public PSPropertyPlaceholderConfigurer() {
    // Preserve the legacy FALLBACK contract: local properties beat system/environment.
    setLocalOverride(true);
  }

  /**
   * Appends the {@link IPSBeanProperties} fallback source directly after the
   * local-properties source so the historical precedence (local, then bean
   * properties, then environment) holds inside the resolver built by the super
   * class. This is the {@code createPropertyResolver} customization point
   * designed for exactly this kind of extension.
   *
   * @param propertySources the sources assembled by the super class, never
   *     {@code null}
   * @return the resolver to use for placeholder replacement, never {@code null}
   */
  @Override
  protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
    var fallback = new IPSBeanPropertiesPropertySource();
    if (propertySources.contains(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME)) {
      propertySources.addAfter(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, fallback);
    } else {
      // Defensive: only reachable if a caller supplied a fully custom source set
      // via setPropertySources without a local source; keep the fallback last so
      // explicit sources still win.
      propertySources.addLast(fallback);
    }
    return super.createPropertyResolver(propertySources);
  }

  /**
   * {@link PropertySource} that resolves keys against the {@link IPSBeanProperties}
   * singleton. The bean manager is looked up lazily on every call so this source
   * honors late {@code sys_beanProperties} initialization, exactly as the previous
   * per-placeholder override did.
   */
  private static final class IPSBeanPropertiesPropertySource extends PropertySource<Object> {

    /** Name under which this source appears in {@code getAppliedPropertySources}. */
    private static final String BEAN_PROPERTIES_PROPERTY_SOURCE_NAME = "beanProperties";

    IPSBeanPropertiesPropertySource() {
      super(BEAN_PROPERTIES_PROPERTY_SOURCE_NAME, new Object());
    }

    @Override
    public boolean containsProperty(String name) {
      return PSBeanPropertiesLocator.getBeanProperties().getProperty(name) != null;
    }

    @Override
    public String getProperty(String name) {
      // Same accessor (including its String-cast contract) as the previous
      // resolvePlaceholder override used.
      return PSBeanPropertiesLocator.getBeanProperties().getString(name);
    }
  }
}
