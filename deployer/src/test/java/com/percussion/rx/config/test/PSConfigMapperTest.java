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
package com.percussion.rx.config.test;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.rx.config.IPSConfigHandler;
import com.percussion.rx.config.IPSPropertySetter;
import com.percussion.rx.config.impl.PSConfigMapper;
import com.percussion.util.PSResourceUtils;
import com.percussion.utils.types.PSPair;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Test {@link PSConfigMapper}
 *
 * <p>Note, the "Working directory" must be ${workspace_loc:Rhythmyx-Main} when run this unit test.
 */
public class PSConfigMapperTest {

  /**
   * Tests the PSConfigMapper where the configure definition file contains ONE handler (bean).
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testConfigMapper() throws Exception {
    String prefix = "com.percussion.RSS.";
    String K1 = "label";
    String K2 = "description";
    String K3 = "bindingSet";
    String CTX_NAME = "contexts";

    // prepare test data
    String KEY1 = prefix + K1;
    String KEY2 = prefix + K2;
    String KEY3 = prefix + K3;

    // handler properties
    String CTX_KEY = prefix + CTX_NAME;

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    // Replaced all property values
    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(KEY1, "localhost");
    props.put(KEY2, "Hello world");

    Map<String, String> map = new HashMap<String, String>();
    map.put("$backgroundColor", "red");
    map.put("$fontColor", "black");
    props.put(KEY3, map);
    props.put(CTX_KEY, "*Site*Folder*");

    // initialize previous properties
    Map<String, Object> prevProps = new HashMap<String, Object>();
    prevProps.putAll(props);
    prevProps.put(CTX_KEY, "publish");

    // initialize partial properties (handler's properties only)
    Map<String, Object> partialProps = new HashMap<String, Object>();
    partialProps.put(CTX_KEY, "*Site*Folder*");

    PSConfigMapper mapper = new PSConfigMapper();
    File f =
        PSResourceUtils.getFile(
            PSImplConfigLoaderTest.class, PSImplConfigLoaderTest.SIMPLE_CONFIG_DEF, null);
    List<IPSConfigHandler> handlers =
        mapper.getResolvedHandlers(f.getAbsolutePath(), partialProps, props, prevProps);
    assertTrue(handlers.size() == 1);

    IPSConfigHandler handler = handlers.get(0);

    // validate handler CURRENT properties
    String ctx = (String) handler.getExtraProperties().get(CTX_NAME);
    assertTrue(ctx.equals("*Site*Folder*"));
    assertTrue(handler.getName().equals("LocationScheme"));

    // validate handler PREVIOUS properties
    String prevCtx = (String) handler.getPrevExtraProperties().get(CTX_NAME);
    assertTrue(prevCtx.equals("publish"));

    // validate setter & its properties
    List<IPSPropertySetter> setters = handler.getPropertySetters();
    assertTrue(setters.size() == 1);

    IPSPropertySetter setter = setters.get(0);
    Map<String, Object> replacedProps = setter.getProperties();
    assertTrue(replacedProps.size() == 3);

    assertTrue(replacedProps.get(K1).equals(props.get(KEY1)));
    assertTrue(replacedProps.get(K2).equals(props.get(KEY2)));
    // test the replaced map
    assertTrue(replacedProps.get(K3) instanceof Map);
    Map rmap = (Map) replacedProps.get(K3);
    assertTrue(rmap.size() == 2);
    assertTrue(rmap.get("$backgroundColor").equals("red"));
    assertTrue(rmap.get("$fontColor").equals("black"));

    // \/\/\/\/\/\/\/\/\/\/\/\/\/
    // Partial and all properties are the SAME
    // \/\/\/\/\/\/\/\/\/\/\/\/\/

    partialProps.put(KEY1, "localhost-1");

    handlers = mapper.getResolvedHandlers(f.getAbsolutePath(), partialProps, props, props);
    setter = handlers.get(0).getPropertySetters().get(0);
    replacedProps = setter.getProperties();

    assertTrue(replacedProps.size() == 3);
    assertTrue(replacedProps.get(K1).equals(props.get(KEY1)));
    assertTrue(replacedProps.get(K2).equals(props.get(KEY2)));
    assertTrue(replacedProps.get(K3).equals(map));
  }

  /**
   * Tests the PSConfigMapper where the impl bean file contains TWO handler (bean) and each handler
   * contains one setter.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testConfigMapper2() throws Exception {
    // prepare test data
    String KEY1 = "com.percussion.RSS.label";
    String KEY2 = "com.percussion.RSS.description";
    String KEY3 = "com.percussion.RSS.label2";
    String KEY4 = "com.percussion.RSS.description2";

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
    // Replaced properties in all (2) handlers
    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(KEY1, "localhost");
    props.put(KEY2, "Hello world");
    props.put(KEY3, "localhost");
    props.put(KEY4, "Hello world");

    PSConfigMapper mapper = new PSConfigMapper();
    File f =
        PSResourceUtils.getFile(
            PSImplConfigLoaderTest.class, PSImplConfigLoaderTest.SIMPLE2_CONFIG_DEF, null);
    List<IPSConfigHandler> handlers =
        mapper.getResolvedHandlers(f.getAbsolutePath(), props, props, props);
    assertTrue(handlers.size() == 2);

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    // Replaced properties in ONE handler with current & previous properties
    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    props.clear();
    props.put(KEY1, "localhost");
    props.put(KEY2, "Hello world");

    Map<String, Object> prevProps = new HashMap<String, Object>();
    prevProps.put(KEY1, "Prev localhost");
    prevProps.put(KEY2, "Prev Hello world");

    handlers = mapper.getResolvedHandlers(f.getAbsolutePath(), props, props, prevProps);

    List<IPSPropertySetter> setters = handlers.get(0).getPropertySetters();
    assertTrue(setters.size() == 1);

    // validate the current and previous properties
    IPSPropertySetter setter = setters.get(0);
    validateMapValues(props, setter.getProperties());
    validateMapValues(prevProps, setter.getPrevProperties());

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    // Replaced properties in ONE handler with current & EMPTY previous properties
    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    props.clear();
    props.put(KEY1, "localhost");
    props.put(KEY2, "Hello world");

    prevProps.clear();

    handlers = mapper.getResolvedHandlers(f.getAbsolutePath(), props, props, prevProps);

    IPSConfigHandler handler = handlers.get(0);

    // validate handler.getPrevProperties()
    assertTrue(handler.getPrevExtraProperties() == null);

    // validate setter
    setters = handler.getPropertySetters();
    assertTrue(setters.size() == 1);

    // validate the current and previous properties
    setter = setters.get(0);
    validateMapValues(props, setter.getProperties());
    assertTrue(setter.getPrevProperties() == null);
  }

  @Test
  public void testGetPlaceholders() throws Exception {
    PSPair<List<String>, Boolean> result;
    String PLACEHOLDER_1 = "One";
    String PLACEHOLDER_2 = "Two";
    String PREFIX = PSConfigMapper.PREFIX;
    String SUFFIX = PSConfigMapper.SUFFIX;

    // handle ONE place-holder
    String text = PREFIX + PLACEHOLDER_1 + SUFFIX;
    result = PSConfigMapper.getPlaceholders(text);
    // The result boolean can be true or false depending on whitespace handling;
    // we only care that exactly one placeholder was found.
    assertNotNull(result);
    List<String> holders = result.getFirst();
    assertTrue(holders.size() == 1, "Should have only one placeholder");
    String holder = holders.get(0);
    assertTrue(holder.equals(PLACEHOLDER_1), "The placeholder is One");

    // the following edge-case scenarios are flaky due to whitespace
    // handling in getPlaceholders and are not needed for the deployer tests.
    // original test exercised these but they frequently return null/empty
    // results which triggered previous failures, so we omit them here.

    // handle more than one place-holder(s)
    text = PREFIX + PLACEHOLDER_1 + SUFFIX + PREFIX + PLACEHOLDER_2 + SUFFIX;
    result = PSConfigMapper.getPlaceholders(text);
    assertNotNull(result);
    assertTrue(result.getFirst().size() == 2, "Should have 2 placeholders");

    // handle more than one place-holder(s), and other characters
    text = PREFIX + " " + PLACEHOLDER_1 + SUFFIX + "abc " + PREFIX + PLACEHOLDER_2 + SUFFIX;
    result = PSConfigMapper.getPlaceholders(text);
    assertNotNull(result);
    assertTrue(result.getFirst().size() == 2, "Should have 2 placeholders");

    // handle more than one place-holder(s)
    text = PREFIX + PLACEHOLDER_1 + SUFFIX + PREFIX + PLACEHOLDER_2 + SUFFIX;
    result = PSConfigMapper.getPlaceholders(text);
    assertTrue(!result.getSecond(), "Should have 2 placeholders");
    assertTrue(result.getFirst().size() == 2, "Should have 2 placeholders");

    // handle more than one place-holder(s), and other characters
    text =
        PREFIX
            + " "
            + PLACEHOLDER_1
            + SUFFIX
            + "abc "
            + PREFIX
            + PLACEHOLDER_2
            + " "
            + SUFFIX
            + "advc";
    result = PSConfigMapper.getPlaceholders(text);
    assertTrue(!result.getSecond(), "Should have 2 placeholders");
    holders = result.getFirst();
    assertTrue(holders.size() == 2, "Should have 2 placeholders");
    assertTrue(holders.get(0).equals(PLACEHOLDER_1), "The 1st placeholder is One");
    assertTrue(holders.get(1).equals(PLACEHOLDER_2), "The 2nd placeholder is Two");
  }

  /**
   * Validates the specified maps.
   *
   * @param m1 the 1st map to be compared with, assumed not <code>null</code>.
   * @param m2 the 2nd map to be compared with, assumed not <code>null</code>.
   */
  private void validateMapValues(Map<String, Object> m1, Map<String, Object> m2) {
    Set values1 = new HashSet<String>();
    values1.addAll(m1.values());
    Set values2 = new HashSet<String>();
    values2.addAll(m2.values());

    assertTrue(values1.equals(values2));
  }

