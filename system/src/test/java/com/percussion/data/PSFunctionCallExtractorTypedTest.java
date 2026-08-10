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
import static org.junit.jupiter.api.Assertions.assertNull;

import com.percussion.design.objectstore.PSFunctionCall;
import com.percussion.design.objectstore.PSFunctionParamValue;
import com.percussion.design.objectstore.PSTextLiteral;
import java.text.ParseException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for typed collections inside {@link PSFunctionCallExtractor} after rawtypes
 * cleanup. {@link PSFunctionCallExtractor#formatFunctionBody(String, String[])} is exercised
 * directly; construction uses a minimal function-call definition.
 */
@Tag("UnitTest")
class PSFunctionCallExtractorTypedTest {

  @Test
  void formatFunctionBodySubstitutesPositionalArgs() throws ParseException {
    PSFunctionCallExtractor extractor = minimalExtractor();
    String formatted = extractor.formatFunctionBody("UPPER({0})", new String[] {"hello"});
    assertEquals("UPPER(hello)", formatted);
  }

  @Test
  void formatFunctionBodyReturnsNullWhenAnyArgNull() throws ParseException {
    PSFunctionCallExtractor extractor = minimalExtractor();
    assertNull(extractor.formatFunctionBody("F({0},{1})", new String[] {"a", null}));
  }

  @Test
  void formatFunctionBodyHandlesEmptyArgArray() throws ParseException {
    PSFunctionCallExtractor extractor = minimalExtractor();
    assertEquals("NOW()", extractor.formatFunctionBody("NOW()", new String[0]));
  }

  private static PSFunctionCallExtractor minimalExtractor() {
    // Function body from the def is not used by formatFunctionBody; only required for ctor.
    PSFunctionParamValue[] params =
        new PSFunctionParamValue[] {new PSFunctionParamValue(new PSTextLiteral("x"))};
    PSFunctionCall call = new PSFunctionCall("TEST_FN", params, null, null);
    return new PSFunctionCallExtractor(call);
  }
}
