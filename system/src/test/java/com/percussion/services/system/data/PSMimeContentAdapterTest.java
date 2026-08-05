/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.services.system.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.content.IPSMimeContentTypes;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.util.PSCharSetsConstants;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.tools.PSTestUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link PSMimeContentAdapter} class (programming interface + buffering). */
class PSMimeContentAdapterTest {

  /**
   * Tests the programming interface.
   *
   * @throws Exception If the test fails
   */
  @Test
  void testInterface() throws Exception {
    String data = "some content...";
    ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

    PSMimeContentAdapter content = new PSMimeContentAdapter();
    // test defaults
    assertEquals(PSCharSetsConstants.rxStdEnc(), content.getCharacterEncoding());
    assertEquals(IPSMimeContentTypes.MIME_TYPE_OCTET_STREAM, content.getMimeType());
    assertEquals(IPSMimeContentTypes.MIME_ENC_BASE64, content.getTransferEncoding());
    assertNotNull(content.getContent());
    assertEquals(-1, content.getContentLength());

    assertThrows(IllegalStateException.class, content::getGUID);
    assertThrows(IllegalStateException.class, content::getName);

    // test setters
    PSTestUtils.testSetter(content, "GUID", null, IPSGuid.class, true);
    PSTestUtils.testSetter(
        content, "GUID", new PSGuid(PSTypeEnum.INTERNAL, 0), IPSGuid.class, true);
    PSTestUtils.testSetter(
        content, "GUID", new PSGuid(PSTypeEnum.CONFIGURATION, 123), IPSGuid.class, false);
    PSTestUtils.testSetter(content, "Name", null, true);
    PSTestUtils.testSetter(content, "Name", "", true);
    PSTestUtils.testSetter(content, "Name", "test", false);
    // Null content clears the buffer (BeanUtils-safe for XML restore property order).
    content.setContent(null);
    assertNotNull(content.getContent());
    assertEquals(0, content.getContent().readAllBytes().length);
    PSTestUtils.testSetter(content, "Content", in, InputStream.class, false);
    assertEquals(data, new String(content.getContent().readAllBytes(), StandardCharsets.UTF_8));
    // Stream is buffered — second read still returns the payload.
    assertEquals(data, new String(content.getContent().readAllBytes(), StandardCharsets.UTF_8));
    PSTestUtils.testSetter(content, "ContentLength", Long.valueOf(-2), long.class, true);
    PSTestUtils.testSetter(content, "ContentLength", Long.valueOf(100), long.class, false);
    PSTestUtils.testSetter(content, "MimeType", null, true);
    PSTestUtils.testSetter(content, "MimeType", "", true);
    PSTestUtils.testSetter(content, "MimeType", "test", false);

    assertThrows(
        IllegalStateException.class,
        () -> content.setGUID(new PSGuid(PSTypeEnum.CONFIGURATION, 456)));

    PSTestUtils.testSetter(content, "CharacterEncoding", null, true);
    PSTestUtils.testSetter(content, "CharacterEncoding", "", true);

    PSTestUtils.testSetter(content, "AttachmentId", Long.valueOf(1), long.class, false);
    assertNull(content.getContent());
    assertTrue(content.isContentAttached());

    content.setContent(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
    assertEquals(-1, content.getAttachmentId());
  }
}
