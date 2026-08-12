// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.parser;

import com.percussion.pagemanagement.data.PSAbstractRegion;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.parser.IPSRegionParser.IPSRegionParserRegionFactory;

/**
 * Adapter for region parser and region factory.
 *
 * @param <REGION> Region type.
 * @param <CODE> Code type.
 * @author adamgent, Sunny Sal
 */
public abstract class PSRegionParserAdapter<
        REGION extends PSAbstractRegion, CODE extends PSRegionCode>
    implements IPSRegionParserRegionFactory<REGION, CODE>, IPSRegionParser<REGION, CODE> {

  /**
   * Lazy: {@link PSRegionParser} takes the factory ({@code this}) as a constructor arg. Creating it
   * in a field initializer would leak {@code this} before the subclass finishes construction
   * ({@code this-escape}). Construction is complete before {@link #parse(String)} runs.
   */
  private PSRegionParser<REGION, CODE> parser;

  private PSRegionParser<REGION, CODE> parser() {
    if (parser == null) {
      parser = new PSRegionParser<>(this);
    }
    return parser;
  }

  @Override
  public PSParsedRegionTree<REGION, CODE> parse(String text) {
    return parser().parse(text);
  }
}
