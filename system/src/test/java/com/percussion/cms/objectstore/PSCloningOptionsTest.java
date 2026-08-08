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
package com.percussion.cms.objectstore;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/** Test the cloning options according to the schema defined in schema sys_FolderParameters.xsd. */
public class PSCloningOptionsTest {
  // legacy constructor removed - using @Test methods

  /**
   * Test all public constuctors.
   *
   * @throws Exception for any error.
   */
  @Test
  public void testConstructors() throws Exception {
    Map<Integer, Integer> communityMappings = new HashMap<>();

    // test valid site type
    new PSCloningOptions(
        PSCloningOptions.TYPE_SITE,
        "siteToCopy",
        "siteName",
        "folderName",
        PSCloningOptions.COPY_NO_CONTENT,
        PSCloningOptions.COPYCONTENT_AS_LINK,
        communityMappings);

    // test valid subfolder type
    new PSCloningOptions(
        PSCloningOptions.TYPE_SITE_SUBFOLDER,
        "folderName",
        PSCloningOptions.COPY_NO_CONTENT,
        PSCloningOptions.COPYCONTENT_AS_LINK,
        null);

    // test invalid type
    Exception exception = null;
    try {
      new PSCloningOptions(
          50,
          null,
          "siteToCopy",
          "folderName",
          PSCloningOptions.COPY_NO_CONTENT,
          PSCloningOptions.COPYCONTENT_AS_LINK,
          communityMappings);
    } catch (Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof IllegalArgumentException);

    // test null folderName
    exception = null;
    try {
      new PSCloningOptions(
          PSCloningOptions.TYPE_SITE,
          "siteToCopy",
          "siteName",
          null,
          PSCloningOptions.COPY_NO_CONTENT,
          PSCloningOptions.COPYCONTENT_AS_LINK,
          communityMappings);
    } catch (Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof IllegalArgumentException);

    // test empty folderName
    exception = null;
    try {
      new PSCloningOptions(
          PSCloningOptions.TYPE_SITE,
          "siteToCopy",
          "siteName",
          " ",
          PSCloningOptions.COPY_NO_CONTENT,
          PSCloningOptions.COPYCONTENT_AS_LINK,
          communityMappings);
    } catch (Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof IllegalArgumentException);

    // test invalid copyOptions
    exception = null;
    try {
      new PSCloningOptions(
          PSCloningOptions.TYPE_SITE,
          "siteToCopy",
          "siteName",
          "folderName",
          -1,
          PSCloningOptions.COPYCONTENT_AS_LINK,
          communityMappings);
    } catch (Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof IllegalArgumentException);

    // test invalid copyContentOptions
    exception = null;
    try {
      new PSCloningOptions(
          PSCloningOptions.TYPE_SITE,
          "siteToCopy",
          "siteName",
          "folderName",
          PSCloningOptions.COPY_NO_CONTENT,
          -1,
          communityMappings);
    } catch (Exception e) {
      exception = e;
    }
    assertTrue(exception instanceof IllegalArgumentException);
  }

  /**
   * Test all public methods contracts.
   *
   * @throws Exception for any error.
   */
  @Test
  public void testPublicAPI() throws Exception {
    Map<Integer, Integer> communityMappings = new HashMap<>();
    communityMappings.put(Integer.valueOf(1), Integer.valueOf(2));
    communityMappings.put(Integer.valueOf(3), Integer.valueOf(4));
    communityMappings.put(Integer.valueOf(5), Integer.valueOf(6));

    PSCloningOptions options_1 =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE,
            "siteToCopy",
            "siteName",
            "folderName",
            PSCloningOptions.COPY_NO_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            communityMappings);

    PSCloningOptions options_2 =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folderName",
            PSCloningOptions.COPY_NAVIGATION_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            null);

    assertTrue(!options_1.equals(options_2));

    // test copyFrom
    options_2.copyFrom(options_1);
    assertTrue(options_1.equals(options_2));

    // test clone
    assertTrue(options_1.equals(options_1.clone()));

    // test toXml / fromXml
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCloningOptions options_1_copy = new PSCloningOptions(options_1.toXml(doc), null, null);
    assertTrue(options_1.equals(options_1_copy));

    PSCloningOptions options_3 =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folderName",
            PSCloningOptions.COPY_ALL_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_NEW_COPY,
            null);
    PSCloningOptions options_3_copy = new PSCloningOptions(options_3.toXml(doc), null, null);
    assertTrue(options_3.equals(options_3_copy));

    PSCloningOptions options_4 =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE_SUBFOLDER,
            "folderName",
            PSCloningOptions.COPY_NAVIGATION_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            null);
    options_4.addSiteMapping(Integer.valueOf(100), Integer.valueOf(201));
    options_4.addSiteMapping(Integer.valueOf(101), Integer.valueOf(202));
    options_4.addSiteMapping(Integer.valueOf(102), Integer.valueOf(203));
    PSCloningOptions options_4_copy = new PSCloningOptions(options_4.toXml(doc), null, null);
    assertTrue(options_4.equals(options_4_copy));

    PSCloningOptions options_5 =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE,
            "siteToCopy",
            "siteName",
            "folderName",
            PSCloningOptions.COPY_NO_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            communityMappings);
    options_5.addSiteMapping(Integer.valueOf(100), Integer.valueOf(201));
    options_5.addSiteMapping(Integer.valueOf(101), Integer.valueOf(202));
    options_5.addSiteMapping(Integer.valueOf(102), Integer.valueOf(203));
    PSCloningOptions options_5_copy = new PSCloningOptions(options_5.toXml(doc), null, null);
    assertTrue(options_5.equals(options_5_copy));
  }

  /**
   * Typed community / site mapping getters after generics batch (#2376): maps preserve entries
   * through construction, addSiteMapping, and XML round-trip.
   */
  @Test
  public void testTypedMappingApis() throws Exception {
    Map<Integer, Integer> communities = new HashMap<>();
    communities.put(10, 20);
    communities.put(30, 40);

    PSCloningOptions options =
        new PSCloningOptions(
            PSCloningOptions.TYPE_SITE,
            "srcSite",
            "newSite",
            "folder",
            PSCloningOptions.COPY_NO_CONTENT,
            PSCloningOptions.COPYCONTENT_AS_LINK,
            communities);

    Map<Integer, Integer> gotCommunities = options.getCommunityMappings();
    assertEquals(Integer.valueOf(20), gotCommunities.get(10));
    assertEquals(Integer.valueOf(40), gotCommunities.get(30));
    assertEquals(2, gotCommunities.size());

    options.addSiteMapping(100, 200);
    options.addSiteMapping(101, 201);
    Map<Integer, Integer> sites = options.getSiteMappings();
    assertEquals(Integer.valueOf(200), sites.get(100));
    assertEquals(Integer.valueOf(201), sites.get(101));
    assertEquals(2, sites.size());

    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    PSCloningOptions restored = new PSCloningOptions(options.toXml(doc), null, null);
    assertEquals(options.getCommunityMappings(), restored.getCommunityMappings());
    assertEquals(options.getSiteMappings(), restored.getSiteMappings());
    assertEquals(options, restored);
  }

  // JUnit 3 style suite removed; using JUnit 5 test methods

}
