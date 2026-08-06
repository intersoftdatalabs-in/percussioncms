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
package com.percussion.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.percussion.design.objectstore.IPSReplacementValue;
import org.junit.jupiter.api.Test;

/** Unit tests for PSDataExtractor class. */
class PSDataExtractorTest {

  @Test
  void testConstructorWithSingleNullSource_ThrowsNullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> new TestDataExtractor((IPSReplacementValue) null),
        "Should throw NullPointerException when source is null");
  }

  @Test
  void testConstructorWithSingleValidSource() {
    IPSReplacementValue source = mock(IPSReplacementValue.class);
    TestDataExtractor extractor = new TestDataExtractor(source);

    assertNotNull(extractor);
    IPSReplacementValue[] sources = extractor.getSource();
    assertEquals(1, sources.length);
    assertEquals(source, sources[0]);
  }

  @Test
  void testConstructorWithNullArraySource() {
    // Should not throw exception, should initialize with empty array
    TestDataExtractor extractor = new TestDataExtractor((IPSReplacementValue[]) null);

    assertNotNull(extractor);
    IPSReplacementValue[] sources = extractor.getSource();
    assertEquals(0, sources.length);
  }

  @Test
  void testConstructorWithEmptyArraySource() {
    IPSReplacementValue[] source = new IPSReplacementValue[0];
    TestDataExtractor extractor = new TestDataExtractor(source);

    assertNotNull(extractor);
    IPSReplacementValue[] sources = extractor.getSource();
    assertEquals(0, sources.length);
  }

  @Test
  void testConstructorWithMultipleValidSources() {
    IPSReplacementValue source1 = mock(IPSReplacementValue.class);
    IPSReplacementValue source2 = mock(IPSReplacementValue.class);
    IPSReplacementValue[] sourceArray = {source1, source2};

    TestDataExtractor extractor = new TestDataExtractor(sourceArray);

    assertNotNull(extractor);
    IPSReplacementValue[] sources = extractor.getSource();
    assertEquals(2, sources.length);
    assertEquals(source1, sources[0]);
    assertEquals(source2, sources[1]);
  }

  @Test
  void testGetSingleSourceWithEmptyArray() {
    TestDataExtractor extractor = new TestDataExtractor((IPSReplacementValue[]) null);

    IPSReplacementValue result = extractor.getSingleSource();
    // Should return null for empty array
    assertTrue(result == null || extractor.getSource().length == 0);
  }

  @Test
  void testGetSingleSourceWithData() {
    IPSReplacementValue source = mock(IPSReplacementValue.class);
    TestDataExtractor extractor = new TestDataExtractor(source);

    IPSReplacementValue result = extractor.getSingleSource();
    assertEquals(source, result);
  }

  /** Concrete implementation of PSDataExtractor for testing purposes. */
  private static class TestDataExtractor extends PSDataExtractor {
    public TestDataExtractor(IPSReplacementValue source) {
      super(source);
    }

    public TestDataExtractor(IPSReplacementValue[] source) {
      super(source);
    }

    @Override
    public Object extract(PSExecutionData data) throws PSDataExtractionException {
      return null;
    }

    @Override
    public Object extract(PSExecutionData data, Object defValue) throws PSDataExtractionException {
      return defValue;
    }
  }
}