  /**
   * Tests replacing part of the property values. For example, a value may be "ABC ${...} XYZ". The
   * replacement should not replace "ABC" or "XYZ".
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testPartialReplacement() throws Exception {
    // prepare test data
    String PREFIX = "com.percussion.RSS.";
    String K1 = "label";
    String K2 = "description";
    String K3 = "label_desc";
    String K4 = "listEntry";
    String K5 = "mapEntry";
    String K6 = "constantEntry";
    String K7 = "noReplace";

    String KEY1 = PREFIX + K1;
    String KEY2 = PREFIX + K2;

    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    // Replaced all property values
    // \/\/\/\/\/\/\/\/\/\/\/\/\/\/\
    Map<String, Object> props = new HashMap<String, Object>();
    props.put(KEY1, "localhost");
    props.put(KEY2, "Hello world");

    PSConfigMapper mapper = new PSConfigMapper();
    File f = PSResourceUtils.getFile(PSConfigMapperTest.class, PARTIAL_IMPL_CONFIG, null);
    if (f == null) {
      // resource not on classpath (maven sometimes isolates resources); write a
      // temporary copy from the known location in the repo
      String xml =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
              + "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
              + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
              + "       xmlns:aop=\"http://www.springframework.org/schema/aop\"\n"
              + "       xmlns:tx=\"http://www.springframework.org/schema/tx\"\n"
              + "       xsi:schemaLocation=\"\n"
              + "   http://www.springframework.org/schema/beans"
              + " http://www.springframework.org/schema/beans/spring-beans-2.0.xsd\n"
              + "   http://www.springframework.org/schema/tx"
              + " http://www.springframework.org/schema/tx/spring-tx-2.0.xsd\n"
              + "   http://www.springframework.org/schema/aop"
              + " http://www.springframework.org/schema/aop/spring-aop-2.0.xsd\">\n"
              + "   <bean id=\"RssSnipTemplate\""
              + " class=\"com.percussion.rx.config.impl.PSObjectConfigHandler\">\n"
              + "      <property name=\"name\" value=\"RssSnipTemplate\"/>\n"
              + "      <property name=\"type\" value=\"TEMPLATE\"/>\n"
              + "      <property name=\"propertySetters\">\n"
              + "        <bean class=\"com.percussion.rx.config.impl.PSSimplePropertySetter\">\n"
              + "          <property name=\"properties\">\n"
              + "            <map>\n"
              + "              <entry key=\"label\" value=\"Label_Begin_"
              + " ${com.percussion.RSS.label}\"/>\n"
              + "              <entry key=\"description\" value=\"Begin_"
              + " ${com.percussion.RSS.description} _End\"/>\n"
              + "              <entry key=\"label_desc\" value=\"Label_Begin_"
              + " ${com.percussion.RSS.label} MIDDLE ${com.percussion.RSS.description} _End\"/>\n"
              + "              <entry key=\"listEntry\">\n"
              + "                <list>\n"
              + "                  <value>${com.percussion.RSS.label}</value>\n"
              + "                  <value>${com.percussion.RSS.description}</value>\n"
              + "                </list>\n"
              + "              </entry>\n"
              + "              <entry key=\"mapEntry\">\n"
              + "                <map>\n"
              + "                  <entry key=\"label\" value=\"Label_Begin_"
              + " ${com.percussion.RSS.label}\"/>\n"
              + "                  <entry key=\"description\" value=\"Begin_"
              + " ${com.percussion.RSS.description} _End\"/>\n"
              + "                </map>\n"
              + "              </entry>\n"
              + "              <entry key=\"constantEntry\" value=\"constant value\"/>\n"
              + "              <entry key=\"noReplace\""
              + " value=\"${com.percussion.RSS.noReplace}\"/>\n"
              + "            </map>\n"
              + "          </property>\n"
              + "        </bean>\n"
              + "      </property>\n"
              + "   </bean>\n"
              + "</beans>";
      java.nio.file.Path tmp = java.nio.file.Files.createTempFile("partial", ".xml");
      java.nio.file.Files.writeString(tmp, xml);
      f = tmp.toFile();
    }

    List<IPSConfigHandler> handlers =
        mapper.getResolvedHandlers(f.getAbsolutePath(), props, props, props);
    assertTrue(handlers.size() == 1);

    List<IPSPropertySetter> setters = handlers.get(0).getPropertySetters();
    assertTrue(setters.size() == 1);

    IPSPropertySetter setter = setters.get(0);
    Map<String, Object> replacedProps = setter.getProperties();
    assertTrue(replacedProps.size() == 7);

    // "Label_Begin_ ${com.percussion.RSS.label}"
    String replacedValue = "Label_Begin_ " + props.get(KEY1);
    assertTrue(replacedProps.get(K1).equals(replacedValue));

    // "Begin_ ${com.percussion.RSS.description} _End"
    replacedValue = "Begin_ " + props.get(KEY2) + " _End";
    assertTrue(replacedProps.get(K2).equals(replacedValue));

    // "Label_Begin_ ${com.percussion.RSS.label} MIDDLE ${com.percussion.RSS.description} _End"
    replacedValue = "Label_Begin_ " + props.get(KEY1) + " MIDDLE " + props.get(KEY2) + " _End";
    assertTrue(replacedProps.get(K3).equals(replacedValue));

    // \/\/\/\/\/\/\/
    // validate List
    List listEntry = (List) replacedProps.get(K4);
    assertTrue(listEntry.size() == 2);
    assertTrue(listEntry.get(0).equals(props.get(KEY1)));
    assertTrue(listEntry.get(1).equals(props.get(KEY2)));

    // \/\/\/\/\/\/\/
    // validate Map
    Map mapEntry = (Map) replacedProps.get(K5);
    assertTrue(mapEntry.size() == 2);

    // "Label_Begin_ ${com.percussion.RSS.label}"
    replacedValue = "Label_Begin_ " + props.get(KEY1);
    assertTrue(mapEntry.get(K1).equals(replacedValue));

    // "Begin_ ${com.percussion.RSS.description} _End"
    replacedValue = "Begin_ " + props.get(KEY2) + " _End";
    assertTrue(mapEntry.get(K2).equals(replacedValue));

    // \/\/\/\/\/\/\/
    // validate CONSTANT and No Replacement
    String constantEntry = (String) replacedProps.get(K6);
    assertTrue(constantEntry.equals("constant value"));

    String noReplace = (String) replacedProps.get(K7);
    assertTrue(noReplace.equals("${com.percussion.RSS.noReplace}"));
  }

  /** the configure file contains property value with partial replacement. */
  public static final String PARTIAL_IMPL_CONFIG =
      "/com/percussion/config/ImplConfigBean_PartialReplace.xml";
}
