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

import com.rometools.modules.mediarss.types.Restriction;

/***
 * &lt;media:restriction&gt;
 *
 * Allows restrictions to be placed on the aggregator rendering the media in the feed. Currently, restrictions are based on distributor (uri) and country codes. This element is purely informational and no obligation can be assumed or implied. Only one &lt;media:restriction&gt; element of the same type can be applied to a media object - all others will be ignored. Entities in this element should be space separated. To allow the producer to explicitly declare his/her intentions, two literals are reserved: 'all', 'none'. These literals can only be used once. This element has 1 required attribute, and 1 optional attribute (with strict requirements for its exclusion).
 *
 * &lt;media:restriction relationship="allow" type="country"&gt;au us&lt;/media:restriction&gt;
 * relationship indicates the type of relationship that the restriction represents (allow | deny). In the example above, the media object should only be syndicated in Australia and the United States. It is a required attribute.
 *
 * Note: If the "allow" element is empty and the type is relationship is "allow", it is assumed that the empty list means "allow nobody" and the media should not be syndicated.
 *
 * A more explicit method would be:
 *
 * &lt;media:restriction relationship="allow" type="country"&gt;au us&lt;/media:restriction&gt;
 * type specifies the type of restriction (country | uri) that the media can be syndicated. It is an optional attribute; however can only be excluded when using one of the literal values "all" or "none".
 *
 * "country" allows restrictions to be placed based on country code. [ISO 3166]
 *
 * "uri" allows restrictions based on URI. Examples: urn:apple, http://images.google.com, urn:yahoo, etc.
 * @author natechadwick
 *
 * <p>Allows restrictions to be placed on the aggregator rendering the media in the feed. Currently,
 * restrictions are based on distributor (uri) and country codes. This element is purely
 * informational and no obligation can be assumed or implied. Only one &lt;media:restriction&gt; element
 * of the same type can be applied to a media object - all others will be ignored. Entities in this
 * element should be space separated. To allow the producer to explicitly declare his/her
 * intentions, two literals are reserved: 'all', 'none'. These literals can only be used once. This
 * element has 1 required attribute, and 1 optional attribute (with strict requirements for its
 * exclusion).
 *
 * <p>&lt;media:restriction relationship="allow" type="country"&gt;au us&lt;/media:restriction&gt; relationship
 * indicates the type of relationship that the restriction represents (allow | deny). In the example
 * above, the media object should only be syndicated in Australia and the United States. It is a
 * required attribute.
 *
 * <p>Note: If the "allow" element is empty and the type is relationship is "allow", it is assumed
 * that the empty list means "allow nobody" and the media should not be syndicated.
 *
 * <p>A more explicit method would be:
 *
 * <p>&lt;media:restriction relationship="allow" type="country"&gt;au us&lt;/media:restriction&gt; type
 * specifies the type of restriction (country | uri) that the media can be syndicated. It is an
 * optional attribute; however can only be excluded when using one of the literal values "all" or
 * "none".
 *
 * <p>"country" allows restrictions to be placed based on country code. [ISO 3166]
 *
 * <p>"uri" allows restrictions based on URI. Examples: urn:apple, http://images.google.com,
 * urn:yahoo, etc.
 *
 * @author natechadwick
 */
public class PSSynFeedRestriction {

  private Restriction r;

  /**
   * Returns the relationship.
   *
   * @return the result
   */
  public String getRelationship() {

    if (r.getRelationship().equals(Restriction.Relationship.ALLOW)) return "allow";
    else return "deny";
  }

  /**
   * Returns the type.
   *
   * @return the result
   */
  public String getType() {
    if (r.getType().equals(Restriction.Type.COUNTRY)) {
      return "country";
    } else {
      return Restriction.Type.URI.toString();
    }
  }

  /**
   * Returns the value.
   *
   * @return the result
   */
  public String getValue() {
    return r.getValue();
  }

  /**
   * Creates a new PSSynFeedRestriction.
   *
   * @param arg the arg
   */
  public PSSynFeedRestriction(Restriction arg) {
    r = arg;
  }

  /**
   * toString operation.
   *
   * @return the result
   */
  @Override
  public String toString() {
    String ret = "";

    if (r.getRelationship().equals(Restriction.Relationship.ALLOW)) {
      ret = "Allow";
    } else {
      ret = "Deny";
    }
    return ret;
  }
}
