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
package com.percussion.pso.syndication;

import com.rometools.modules.mediarss.types.Credit;

/***
 *
 * &lt;media:credit&gt;
 *
 * Notable entity and the contribution to the creation of the media object. Current entities can include people, companies, locations, etc. Specific entities can have multiple roles, and several entities can have the same role. These should appear as distinct &lt;media:credit&gt; elements. It has 2 optional attributes.
 *
 * &lt;media:credit role="producer" scheme="urn:ebu"&gt;entity name&lt;/media:credit&gt;
 *
 * role specifies the role the entity played. Must be lowercase. It is an optional attribute.
 *
 * scheme is the URI that identifies the role scheme. It is an optional attribute. If this attribute is not included, the default scheme is 'urn:ebu'. See: European Broadcasting Union Role Codes.
 *
 * Example roles:
 *
 * actor
 * anchor person
 * author
 * choreographer
 * composer
 * conductor
 * director
 * editor
 * graphic designer
 * grip
 * illustrator
 * lyricist
 * music arranger
 * music group
 * musician
 * orchestra
 * performer
 * photographer
 * producer
 * reporter
 * vocalist
 *
 * @author natechadwick
 *
 * <p>&lt;media:credit&gt;
 *
 * <p>Notable entity and the contribution to the creation of the media object. Current entities can
 * include people, companies, locations, etc. Specific entities can have multiple roles, and several
 * entities can have the same role. These should appear as distinct &lt;media:credit&gt; elements. It has
 * 2 optional attributes.
 *
 * <p>&lt;media:credit role="producer" scheme="urn:ebu"&gt;entity name&lt;/media:credit&gt;
 *
 * <p>role specifies the role the entity played. Must be lowercase. It is an optional attribute.
 *
 * <p>scheme is the URI that identifies the role scheme. It is an optional attribute. If this
 * attribute is not included, the default scheme is 'urn:ebu'. See: European Broadcasting Union Role
 * Codes.
 *
 * <p>Example roles:
 *
 * <p>actor anchor person author choreographer composer conductor director editor graphic designer
 * grip illustrator lyricist music arranger music group musician orchestra performer photographer
 * producer reporter vocalist
 *
 * @author natechadwick
 */
public class PSSynFeedCredit {

  private Credit credit;

  /**
   * Returns the name.
   *
   * @return the result
   */
  public String getName() {
    return credit.getName();
  }

  /**
   * Returns the role.
   *
   * @return the result
   */
  public String getRole() {
    return credit.getRole();
  }

  /**
   * Returns the scheme.
   *
   * @return the result
   */
  public String getScheme() {
    return credit.getScheme();
  }

  /**
   * Creates a new PSSynFeedCredit.
   *
   * @param arg the arg
   */
  public PSSynFeedCredit(Credit arg) {
    credit = arg;
  }

  /***
   * Returns in &lt;Role&gt;: &lt;Name&gt; format.
   * @return the result
   */
  @Override
  public String toString() {
    String ret = "";

    if (credit.getRole() != null && !credit.getRole().equals("")) {
      ret = credit.getRole();
    }

    if (credit.getName() != null && !credit.getName().equals("")) {
      if (!ret.equals("")) {
        ret = ret.concat(": " + credit.getName());
      } else {
        ret = credit.getName();
      }
    }

    return ret;
  }
}
