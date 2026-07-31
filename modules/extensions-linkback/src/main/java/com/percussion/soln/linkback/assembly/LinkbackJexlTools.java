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
package com.percussion.soln.linkback.assembly;

import com.percussion.extension.*;
import com.percussion.soln.linkback.codec.LinkbackTokenCodec;
import com.percussion.soln.linkback.codec.impl.StringLinkBackTokenImpl;
import com.percussion.soln.linkback.utils.LinkbackUtils;
import java.io.File;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** JEXL function to encode assembly parameters into a linkback token. */
public class LinkbackJexlTools implements IPSJexlExpression {

  private static final Logger log = LogManager.getLogger(LinkbackJexlTools.class);

  private LinkbackTokenCodec linkbackCodec = null;

  /** Creates a JEXL tool with no configured codec. */
  public LinkbackJexlTools() {}

  /**
   * Encodes assembly parameters into a linkback token.
   *
   * @param map the parameters to encode
   * @return the encoded linkback token
   */
  @IPSJexlMethod(
      description = "encode a map of parameters into a linkback token.",
      params = {@IPSJexlParam(name = "map", description = "map of <String, Object>")})
  public String encode(Map<String, Object> map) {
    return linkbackCodec.encode(map);
  }

  /**
   * Returns the request parameter name used for linkback tokens.
   *
   * @return the request parameter name
   */
  public String getLinkbackParamName() {
    return LinkbackUtils.LINKBACK_PARAM_NAME;
  }

  /**
   * Returns the codec used to encode linkback tokens.
   *
   * @return the codec used to encode linkback tokens
   */
  public LinkbackTokenCodec getLinkbackCodec() {
    return linkbackCodec;
  }

  /**
   * Sets the codec used to encode linkback tokens.
   *
   * @param linkbackCodec the codec to use
   */
  public void setLinkbackCodec(LinkbackTokenCodec linkbackCodec) {
    this.linkbackCodec = linkbackCodec;
  }

  /**
   * Initializes the JEXL tool and creates the default codec when none is configured.
   *
   * @param arg0 the extension definition
   * @param arg1 the extension directory
   * @throws PSExtensionException if extension initialization fails
   */
  public void init(IPSExtensionDef arg0, File arg1) throws PSExtensionException {
    if (linkbackCodec == null) {
      log.debug("create a default codec");
      linkbackCodec = new StringLinkBackTokenImpl();
    }
  }
}
