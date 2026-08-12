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
package com.percussion.share.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.monitor.service.PSMonitor;
import com.percussion.pagemanagement.assembler.PSRenderAsset;
import com.percussion.pagemanagement.data.PSRenderLink;
import com.percussion.pagemanagement.data.PSResourceLinkAndLocation;
import com.percussion.pagemanagement.data.PSResourceLocation;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.sitemanage.data.PSPubInfo;
import com.percussion.sitemanage.data.PSSiteSummary;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral coverage for serial-field residual mitigations (issue #3061): nested types implement
 * {@link Serializable}, runtime-only fields are {@code transient}, and round-trip preserves wire
 * data.
 */
@Tag("UnitTest")
class PSSerialFieldResidualTest {

  @Test
  void mapWrapperPubInfoRenderTypesAreSerializable() {
    assertTrue(Serializable.class.isAssignableFrom(PSMapWrapper.class));
    assertTrue(Serializable.class.isAssignableFrom(PSPubInfo.class));
    assertTrue(Serializable.class.isAssignableFrom(PSRenderLink.class));
    assertTrue(Serializable.class.isAssignableFrom(PSResourceLocation.class));
    assertTrue(Serializable.class.isAssignableFrom(PSWidgetDefinition.WidgetPrefs.class));
    assertTrue(Serializable.class.isAssignableFrom(PSWidgetDefinition.Content.class));
    assertTrue(Serializable.class.isAssignableFrom(PSWidgetDefinition.Code.class));
    assertTrue(Serializable.class.isAssignableFrom(PSMonitor.class));
  }

  @Test
  void pathItemRelatedObjectIsTransient() throws Exception {
    var field = PSPathItem.class.getDeclaredField("relatedObject");
    assertTrue(Modifier.isTransient(field.getModifiers()));
  }

  @Test
  void renderAssetNodeIsTransient() throws Exception {
    var field = PSRenderAsset.class.getDeclaredField("node");
    assertTrue(Modifier.isTransient(field.getModifiers()));
  }

  @Test
  void mapWrapperRoundTripPreservesEntries() throws Exception {
    var wrap = new PSMapWrapper();
    wrap.setEntries(new HashMap<>(Map.of("a", "1", "b", "2")));
    var copy = roundTrip(wrap);
    assertEquals("1", copy.getEntries().get("a"));
    assertEquals("2", copy.getEntries().get("b"));
    assertInstanceOf(HashMap.class, copy.getEntries());
  }

  @Test
  void pubInfoOnSiteSummaryRoundTrip() throws Exception {
    var pub = new PSPubInfo("bucket", "ak", "sk", "us-east-1");
    pub.setArnRole("arn:aws:iam::123:role/r");
    var site = new PSSiteSummary();
    site.setName("site-a");
    site.setPubInfo(pub);

    var copy = roundTrip(site);
    assertEquals("site-a", copy.getName());
    var pubCopy = copy.getPubInfo().orElseThrow();
    assertEquals("bucket", pubCopy.getBucketName());
    assertEquals("us-east-1", pubCopy.getRegionName());
    // Credential fields are transient — must not survive Java serialization.
    assertNull(pubCopy.getAccessKey());
    assertNull(pubCopy.getSecretKey());
    assertNull(pubCopy.getArnRole());
  }

  @Test
  void resourceLinkAndLocationRoundTrip() throws Exception {
    var link = new PSRenderLink();
    link.setUrl("https://example.test/a");
    var loc = new PSResourceLocation();
    loc.setFilePath("/rx_resources/a.css");
    var rll = new PSResourceLinkAndLocation();
    rll.setRenderLink(link);
    rll.setResourceLocation(loc);

    var copy = roundTrip(rll);
    assertEquals("https://example.test/a", copy.getRenderLink().getUrl());
    assertEquals("/rx_resources/a.css", copy.getResourceLocation().getFilePath());
  }

  @Test
  void pathItemRelatedObjectNotSerialized() throws Exception {
    var item = new PSPathItem();
    item.setName("n");
    item.setRelatedObject(new Object());
    var copy = roundTrip(item);
    assertEquals("n", copy.getName());
    assertNull(copy.getRelatedObject());
  }

  @SuppressWarnings("unchecked")
  private static <T> T roundTrip(T value) throws Exception {
    var bos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(bos)) {
      oos.writeObject(value);
    }
    try (var ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
      return (T) ois.readObject();
    }
  }
}
