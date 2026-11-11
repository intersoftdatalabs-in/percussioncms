/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.percussion.rx.config.IPSBeanProperties;
import com.percussion.rx.config.PSBeanPropertiesLocator;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Behavioral proof for the Spring 7 migration of
 * {@link PSPropertyPlaceholderConfigurer} off the {@code forRemoval}
 * {@code PropertyPlaceholderConfigurer} base class (#6032 cluster 1).
 *
 * <p>Pins the historical resolution order through a real
 * {@link GenericApplicationContext} refresh: local properties first, then the
 * {@link IPSBeanProperties} fallback, then system properties/environment, with
 * unresolvable placeholders still failing fast.
 */
@DisplayName("PSPropertyPlaceholderConfigurer resolution order (#6032)")
class PSPropertyPlaceholderConfigurerTest {

  private static final String SYS_KEY = "ps.test.placeholder.syskey";

  @AfterEach
  void restoreStaticState() {
    try {
      beanPropertiesField().set(null, null);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
    System.clearProperty(SYS_KEY);
  }

  @Test
  @DisplayName("local properties resolve placeholders")
  void localPropertiesResolve() {
    var bean = runContext(properties(Map.of("test.local", "local-value")), beanWith("value", "${test.local}"));
    assertEquals("local-value", bean.getValue());
  }

  @Test
  @DisplayName("IPSBeanProperties backs placeholders missed by local properties")
  void beanPropertiesFallback() {
    presetBeanProperties(Map.of("test.fallback", "beanprops-value"));
    var bean = runContext(new Properties(), beanWith("fallback", "${test.fallback}"));
    assertEquals("beanprops-value", bean.getFallback());
  }

  @Test
  @DisplayName("local properties win over IPSBeanProperties and system properties")
  void localBeatsBeanPropertiesAndSystem() {
    presetBeanProperties(Map.of("test.shared", "beanprops-value"));
    System.setProperty("test.shared", "system-value");
    try {
      var bean =
          runContext(
              properties(Map.of("test.shared", "local-value")), beanWith("shared", "${test.shared}"));
      assertEquals("local-value", bean.getShared());
    } finally {
      System.clearProperty("test.shared");
    }
  }

  @Test
  @DisplayName("IPSBeanProperties wins over system properties (legacy FALLBACK order)")
  void beanPropertiesBeatsSystem() {
    presetBeanProperties(Map.of(SYS_KEY, "beanprops-value"));
    System.setProperty(SYS_KEY, "system-value");
    var bean = runContext(new Properties(), beanWith("sys", "${" + SYS_KEY + "}"));
    assertEquals("beanprops-value", bean.getSys());
  }

  @Test
  @DisplayName("system properties resolve placeholders missed by every other source")
  void systemFallback() {
    // Empty bean store: the fallback source abstains (null) so resolution
    // reaches the environment, mirroring a configured-but-silent store.
    presetBeanProperties(Map.of());
    System.setProperty(SYS_KEY, "system-value");
    var bean = runContext(new Properties(), beanWith("sys", "${" + SYS_KEY + "}"));
    assertEquals("system-value", bean.getSys());
  }

  @Test
  @DisplayName("unresolvable placeholders still fail fast")
  void unresolvableThrows() {
    presetBeanProperties(Map.of());
    try (var ctx = new GenericApplicationContext()) {
      ctx.registerBeanDefinition("testBean", beanWith("value", "${test.missing}"));
      ctx.registerBeanDefinition("configurer", configurerDefinition(new Properties()));
      assertThrows(
          org.springframework.beans.factory.BeanDefinitionStoreException.class, ctx::refresh);
    }
  }

  private TestBean runContext(Properties localProps, RootBeanDefinition beanDef) {
    // Local properties travel on the configurer bean itself ("properties"
    // property), mirroring the production user-beans.xml wiring style.
    try (var ctx = new GenericApplicationContext()) {
      ctx.registerBeanDefinition("testBean", beanDef);
      ctx.registerBeanDefinition("configurer", configurerDefinition(localProps));
      ctx.refresh();
      return ctx.getBean("testBean", TestBean.class);
    }
  }

  private RootBeanDefinition beanWith(String property, String expression) {
    var def = new RootBeanDefinition(TestBean.class);
    def.getPropertyValues().add(property, expression);
    return def;
  }

  private RootBeanDefinition configurerDefinition(Properties localProps) {
    var def = new RootBeanDefinition(PSPropertyPlaceholderConfigurer.class);
    def.getPropertyValues().add("properties", localProps);
    return def;
  }

  private Properties properties(Map<String, String> values) {
    var props = new Properties();
    props.putAll(values);
    return props;
  }

  private void presetBeanProperties(Map<String, String> values) {
    try {
      // Exact production field type (IPSBeanProperties); restored in @AfterEach.
      beanPropertiesField().set(null, new MapBackedBeanProperties(values));
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Field beanPropertiesField() {
    try {
      var field = PSBeanPropertiesLocator.class.getDeclaredField("beanProperties");
      field.setAccessible(true);
      return field;
    } catch (NoSuchFieldException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Minimal {@link IPSBeanProperties} fake; production type, no Mockito needed. */
  private static final class MapBackedBeanProperties implements IPSBeanProperties {

    private final Map<String, Object> values = new HashMap<>();

    MapBackedBeanProperties(Map<String, String> values) {
      this.values.putAll(values);
    }

    @Override
    public Map<String, Object> getProperties() {
      return Map.copyOf(values);
    }

    @Override
    public Object getProperty(String name) {
      return values.get(name);
    }

    @Override
    public String getString(String name) {
      var value = values.get(name);
      return value == null ? null : (String) value;
    }

    @Override
    public List<?> getList(String name) {
      return List.of();
    }

    @Override
    public Map<?, ?> getMap(String name) {
      return Map.of();
    }

    @Override
    public void save(Map<String, Object> props) {
      values.putAll(props);
    }
  }

  /** Placeholder consumer with one setter per placeholder under test. */
  public static class TestBean {

    private String value;

    private String fallback;

    private String shared;

    private String sys;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public String getFallback() {
      return fallback;
    }

    public void setFallback(String fallback) {
      this.fallback = fallback;
    }

    public String getShared() {
      return shared;
    }

    public void setShared(String shared) {
      this.shared = shared;
    }

    public String getSys() {
      return sys;
    }

    public void setSys(String sys) {
      this.sys = sys;
    }
  }
}
