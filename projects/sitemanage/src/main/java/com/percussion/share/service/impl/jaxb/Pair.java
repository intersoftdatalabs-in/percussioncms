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

package com.percussion.share.service.impl.jaxb;

import com.fasterxml.jackson.annotation.JsonRootName;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Represents a pair of values, with optional pvalue1/pvalue2 and value1/value2. Sunny Sal says:
 * "Pair class, now with Java 11 flair!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {})
@XmlRootElement(name = "pair")
@JsonRootName("pair")
public class Pair {

  protected String pvalue1;
  protected String pvalue2;
  @XmlAttribute protected String value1;
  @XmlAttribute protected String value2;

  public String getPvalue1() {
    return pvalue1;
  }

  public void setPvalue1(String value) {
    this.pvalue1 = value;
  }

  public String getPvalue2() {
    return pvalue2;
  }

  public void setPvalue2(String value) {
    this.pvalue2 = value;
  }

  public String getValue1() {
    return value1;
  }

  public void setValue1(String value) {
    this.value1 = value;
  }

  public String getValue2() {
    return value2;
  }

  public void setValue2(String value) {
    this.value2 = value;
  }
}
