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
package com.percussion.services.content;

/**
 * Error numbers for use with the bundle
 * <code>PSContentErrorStringBundle.properties</code>.
 *
 * <p><strong>Phase 2b note:</strong> {@link #MISSING_KEYWORD} is package-local ({@code 1}) and is
 * <em>not</em> registered in the flat {@code LegacyErrorCodeRegistry} (would collide with other
 * package-local catalogs such as workflow). Prefer typed {@code ContentErrorCodes} high-level
 * lifecycle codes for intentional content audit emits. Conversion-range content errors live on
 * {@code com.percussion.content.IPSContentErrors} (17001+) and are bridged in {@code
 * ContentErrorCodes}.
 */
public interface IPSContentErrors
{
   /**
    * Missing keyword.
    * <p>
    * The arguments passed in for this message are:
    * <TABLE BORDER="1"><CAPTION>Error Arguments</CAPTION>
    * <TR><TH>Arg</TH><TH>Description</TH></TR>
    * <TR><TD>0</TD><TD>The id of the missing keyword</TD></TR>
    * </TABLE>
    */
   public static final int MISSING_KEYWORD = 1;
}

